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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;

final class PrefetchProcessorTreeBuilder implements PrefetchProcessor {

    private QueryMetadata queryMetadata;
    private DataContext context;
    private PrefetchProcessorNode root;
    private LinkedList<PrefetchProcessorNode> nodeStack;

    private ClassDescriptor descriptor;
    private List mainResultRows;
    private Map extraResultsByPath;

    PrefetchProcessorTreeBuilder(HierarchicalObjectResolver objectTreeResolver,
            List mainResultRows, Map extraResultsByPath) {
        this.context = objectTreeResolver.context;
        this.queryMetadata = objectTreeResolver.queryMetadata;
        this.mainResultRows = mainResultRows;
        this.extraResultsByPath = extraResultsByPath;
        this.descriptor=objectTreeResolver.descriptor;
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
            PrefetchProcessorNode decorated = new PrefetchProcessorNode(getParent(), node
                    .getName());

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
            arc = (ArcProperty) currentNode.getResolver().getDescriptor().getProperty(
                    node.getName());

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
            if(this.descriptor!=null){
                descriptor=this.descriptor;
            }else{
                descriptor = queryMetadata.getClassDescriptor();
            }
            rows = mainResultRows;
        }

        node.setDataRows(rows);

        node.setIncoming(arc);
        if (node.getParent() != null && !node.isJointPrefetch()) {
            node.setResolver(new HierarchicalObjectResolverNode(
                    node,
                    context,
                    descriptor,
                    queryMetadata.isRefreshingObjects()));
        }
        else {
            node.setResolver(new ObjectResolver(context, descriptor, queryMetadata
                    .isRefreshingObjects()));
        }

        if (node.getParent() == null || node.getParent().isPhantom()) {
            node.setParentAttachmentStrategy(new NoopParentAttachmentStrategy());
        }
        else if (node.isJointPrefetch()) {
            node
                    .setParentAttachmentStrategy(new StackLookupParentAttachmentStrategy(
                            node));
        }
        else if (node
                .getIncoming()
                .getRelationship()
                .isSourceIndependentFromTargetChange()) {
            node.setParentAttachmentStrategy(new JoinedIdParentAttachementStrategy(
                    context.getGraphManager(),
                    node));
        }
        else {
            node
                    .setParentAttachmentStrategy(new ResultScanParentAttachmentStrategy(
                            node));
        }

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
        return (nodeStack.isEmpty()) ? null : nodeStack.getLast();
    }
}