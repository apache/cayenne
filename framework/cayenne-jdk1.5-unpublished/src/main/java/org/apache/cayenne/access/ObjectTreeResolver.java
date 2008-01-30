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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Processes a number of DataRow sets corresponding to a given prefetch tree, resolving
 * DataRows to an object tree. Can process any combination of joint and disjoint sets, per
 * prefetch tree.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ObjectTreeResolver {

    DataContext context;
    QueryMetadata queryMetadata;
    DataRowStore cache;

    ObjectTreeResolver(DataContext context, QueryMetadata queryMetadata) {
        this.queryMetadata = queryMetadata;
        this.context = context;
        this.cache = context.getObjectStore().getDataRowCache();
    }

    /**
     * Properly synchronized version of 'resolveObjectTree'.
     */
    List synchronizedObjectsFromDataRows(
            PrefetchTreeNode tree,
            List mainResultRows,
            Map extraResultsByPath) {

        synchronized (context.getObjectStore()) {
            synchronized (cache) {
                return resolveObjectTree(tree, mainResultRows, extraResultsByPath);
            }
        }
    }

    List resolveObjectTree(
            PrefetchTreeNode tree,
            List mainResultRows,
            Map extraResultsByPath) {

        // create a copy of the tree using DecoratedPrefetchNodes and then traverse it
        // resolving objects...
        PrefetchProcessorNode decoratedTree = new TreeBuilder(
                mainResultRows,
                extraResultsByPath).buildTree(tree);

        // do a single path for disjoint prefetches, joint subtrees will be processed at
        // each disjoint node that is a parent of joint prefetches.
        decoratedTree.traverse(new DisjointProcessor());

        // connect related objects
        decoratedTree.traverse(new PostProcessor());

        return decoratedTree.getObjects() != null
                ? decoratedTree.getObjects()
                : new ArrayList(1);
    }

    // A PrefetchProcessor that creates a replica of a PrefetchTree with node
    // subclasses that can carry extra info needed during traversal.
    final class TreeBuilder implements PrefetchProcessor {

        PrefetchProcessorNode root;
        LinkedList<PrefetchProcessorNode> nodeStack;

        List mainResultRows;
        Map extraResultsByPath;

        TreeBuilder(List mainResultRows, Map extraResultsByPath) {
            this.mainResultRows = mainResultRows;
            this.extraResultsByPath = extraResultsByPath;
        }

        PrefetchProcessorNode buildTree(PrefetchTreeNode tree) {
            // reset state
            this.nodeStack = new LinkedList<PrefetchProcessorNode>();
            this.root = null;

            tree.traverse(this);

            if (root == null) {
                throw new CayenneRuntimeException(
                        "Failed to create prefetch processing tree.");
            }

            return root;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {

            // root should be treated as disjoint
            if (getParent() == null) {
                return startDisjointPrefetch(node);
            }
            else {
                PrefetchProcessorNode decorated = new PrefetchProcessorNode(
                        getParent(),
                        node.getName());

                decorated.setPhantom(true);
                return addNode(decorated);
            }
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {

            // look ahead for joint children as joint children will require a different
            // node type.

            // TODO, Andrus, 11/16/2005 - minor inefficiency: 'adjacentJointNodes' would
            // grab ALL nodes, we just need to find first and stop...
            PrefetchProcessorNode decorated = !node.adjacentJointNodes().isEmpty()
                    ? new PrefetchProcessorJointNode(getParent(), node.getName())
                    : new PrefetchProcessorNode(getParent(), node.getName());
            decorated.setPhantom(false);

            // semantics has to be "DISJOINT" even if the node is joint, as semantics
            // defines relationship with parent..
            decorated.setSemantics(PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
            return addNode(decorated);
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorJointNode decorated = new PrefetchProcessorJointNode(
                    getParent(),
                    node.getName());
            decorated.setPhantom(false);
            decorated.setSemantics(PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
            boolean result = addNode(decorated);

            // set "jointChildren" flag on all nodes in the same "join group"
            PrefetchProcessorNode groupNode = decorated;
            while (groupNode.getParent() != null && !groupNode.isDisjointPrefetch()) {
                groupNode = (PrefetchProcessorNode) groupNode.getParent();
                groupNode.setJointChildren(true);
            }

            return result;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            // handle unknown as disjoint...
            return startDisjointPrefetch(node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // pop stack...
            nodeStack.removeLast();
        }

        boolean addNode(PrefetchProcessorNode node) {

            List rows;
            ArcProperty arc;
            ClassDescriptor descriptor;

            PrefetchProcessorNode currentNode = getParent();

            if (currentNode != null) {
                rows = (List) extraResultsByPath.get(node.getPath());
                arc = (ArcProperty) currentNode
                        .getResolver()
                        .getDescriptor()
                        .getProperty(node.getName());

                if (arc == null) {
                    throw new CayenneRuntimeException("No relationship with name '"
                            + node.getName()
                            + "' found in entity "
                            + currentNode.getResolver().getEntity().getName());
                }

                descriptor = arc.getTargetDescriptor();
            }
            else {
                arc = null;
                descriptor = queryMetadata.getClassDescriptor();
                rows = mainResultRows;
            }

            node.setDataRows(rows);
            node.setResolver(new ObjectResolver(context, descriptor, queryMetadata
                    .isRefreshingObjects(), queryMetadata.isResolvingInherited()));
            node.setIncoming(arc);

            if (currentNode != null) {
                currentNode.addChild(node);
            }

            node.afterInit();

            // push node on stack
            if (nodeStack.isEmpty()) {
                root = node;
            }
            nodeStack.addLast(node);

            return true;
        }

        PrefetchProcessorNode getParent() {
            return (nodeStack.isEmpty()) ? null : nodeStack
                    .getLast();
        }
    }

    final class DisjointProcessor implements PrefetchProcessor {

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

            List objects;

            // disjoint node that is an instance of DecoratedJointNode is a top
            // of a local joint prefetch "group"...
            if (processorNode instanceof PrefetchProcessorJointNode) {
                JointProcessor subprocessor = new JointProcessor(
                        (PrefetchProcessorJointNode) processorNode);
                Iterator it = processorNode.getDataRows().iterator();
                while (it.hasNext()) {
                    subprocessor.setCurrentFlatRow((DataRow) it.next());
                    processorNode.traverse(subprocessor);
                }

                objects = processorNode.getObjects();

                cache.snapshotsUpdatedForObjects(
                        objects,
                        ((PrefetchProcessorJointNode) processorNode).getResolvedRows(),
                        queryMetadata.isRefreshingObjects());
            }
            // disjoint prefetch on flattened relationships still requires manual matching
            else if (processorNode.getIncoming() != null
                    && processorNode.getIncoming().getRelationship().isFlattened()) {

                objects = processorNode.getResolver().relatedObjectsFromDataRows(
                        processorNode.getDataRows(),
                        processorNode);
                processorNode.setObjects(objects);
            }
            else {
                objects = processorNode.getResolver().objectsFromDataRows(
                        processorNode.getDataRows());
                processorNode.setObjects(objects);
            }

            // ... continue with processing even if the objects list is empty to handle
            // multi-step prefetches.
            if (objects.isEmpty()) {
                return true;
            }

            // create temporary relationship mapping if needed..; flattened relationships
            // are matched with parents during resolving phase, so skip them here.
            if (processorNode.isPartitionedByParent()
                    && !processorNode.getIncoming().getRelationship().isFlattened()) {

                ObjEntity sourceObjEntity = null;
                String relatedIdPrefix = null;

                // determine resolution strategy
                ArcProperty reverseArc = processorNode
                        .getIncoming()
                        .getComplimentaryReverseArc();

                // if null, prepare for manual matching
                if (reverseArc == null) {
                    relatedIdPrefix = processorNode
                            .getIncoming()
                            .getRelationship()
                            .getReverseDbRelationshipPath()
                            + ".";

                    sourceObjEntity = (ObjEntity) processorNode
                            .getIncoming()
                            .getRelationship()
                            .getSourceEntity();
                }

                Iterator it = objects.iterator();
                while (it.hasNext()) {
                    Persistent destinationObject = (Persistent) it.next();
                    Persistent sourceObject = null;

                    if (reverseArc != null) {
                        sourceObject = (Persistent) reverseArc
                                .readProperty(destinationObject);
                    }
                    else {
                        ObjectStore objectStore = context.getObjectStore();

                        // prefetched snapshots contain parent ids prefixed with
                        // relationship name.

                        DataRow snapshot = objectStore.getSnapshot(destinationObject
                                .getObjectId());

                        ObjectId id = processorNode.getResolver().createObjectId(
                                snapshot,
                                sourceObjEntity,
                                relatedIdPrefix);

                        sourceObject = (Persistent) objectStore.getNode(id);
                    }

                    // don't attach to hollow objects
                    if (sourceObject != null
                            && sourceObject.getPersistenceState() != PersistenceState.HOLLOW) {
                        processorNode.linkToParent(destinationObject, sourceObject);
                    }
                }
            }

            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            // allow joint prefetch nodes to process their children, but skip their own
            // processing.
            return true;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
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

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            // disjoint prefetch that is not the root terminates the walk...
            return node == rootNode ? startJointPrefetch(node) : false;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;

            Persistent object = null;

            // find existing object, if found skip further processing
            Map id = processorNode.idFromFlatRow(currentFlatRow);
            object = processorNode.getResolved(id);

            if (object == null) {

                DataRow row = processorNode.rowFromFlatRow(currentFlatRow);
                object = processorNode.getResolver().objectFromDataRow(row);

                processorNode.putResolved(id, object);
                processorNode.addObject(object, row);
            }

            // categorization by parent needed even if an object is already there
            // (many-to-many case)
            if (processorNode.isPartitionedByParent()) {

                PrefetchProcessorNode parent = (PrefetchProcessorNode) processorNode
                        .getParent();
                processorNode.linkToParent(object, parent.getLastResolved());
            }

            processorNode.setLastResolved(object);
            return processorNode.isJointChildren();
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return ((PrefetchProcessorNode) node).isJointChildren();
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }

        public void finishPrefetch(PrefetchTreeNode node) {
            // noop
        }
    }

    // processor that converts temporary associations between DataObjects to Cayenne
    // relationships and also fires snapshot update events
    final class PostProcessor implements PrefetchProcessor {

        public void finishPrefetch(PrefetchTreeNode node) {
        }

        public boolean startDisjointPrefetch(PrefetchTreeNode node) {
            ((PrefetchProcessorNode) node).connectToParents();
            return true;
        }

        public boolean startJointPrefetch(PrefetchTreeNode node) {
            PrefetchProcessorJointNode processorNode = (PrefetchProcessorJointNode) node;

            if (!processorNode.getObjects().isEmpty()) {
                cache.snapshotsUpdatedForObjects(
                        processorNode.getObjects(),
                        processorNode.getResolvedRows(),
                        queryMetadata.isRefreshingObjects());
                processorNode.connectToParents();
            }

            return true;
        }

        public boolean startPhantomPrefetch(PrefetchTreeNode node) {
            return true;
        }

        public boolean startUnknownPrefetch(PrefetchTreeNode node) {
            throw new CayenneRuntimeException("Unknown prefetch node: " + node);
        }
    }
}
