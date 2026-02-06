/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.SingleEntryMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
            Map<CayennePath, List<?>> extraResultsByPath) {

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
                                               Map<CayennePath, List<?>> extraResultsByPath) {
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
            
            // If parent is a joint node, defer processing until the joint node is processed
            if (parentProcessorNode.getSemantics() == PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS) {
                // Mark that we need to process this later, but don't fetch data now
                return true;
            }
            
            return processDisjointByIdNode(node);
        }
        
        // Process a disjointById node without checking for deferral
        private boolean processDisjointByIdNode(PrefetchTreeNode node) {
            PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;
            PrefetchProcessorNode parentProcessorNode = (PrefetchProcessorNode) processorNode.getParent();
            ObjRelationship relationship = processorNode.getIncoming().getRelationship();

            List<DbRelationship> dbRelationships = relationship.getDbRelationships();
            CayennePath dataRowPrefix = CayennePath.EMPTY_PATH;
            CayennePath qualifierPrefix = CayennePath.EMPTY_PATH;
            if (relationship.isFkThroughInheritance()) {
                for (int i = 0; i < dbRelationships.size() - 1; i++) {
                    dataRowPrefix = dataRowPrefix.dot(dbRelationships.get(i).getName());
                }
            } else if (dbRelationships.size() > 1) {
                // we need path prefix for flattened relationships
                for (int i = dbRelationships.size() - 1; i >= 1; i--) {
                    dataRowPrefix = dataRowPrefix.dot(dbRelationships.get(i).getReverseRelationship().getName());
                }
                qualifierPrefix = dataRowPrefix;
            }

            List<DataRow> parentDataRows;

            // note that a disjoint prefetch that has adjacent joint prefetches
            // will be a PrefetchProcessorJointNode, so here check for semantics, not node type
            if (parentProcessorNode.getSemantics() == PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS) {
                parentDataRows = ((PrefetchProcessorJointNode) parentProcessorNode).getResolvedRows();
            } else {
                parentDataRows = parentProcessorNode.getDataRows();
            }

            // If parent data rows is null or empty, there's nothing to prefetch
            if (parentDataRows == null || parentDataRows.isEmpty()) {
                processorNode.setDataRows(new ArrayList<>());
                return true;
            }

            int maxIdQualifierSize = context.getParentDataDomain().getMaxIdQualifierSize();
            List<DbJoin> joins = getDbJoins(relationship);
            Map<DbJoin, String> joinToDataRowKey = getDataRowKeys(joins, dataRowPrefix);

            List<PrefetchSelectQuery<DataRow>> queries = new ArrayList<>();
            PrefetchSelectQuery<DataRow> currentQuery = null;
            int qualifiersCount = 0;
            Set<List<Object>> values = new HashSet<>();

            for (DataRow dataRow : parentDataRows) {
                // handling too big qualifiers
                if (currentQuery == null
                        || (maxIdQualifierSize > 0 && qualifiersCount + joins.size() > maxIdQualifierSize)) {

                    createDisjointByIdPrefetchQualifier(qualifierPrefix, currentQuery, joins, values);

                    currentQuery = new PrefetchSelectQuery<>(node.getPath(), relationship);
                    currentQuery.fetchDataRows();
                    queries.add(currentQuery);
                    qualifiersCount = 0;
                    values = new HashSet<>();
                }

                List<Object> joinValues = new ArrayList<>(joins.size());
                for (DbJoin join : joins) {
                    String dataRowKey = joinToDataRowKey.get(join);
                    Object targetValue = dataRow.get(dataRowKey);
                    joinValues.add(targetValue);
                }

                if(values.add(joinValues)) {
                    qualifiersCount += joins.size();
                }
            }
            // add final part of values
            createDisjointByIdPrefetchQualifier(qualifierPrefix, currentQuery, joins, values);

            PrefetchTreeNode jointSubtree = node.cloneJointSubtree();

            Expression reversePath = null;
            if (relationship.isSourceIndependentFromTargetChange() && !relationship.isFkThroughInheritance()) {
                reversePath = ExpressionFactory.dbPathExp(relationship.getReverseDbRelationshipPath());
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

        private List<DbJoin> getDbJoins(ObjRelationship relationship) {
            // we get the part of the relationship path that contains FK
            List<DbRelationship> dbRelationships = relationship.getDbRelationships();
            if(relationship.isToMany() || !relationship.isToPK()) {
                return dbRelationships.get(0).getJoins();
            } else {
                return dbRelationships.get(dbRelationships.size() - 1).getJoins();
            }
        }

        /**
         * precalculate join to a DataRow key
         * @param joins to get key from
         * @param pathPrefix for the flattened path
         * @return mapping of DbJoin to DataRow key to use
         */
        private Map<DbJoin, String> getDataRowKeys(List<DbJoin> joins, CayennePath pathPrefix) {
            Map<DbJoin, String> joinToDataRowKey = joins.size() == 1
                    ? new SingleEntryMap<>(joins.get(0))
                    : new HashMap<>(joins.size());
            for (DbJoin join : joins) {
                String dataRowKey;
                if(join.getRelationship().isToMany() || !join.getRelationship().isToPK()) {
                    dataRowKey = join.getSourceName();
                } else {
                    dataRowKey = pathPrefix.dot(join.getSourceName()).value();
                }
                joinToDataRowKey.put(join, dataRowKey);
            }
            return joinToDataRowKey;
        }

        private void createDisjointByIdPrefetchQualifier(CayennePath pathPrefix, PrefetchSelectQuery<?> currentQuery,
                                                         List<DbJoin> joins, Set<List<Object>> values) {
            if (currentQuery == null) return;

             // Use an IN clause for the list of prefetch IDs, when the
             // join ON clause only has one predicate with many values.
             // Results in SQL:  ... targetField IN ( ?, ?, ?, .... )
            if (joins.size() == 1 && values.size() > 1) {
                currentQuery.and( ExpressionFactory.inDbExp(
                    pathPrefix.dot(joins.get(0).getTargetName()).value(),
                    values.stream().flatMap(List::stream).collect(Collectors.toSet())
                ));
            } else { // Handle a single value or compound prefetch ID predicates
            	// SQL: ... (field1=? and field2=? ...) OR (field1=? and field2=? ...) etc
            	Expression allJoinsQualifier;
                Expression[] qualifiers = new Expression[values.size()];
                int i = 0;
                for(List<Object> joinValues : values) {
                    allJoinsQualifier = null;
                    for(int j=0; j<joins.size(); j++) {
                        Expression joinQualifier = ExpressionFactory
                                .matchDbExp(pathPrefix.dot(joins.get(j).getTargetName()).value(), joinValues.get(j));
                        if (allJoinsQualifier == null) {
                            allJoinsQualifier = joinQualifier;
                        } else {
                            allJoinsQualifier = allJoinsQualifier.andExp(joinQualifier);
                        }
                    }
                    qualifiers[i++] = allJoinsQualifier;
                }

                currentQuery.or(ExpressionFactory.joinExp(Expression.OR, qualifiers));
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
            if (node.getParent() == null || node.getParent().isJointPrefetch()) {
                return true;
            }

            PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;
            JointProcessor subprocessor = new JointProcessor(processorNode);

            PrefetchProcessorNode parent = (PrefetchProcessorNode) processorNode.getParent();
            while (parent != null && parent.isPhantom()) {
                parent = (PrefetchProcessorNode) parent.getParent();
            }

            if (parent == null) {
                return false;
            }

            List<DataRow> parentRows = parent.getDataRows();
            // phantom node?
            if (parentRows == null || parentRows.size() == 0) {
                return false;
            }

            List<Persistent> parentObjects = parent.getObjects();
            int size = parentRows.size();

            for (int i = 0; i < size; i++) {
                subprocessor.setCurrentFlatRow(parentRows.get(i));
                parent.setLastResolved(parentObjects.get(i));
                processorNode.traverse(subprocessor);
            }

            List<Persistent> objects = processorNode.getObjects();
            cache.snapshotsUpdatedForObjects(
                    objects,
                    processorNode.getResolvedRows(),
                    queryMetadata.isRefreshingObjects());

            // Now process any deferred disjointById children
            DisjointByIdProcessor byIdProcessor = new DisjointByIdProcessor();
            for (PrefetchTreeNode child : node.getChildren()) {
                if (child.isDisjointByIdPrefetch()) {
                    // Now that the joint parent has been processed, we can fetch the disjointById data
                    byIdProcessor.processDisjointByIdNode(child);
                    // And resolve the objects
                    startDisjointPrefetch(child);
                }
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

            if ((node.isDisjointPrefetch() || node.isDisjointByIdPrefetch()) && !needToSaveDuplicates) {
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
            Map<String, Object> id = processorNode.idFromFlatRow(currentFlatRow);
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
            // we need the row for parent attachment even if object was already resolved
            if (row == null) {
                row = processorNode.rowFromFlatRow(currentFlatRow);
            }
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

    // processor that converts temporary associations between Persistent objects to Cayenne
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
