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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Properly synchronized version of 'resolveObjectTree'.
     */
    PrefetchProcessorNode synchronizedRootResultNodeFromDataRows(
            PrefetchTreeNode tree,
            List mainResultRows,
            Map extraResultsByPath) {

        synchronized (context.getObjectStore()) {
            return resolveObjectTree(tree, mainResultRows, extraResultsByPath);
        }
    }

    private PrefetchProcessorNode resolveObjectTree(
            PrefetchTreeNode tree,
            List mainResultRows,
            Map extraResultsByPath) {

        // create a copy of the tree using DecoratedPrefetchNodes and then traverse it
        // resolving objects...
        PrefetchProcessorNode decoratedTree = new PrefetchProcessorTreeBuilder(
                this,
                mainResultRows,
                extraResultsByPath).buildTree(tree);

        // do a single path for disjoint prefetches, joint subtrees will be processed at
        // each disjoint node that is a parent of joint prefetches.
        decoratedTree.traverse(new DisjointProcessor());

        // connect related objects
        decoratedTree.traverse(new PostProcessor());

        return decoratedTree;
    }

    final class DisjointProcessor implements PrefetchProcessor {

        @Override
        public boolean startDisjointPrefetch(PrefetchTreeNode node) {

            PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;

            // this means something bad happened during fetch
            if (processorNode.getDataRows() == null) {
                return false;
            }

            // ... continue with processing even if the objects list is empty to handle
            // multi-step prefetches.
            if (processorNode.getDataRows().isEmpty()) {
                return true;
            }

            List<Persistent> objects = processorNode.getResolver().objectsFromDataRows(processorNode.getDataRows());
            processorNode.setObjects(objects);

            return true;
        }

        @Override
        public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorNode processorNode = (PrefetchProcessorNode) node;

            if (node.getParent().isPhantom()) {
                // TODO: doing nothing in current implementation if parent node is phantom
                return true;
            }

            PrefetchProcessorNode parentProcessorNode = (PrefetchProcessorNode) processorNode
                    .getParent();
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

                    buffer.append(dbRelationships
                            .get(i)
                            .getReverseRelationship()
                            .getName());
                }

                pathPrefix = buffer.append(".").toString();
            }

            List<?> parentDataRows;
            
			// note that a disjoint prefetch that has adjacent joint prefetches
			// will be a PrefetchProcessorJointNode, so here check for
			// semantics, not node type
			if (parentProcessorNode.getSemantics() == PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS) {
				parentDataRows = ((PrefetchProcessorJointNode) parentProcessorNode).getResolvedRows();
			} else {
				parentDataRows = parentProcessorNode.getDataRows();
			}

            int maxIdQualifierSize = context
                    .getParentDataDomain()
                    .getMaxIdQualifierSize();

            List<PrefetchSelectQuery> queries = new ArrayList<>();
            int qualifiersCount = 0;
            PrefetchSelectQuery currentQuery = null;
            List<DbJoin> joins = lastDbRelationship.getJoins();
            Set<List<Object>> values = new HashSet<>();

            for (Object dataRow : parentDataRows) {
                // handling too big qualifiers
                if (currentQuery == null
                        || (maxIdQualifierSize > 0 && qualifiersCount + joins.size() > maxIdQualifierSize)) {

                    createDisjointByIdPrefetchQualifier(pathPrefix, currentQuery, joins, values);

                    currentQuery = new PrefetchSelectQuery(node.getPath(), relationship);
                    queries.add(currentQuery);
                    qualifiersCount = 0;
                    values = new HashSet<>();
                }

                List<Object> joinValues = new ArrayList<>(joins.size());
                for (DbJoin join : joins) {
                    Object targetValue = ((DataRow) dataRow).get(join.getSourceName());
                    joinValues.add(targetValue);
                }

                if(values.add(joinValues)) {
                    qualifiersCount += joins.size();
                }
            }
            // add final part of values
            createDisjointByIdPrefetchQualifier(pathPrefix, currentQuery, joins, values);

            PrefetchTreeNode jointSubtree = node.cloneJointSubtree();

            List dataRows = new ArrayList();
            for (PrefetchSelectQuery query : queries) {
                // need to pass the remaining tree to make joint prefetches work
                if (jointSubtree.hasChildren()) {
                    query.setPrefetchTree(jointSubtree);
                }

                query.setFetchingDataRows(true);
                if (relationship.isSourceIndependentFromTargetChange()) {
                    // setup extra result columns to be able to relate result rows to the
                    // parent result objects.
                    query.addResultPath("db:"
                            + relationship.getReverseDbRelationshipPath());
                }
                dataRows.addAll(context.performQuery(query));
            }
            processorNode.setDataRows(dataRows);

            return startDisjointPrefetch(node);
        }

        private void createDisjointByIdPrefetchQualifier(String pathPrefix, PrefetchSelectQuery currentQuery,
                                                         List<DbJoin> joins, Set<List<Object>> values) {
            Expression allJoinsQualifier;
            if(currentQuery != null) {
                for(List<Object> joinValues : values) {
                    allJoinsQualifier = null;
                    for(int i=0; i<joins.size(); i++) {
                        Expression joinQualifier = ExpressionFactory.matchDbExp(pathPrefix
                                + joins.get(i).getTargetName(), joinValues.get(i));
                        if (allJoinsQualifier == null) {
                            allJoinsQualifier = joinQualifier;
                        } else {
                            allJoinsQualifier = allJoinsQualifier.andExp(joinQualifier);
                        }
                    }
                    currentQuery.orQualifier(allJoinsQualifier);
                }
            }
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

                List parentObjects = parent.getObjects();
                int size = parentRows.size();

                for (int i = 0; i < size; i++) {
                    subprocessor.setCurrentFlatRow((DataRow) parentRows.get(i));
                    parent.setLastResolved((Persistent) parentObjects.get(i));
                    processorNode.traverse(subprocessor);
                }

                List objects = processorNode.getObjects();

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
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
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

                        Set<Persistent> seen = new HashSet<Persistent>(objects.size());
                        Iterator<Persistent> it = objects.iterator();
                        while (it.hasNext()) {
                            if (!seen.add(it.next())) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    // a processor of a single joint result set that walks a subtree of prefetch nodes
    // that use this result set.
    final class JointProcessor implements PrefetchProcessor {

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

            Persistent object = null;

            // find existing object, if found skip further processing
            Map id = processorNode.idFromFlatRow(currentFlatRow);
            object = processorNode.getResolved(id);
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

            // linking by parent needed even if an object is already there
            // (many-to-many case)

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
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
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
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }
    }
}
