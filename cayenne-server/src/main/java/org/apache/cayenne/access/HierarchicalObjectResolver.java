/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

import static org.apache.cayenne.exp.ExpressionFactory.dbPathExp;
import static org.apache.cayenne.exp.ExpressionFactory.fullObjectExp;

/**
 * Processes a number of DataRow sets corresponding to a given prefetch tree, resolving
 * DataRows to an object tree. Can process any combination of joint and disjoint sets, per
 * prefetch tree.
 */
class HierarchicalObjectResolver {

    DataContext context;
    QueryMetadata queryMetadata;
    DataRowStore cache;
    ClassDescriptor descriptor;
    boolean needToSaveDuplicates;

    HierarchicalObjectResolver(DataContext context, QueryMetadata queryMetadata) {
        this.queryMetadata = queryMetadata;
        this.context = context;
        this.cache = context.getObjectStore().getDataRowCache();
    }

    HierarchicalObjectResolver(DataContext context, QueryMetadata metadata,
                               ClassDescriptor descriptor, boolean needToSaveDuplicates) {
        this(context, metadata);
        this.descriptor = descriptor;
        this.needToSaveDuplicates = needToSaveDuplicates;
    }

    /**
     * Properly synchronized object resolution
     */
    PrefetchProcessorNode synchronizedRootResultNodeFromDataRows(
            PrefetchTreeNode tree,
            List<DataRow> mainResultRows,
            Map<String, List<?>> extraResultsByPath) {

        PrefetchProcessorNode decoratedTree = decorateTree(tree, mainResultRows, extraResultsByPath);

        // prepare data for disjoint by id prefetches
        decoratedTree.traverse(new DisjointByIdProcessor());

        // resolve objects under global lock to keep object graph consistent
        synchronized (context.getObjectStore()) {
            // do a single path for disjoint prefetches, joint subtrees will be processed at
            // each disjoint node that is a parent of joint prefetches.
            decoratedTree.traverse(new DisjointProcessor());

            // connect related objects
            decoratedTree.traverse(new PostProcessor());
        }

        return decoratedTree;
    }

    /**
     * create a copy of the tree using DecoratedPrefetchNodes and then traverse it resolving objects...
     */
    private PrefetchProcessorNode decorateTree(PrefetchTreeNode tree,
                                               List<DataRow> mainResultRows,
                                               Map<String, List<?>> extraResultsByPath) {
        return new PrefetchProcessorTreeBuilder(this, mainResultRows, extraResultsByPath)
                .buildTree(tree);
    }

    final class DisjointByIdProcessor implements PrefetchProcessor {

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            if (node.getParent().isPhantom()) {
                // doing nothing in current implementation if parent node is phantom
                return true;
            }

            PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;
            PrefetchProcessorNode parentProcessorNode = (PrefetchProcessorNode) processorNode.getParent();
            ObjRelationship relationship = processorNode.getIncoming().getRelationship();

            List<DbRelationship> dbRelationships = relationship.getDbRelationships();
            DbRelationship lastDbRelationship = dbRelationships.get(0);

            String pathPrefix = "";
            if (dbRelationships.size() > 1) {
                // we need path prefix for flattened relationships
                StringBuilder buffer = new StringBuilder();
                for (int i = dbRelationships.size() - 1; i >= 1; i--) {
                    if (buffer.length() > 0) {
                        buffer.append(".");
                    }

                    buffer.append(dbRelationships.get(i).getReverseRelationship().getName());
                }

                pathPrefix = buffer.append(".").toString();
            }

            List<DataRow> parentDataRows;

            // note that a disjoint prefetch that has adjacent joint prefetches
            // will be a PrefetchProcessorJointNode, so here check for semantics, not node type
            if (parentProcessorNode.getSemantics() == PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS) {
                parentDataRows = ((PrefetchProcessorJointNode) parentProcessorNode).getResolvedRows();
            } else {
                parentDataRows = parentProcessorNode.getDataRows();
            }

            int maxIdQualifierSize = context.getParentDataDomain().getMaxIdQualifierSize();
            List<DbJoin> joins = lastDbRelationship.getJoins();

            List<PrefetchSelectQuery<DataRow>> queries = new ArrayList<>();
            PrefetchSelectQuery<DataRow> currentQuery = null;
            int qualifiersCount = 0;
            Set<List<Object>> values = new HashSet<>();

