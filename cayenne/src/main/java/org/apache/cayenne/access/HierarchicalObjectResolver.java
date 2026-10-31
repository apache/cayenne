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
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.ArrayList;
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

    final DataContext context;
    // the user query the rows belong to; the source of the selects run for disjoint-by-id prefetches
    final Query<?> source;
    final ClassDescriptor descriptor;
    final boolean refreshing;
    final DataRowStore cache;
    final boolean needToSaveDuplicates;

    HierarchicalObjectResolver(DataContext context, Query<?> source, ClassDescriptor descriptor, boolean refreshing) {
        this(context, source, descriptor, refreshing, false);
    }

    HierarchicalObjectResolver(DataContext context, Query<?> source, ClassDescriptor descriptor, boolean refreshing,
                               boolean needToSaveDuplicates) {
        this.context = context;
        this.source = source;
        this.descriptor = descriptor;
        this.refreshing = refreshing;
        this.cache = context.getObjectStore().getDataRowCache();
        this.needToSaveDuplicates = needToSaveDuplicates;
    }

    /**
     * Resolves the rows of the prefetch tree to an object tree. Every object merge and every relationship write
     * happens under the lock of the object written, with the snapshot version check that keeps the result of an older
     * statement from overwriting a newer one, so no lock is held across the tree.
     */
    PrefetchProcessorNode rootResultNodeFromDataRows(
            PrefetchTreeNode tree,
            List<DataRow> mainResultRows,
            Map<CayennePath, List<?>> extraResultsByPath) {

        PrefetchProcessorNode decoratedTree = decorateTree(tree, mainResultRows, extraResultsByPath);

        // prepare data for disjoint by id prefetches
        decoratedTree.traverse(new DisjointByIdProcessor());

        // do a single path for disjoint prefetches, joint subtrees will be processed at
        // each disjoint node that is a parent of joint prefetches.
        decoratedTree.traverse(new DisjointProcessor());

        // connect related objects
        decoratedTree.traverse(new PostProcessor());

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
            CayennePath pathPrefix = CayennePath.EMPTY_PATH;
            if (dbRelationships.size() > 1) {
                // we need path prefix for flattened relationships
                for (int i = dbRelationships.size() - 1; i >= 1; i--) {
                    pathPrefix = pathPrefix.dot(dbRelationships.get(i).getReverseRelationship().getName());
                }
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

            int maxIdQualifierSize = context.getChannel().getDataDomain().getMaxIdQualifierSize();
            List<DbJoin> joins = getDbJoins(relationship);
            DataRowLayout.Reader keyReader = DataRowLayout.of(dataRowKeys(joins, pathPrefix)).reader();

            // the parent keys, in chunks that fit the qualifier size limit; each chunk is fetched by a select of
            // its own, as DataRows, with the joint subtree of the node to make its joint prefetches work
            List<Set<List<Object>>> chunks = new ArrayList<>();
            Set<List<Object>> values = null;
            int qualifiersCount = 0;

            for (DataRow dataRow : parentDataRows) {
                // handling too big qualifiers
                if (values == null
                        || (maxIdQualifierSize > 0 && qualifiersCount + joins.size() > maxIdQualifierSize)) {
                    values = new HashSet<>();
                    chunks.add(values);
                    qualifiersCount = 0;
                }

                List<Object> joinValues = new ArrayList<>(joins.size());
                for (int i = 0; i < joins.size(); i++) {
                    joinValues.add(keyReader.get(dataRow, i));
                }

                if (values.add(joinValues)) {
                    qualifiersCount += joins.size();
                }
            }

            PrefetchTreeNode jointSubtree = node.cloneJointSubtree();

            // extra result columns to be able to relate result rows to the parent result objects
            List<PathExp> resultPaths = relationship.isForeignKey()
                    ? List.of()
                    : List.of((PathExp) ExpressionFactory.dbPathExp(relationship.getReverseDbRelationshipPath()));

            EntityResolver resolver = context.getEntityResolver();
            List<DataRow> dataRows = new ArrayList<>();
            for (Set<List<Object>> chunk : chunks) {
                ResolvedSelect select = ResolvedSelect.builder()
                        .source(source)
                        .entityName(relationship.getTargetEntityName())
                        .fetchingDataRows(true)
                        .where(disjointByIdQualifier(pathPrefix, joins, chunk))
                        .prefetchTree(jointSubtree.hasChildren() ? jointSubtree : null)
                        .prefetch(node.getPath(), resultPaths)
                        .build(resolver);

                @SuppressWarnings("unchecked")
                List<DataRow> chunkRows = (List<DataRow>) QueryResults.firstList(
                        context.onQuery(new QueryContext(context, select.source(), select, false), false));
                dataRows.addAll(chunkRows);
            }
            processorNode.setDataRows(dataRows);

            return true;
        }

        private List<DbJoin> getDbJoins(ObjRelationship relationship) {
            // we get the part of the relationship path that contains FK
            List<DbRelationship> dbRelationships = relationship.getDbRelationships();
            if (!dbRelationships.getFirst().isToPrimaryKey()) {
                return dbRelationships.getFirst().getJoins();
            } else {
                return dbRelationships.getLast().getJoins();
            }
        }

        /**
         * Returns the DataRow key of each join, in join order.
         *
         * @param joins to get key from
         * @param pathPrefix for the flattened path
         */
        private String[] dataRowKeys(List<DbJoin> joins, CayennePath pathPrefix) {
            String[] keys = new String[joins.size()];
            for (int i = 0; i < keys.length; i++) {
                DbJoin join = joins.get(i);
                keys[i] = !join.getRelationship().isToPrimaryKey()
                        ? join.getSourceName()
                        : pathPrefix.dot(join.getSourceName()).value();
            }
            return keys;
        }

        private Expression disjointByIdQualifier(CayennePath pathPrefix, List<DbJoin> joins,
                                                 Set<List<Object>> values) {
            // Use an IN clause for the list of prefetch IDs, when the
            // join ON clause only has one predicate with many values.
            // Results in SQL:  ... targetField IN ( ?, ?, ?, .... )
            if (joins.size() == 1 && values.size() > 1) {
                return ExpressionFactory.inDbExp(
                        pathPrefix.dot(joins.get(0).getTargetName()).value(),
                        values.stream().flatMap(List::stream).collect(Collectors.toSet()));
            }

            // Handle a single value or compound prefetch ID predicates
            // SQL: ... (field1=? and field2=? ...) OR (field1=? and field2=? ...) etc
            Expression[] qualifiers = new Expression[values.size()];
            int i = 0;
            for (List<Object> joinValues : values) {
                Expression allJoinsQualifier = null;
                for (int j = 0; j < joins.size(); j++) {
                    Expression joinQualifier = ExpressionFactory
                            .matchDbExp(pathPrefix.dot(joins.get(j).getTargetName()).value(), joinValues.get(j));
                    allJoinsQualifier = allJoinsQualifier == null
                            ? joinQualifier
                            : allJoinsQualifier.andExp(joinQualifier);
                }
                qualifiers[i++] = allJoinsQualifier;
            }
            return ExpressionFactory.or(qualifiers);
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
                    refreshing);

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
                        Set<Persistent> seen = HashSet.newHashSet(objects.size());
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
            Object id = processorNode.idFromFlatRow(currentFlatRow);
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
                        refreshing);
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
