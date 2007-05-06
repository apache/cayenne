/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.PrefetchProcessor;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.QueryMetadata;

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
        LinkedList nodeStack;

        List mainResultRows;
        Map extraResultsByPath;

        TreeBuilder(List mainResultRows, Map extraResultsByPath) {
            this.mainResultRows = mainResultRows;
            this.extraResultsByPath = extraResultsByPath;
        }

        PrefetchProcessorNode buildTree(PrefetchTreeNode tree) {
            // reset state
            this.nodeStack = new LinkedList();
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
                    ? decorated = new PrefetchProcessorJointNode(getParent(), node
                            .getName())
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
            ObjRelationship relationship;
            ObjEntity entity;

            PrefetchProcessorNode currentNode = getParent();

            if (currentNode != null) {
                rows = (List) extraResultsByPath.get(node.getPath());
                relationship = (ObjRelationship) currentNode
                        .getResolver()
                        .getEntity()
                        .getRelationship(node.getName());

                if (relationship == null) {
                    throw new CayenneRuntimeException("No relationship with name '"
                            + node.getName()
                            + "' found in entity "
                            + currentNode.getResolver().getEntity().getName());
                }

                entity = (ObjEntity) relationship.getTargetEntity();
            }
            else {
                relationship = null;
                entity = queryMetadata.getObjEntity();
                rows = mainResultRows;
            }

            node.setDataRows(rows);
            node.setResolver(new ObjectResolver(context, entity, queryMetadata
                    .isRefreshingObjects(), queryMetadata.isResolvingInherited()));
            node.setIncoming(relationship);

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
            return (nodeStack.isEmpty()) ? null : (PrefetchProcessorNode) nodeStack
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
                    && processorNode.getIncoming().isFlattened()) {

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
                    && !processorNode.getIncoming().isFlattened()) {

                ObjEntity sourceObjEntity = null;
                String relatedIdPrefix = null;

                // determine resolution strategy
                ObjRelationship reverseRelationship = processorNode
                        .getIncoming()
                        .getReverseRelationship();

                // if null, prepare for manual matching
                if (reverseRelationship == null) {
                    relatedIdPrefix = processorNode
                            .getIncoming()
                            .getReverseDbRelationshipPath()
                            + ".";

                    sourceObjEntity = (ObjEntity) processorNode
                            .getIncoming()
                            .getSourceEntity();
                }

                Iterator it = objects.iterator();
                while (it.hasNext()) {
                    DataObject destinationObject = (DataObject) it.next();
                    DataObject sourceObject = null;

                    if (reverseRelationship != null) {
                        sourceObject = (DataObject) destinationObject
                                .readProperty(reverseRelationship.getName());
                    }
                    else {
                        DataContext context = destinationObject.getDataContext();
                        ObjectStore objectStore = context.getObjectStore();

                        // prefetched snapshots contain parent ids prefixed with
                        // relationship name.

                        DataRow snapshot = objectStore.getSnapshot(destinationObject
                                .getObjectId());

                        ObjectId id = processorNode.getResolver().createObjectId(
                                snapshot,
                                sourceObjEntity,
                                relatedIdPrefix);

                        sourceObject = (DataObject) objectStore.getNode(id);
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

            DataObject object = null;

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