            for (DataRow dataRow : parentDataRows) {
                // handling too big qualifiers
                if (currentQuery == null
                        || (maxIdQualifierSize > 0 && qualifiersCount + joins.size() > maxIdQualifierSize)) {

                    if (relationship.hasReverseDdRelationship()) {
                        createDisjointByIdPrefetchQualifier(pathPrefix,
                                currentQuery,
                                getJoinsNames(joins, DbJoin::getTargetName),
                                values);
                        currentQuery = new PrefetchSelectQuery<>(node.getPath(), relationship);
                    } else {
                        createDisjointByIdPrefetchQualifier(pathPrefix,
                                currentQuery,
                                getPksNames(lastDbRelationship),
                                values);
                        currentQuery = createInversedQuery(node.getPath(), relationship);
                    }

                    currentQuery.setFetchingDataRows(true);
                    queries.add(currentQuery);
                    qualifiersCount = 0;
                    values = new HashSet<>();
                }

                List<Object> joinValues;
                if(relationship.hasReverseDdRelationship()) {
                    joinValues = getValues(
                            getJoinsNames(joins, DbJoin::getSourceName),
                            dataRow);
                } else {
                    joinValues = getValues(getPksNames(lastDbRelationship), dataRow);
                }

                if(values.add(joinValues)) {
                    qualifiersCount += joins.size();
                }
            }

            // add final part of values
            if (relationship.hasReverseDdRelationship()) {
                createDisjointByIdPrefetchQualifier(pathPrefix,
                        currentQuery,
                        getJoinsNames(joins, DbJoin::getTargetName),
                        values);
            } else {
                createDisjointByIdPrefetchQualifier(pathPrefix,
                        currentQuery,
                        getPksNames(lastDbRelationship),
                        values);
            }

            PrefetchTreeNode jointSubtree = node.cloneJointSubtree();

            String reversePath = null;
            if (relationship.isSourceIndependentFromTargetChange()) {
                reversePath = "db:";
                reversePath += relationship.hasReverseDdRelationship() ?
                        relationship.getReverseDbRelationshipPath() :
                        relationship.getDbRelationshipPath();
            }

            List<DataRow> dataRows = new ArrayList<>();
            for (PrefetchSelectQuery<DataRow> query : queries) {
                // need to pass the remaining tree to make joint prefetches work
                if (jointSubtree.hasChildren()) {
                    query.setPrefetchTree(jointSubtree);
                }

                if (reversePath != null) {
                    // setup extra result columns to be able to relate result rows to the parent result objects.
                    query.addResultPath(reversePath);
                }

                dataRows.addAll(query.select(context));
            }
            processorNode.setDataRows(dataRows);

            return true;
        }

        private PrefetchSelectQuery<DataRow> createInversedQuery(String path, ObjRelationship relationship) {
            PrefetchSelectQuery<DataRow> currentQuery = new PrefetchSelectQuery<>(path, relationship);
            currentQuery.setRoot(relationship.getSourceEntity());
            Expression fullObjectExp = fullObjectExp(dbPathExp(relationship.getDbRelationshipPath()));
            Class<?> targetClassType = context.getEntityResolver().getClassDescriptor(relationship.getTargetEntityName()).getObjectClass();
            BaseProperty<?> baseProperty = PropertyFactory.createBase(fullObjectExp, targetClassType);
            currentQuery.setColumns(baseProperty);
            return currentQuery;
        }

        private List<Object> getValues(List<String> names, DataRow dataRow) {
            List<Object> joinValues = new ArrayList<>(names.size());
            for(String name : names) {
                Object targetValue = dataRow.get(name);
                joinValues.add(targetValue);
            }
            return joinValues;
        }

        private List<String> getPksNames(DbRelationship dbRelationship) {
            return dbRelationship.getSourceEntity().getPrimaryKeys()
                    .stream().map(DbAttribute::getName).collect(Collectors.toList());
        }

        private List<String> getJoinsNames(List<DbJoin> joins, Function<? super DbJoin, ? extends String> func) {
            return joins.stream().map(func).collect(Collectors.toList());
        }

        private void createDisjointByIdPrefetchQualifier(String pathPrefix, PrefetchSelectQuery currentQuery,
                                                         List<String> names, Set<List<Object>> values) {
            Expression allJoinsQualifier;
            if(currentQuery != null) {
                Expression[] qualifiers = new Expression[values.size()];
                int i = 0;
                for(List<Object> joinValues : values) {
                    allJoinsQualifier = null;
                    for(int j = 0; j < names.size(); j++) {
                        Expression joinQualifier = ExpressionFactory
                                .matchDbExp(pathPrefix + names.get(j), joinValues.get(j));
                        if (allJoinsQualifier == null) {
                            allJoinsQualifier = joinQualifier;
                        } else {
                            allJoinsQualifier = allJoinsQualifier.andExp(joinQualifier);
                        }
                    }
                    qualifiers[i++] = allJoinsQualifier;
                }

                currentQuery.orQualifier(ExpressionFactory.joinExp(Expression.OR, qualifiers));
            }
        }

        @Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: %s", node);
        }

        @Override
        public void finishPrefetch(PrefetchTreeNode node) {
        }
    }

    final class DisjointProcessor implements PrefetchProcessor {

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {

            PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;

            // this means something bad happened during fetch
            if (processorNode.getDataRows() == null) {
                return false;
            }

            // continue with processing even if the objects list is empty to handle multi-step prefetches.
            if (processorNode.getDataRows().isEmpty()) {
                return true;
            }

            List<Persistent> objects = processorNode.getResolver().objectsFromDataRows(processorNode.getDataRows());
            processorNode.setObjects(objects);

            return true;
        }

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            return startDisjointPrefetch(node);
        }

        @Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {

            // delegate processing of the top level joint prefetch to a joint processor,
            // skip non-top joint nodes

            if (node.getParent() != null && !node.getParent().isJointPrefetch()) {

                PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;

                JointProcessor subprocessor = new JointProcessor(processorNode);

                PrefetchProcessorNode parent = (PrefetchProcessorNode) processorNode
                        .getParent();

                while (parent != null && parent.isPhantom()) {
                    parent = (PrefetchProcessorNode) parent.getParent();
                }

                if (parent == null) {
                    return false;
                }

                List parentRows = parent.getDataRows();

                // phantom node?
                if (parentRows == null || parentRows.size() == 0) {
                    return false;
                }

                List<Persistent> parentObjects = parent.getObjects();
                int size = parentRows.size();

                for (int i = 0; i < size; i++) {
                    subprocessor.setCurrentFlatRow((DataRow) parentRows.get(i));
                    parent.setLastResolved(parentObjects.get(i));
                    processorNode.traverse(subprocessor);
                }

                List<Persistent> objects = processorNode.getObjects();

                cache.snapshotsUpdatedForObjects(
                        objects,
                        processorNode.getResolvedRows(),
                        queryMetadata.isRefreshingObjects());

            }
            return true;
        }

        @Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: %s", node);
        }

        @Override
        public void finishPrefetch(PrefetchTreeNode node) {
            // now that all the children are processed, we can clear the dupes

            // TODO: see TODO in ObjectResolver.relatedObjectsFromDataRows

            if ((node.isDisjointPrefetch() || node.isDisjointByIdPrefetch())
                    && !needToSaveDuplicates) {
                PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;
                if (processorNode.isJointChildren()) {
                    List<Persistent> objects = processorNode.getObjects();

                    if (objects != null && objects.size() > 1) {

                        Set<Persistent> seen = new HashSet<>(objects.size());
                        objects.removeIf(persistent -> !seen.add(persistent));
                    }
                }
            }
        }
    }

    // a processor of a single joint result set that walks a subtree of prefetch nodes
    // that use this result set.
    final static class JointProcessor implements PrefetchProcessor {

        DataRow currentFlatRow;
        PrefetchProcessorNode rootNode;

        JointProcessor(PrefetchProcessorJointNode rootNode) {
            this.rootNode = rootNode;
        }

        void setCurrentFlatRow(DataRow currentFlatRow) {
            this.currentFlatRow = currentFlatRow;
        }

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            // disjoint prefetch that is not the root terminates the walk...
            // don't process the root node itself..
            return node == rootNode;
        }

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            return startDisjointPrefetch(node);
        }

        @Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;

            // find existing object, if found skip further processing
            Map id = processorNode.idFromFlatRow(currentFlatRow);
            Persistent object = processorNode.getResolved(id);
            DataRow row = null;
            if (object == null) {

                row = processorNode.rowFromFlatRow(currentFlatRow);
                object = processorNode.getResolver().objectFromDataRow(row);

                // LEFT OUTER JOIN produced no matches...
                if (object == null) {
                    return false;
                }

                processorNode.putResolved(id, object);
                processorNode.addObject(object, row);
            }

            // linking by parent needed even if an object is already there (many-to-many case)
            processorNode.getParentAttachmentStrategy().linkToParent(row, object);

            processorNode.setLastResolved(object);
            return processorNode.isJointChildren();
        }

        @Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return ((PrefetchProcessorNode) node).isJointChildren();
        }

        @Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: %s", node);
        }

        @Override
        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    // processor that converts temporary associations between DataObjects to Cayenne
    // relationships and also fires snapshot update events
    final class PostProcessor implements PrefetchProcessor {

        @Override
        public void finishPrefetch(PrefetchTreeNode node) {
        }

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            ((PrefetchProcessorNode) node).connectToParents();
            return true;
        }

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            return startDisjointPrefetch(node);
        }

        @Override
        public boolean startJointPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;

            if (!processorNode.getObjects().isEmpty()) {
                cache.snapshotsUpdatedForObjects(
                        processorNode.getObjects(),
                        processorNode.getResolvedRows(),
                        queryMetadata.isRefreshingObjects());
            }

            // run 'connectToParents' even if the object list is empty. This is needed to
            // refresh stale relationships e.g. when some related objects got deleted.
            processorNode.connectToParents();
            return true;
        }

        @Override
        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        @Override
        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: %s", node);
        }
    }
}
