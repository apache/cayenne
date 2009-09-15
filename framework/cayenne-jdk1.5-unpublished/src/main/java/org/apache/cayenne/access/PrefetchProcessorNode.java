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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.ToStringBuilder;

/**
 * A specialized PrefetchTreeNode used for disjoint prefetch resolving.
 * 
 * @since 1.2
 */
class PrefetchProcessorNode extends PrefetchTreeNode {

    List dataRows;
    List objects;

    ArcProperty incoming;
    ObjectResolver resolver;

    Map partitionByParent;
    boolean jointChildren;

    private Persistent lastResolved;
    private ParentAttachmentStrategy parentAttachmentStrategy;

    PrefetchProcessorNode(PrefetchProcessorNode parent, String segmentPath) {
        super(parent, segmentPath);
    }

    /**
     * Sets up derived flags and values for faster lookup during traversal. Called after
     * all properties are initialized.
     */
    void afterInit() {

        if (isPartitionedByParent()) {
            partitionByParent = new HashMap();
        }
    }

    /**
     * Creates a temporary association between child and parent objects. Permanent
     * relationship is set using the information created here, by calling
     * 'connectToParents'.
     */
    void linkToParent(Persistent object, Persistent parent) {
        if (parent != null && parent.getPersistenceState() != PersistenceState.HOLLOW) {

            // if a relationship is to-one (i.e. flattened to-one), can connect right
            // away.... write directly to prevent changing persistence state.
            if (incoming instanceof ToOneProperty) {
                incoming.writePropertyDirectly(parent, null, object);
            }
            else {

                List peers = (List) partitionByParent.get(parent);

                // wrap in a list even if relationship is to-one... will unwrap at the end
                // of the processing cycle.
                if (peers == null) {
                    peers = new ArrayList();
                    partitionByParent.put(parent, peers);
                }
                // checking for duplicates is needed in case of nested joint prefetches
                // when there is more than one row with the same combination of adjacent
                // parent and child...
                else if (peers.contains(object)) {
                    return;
                }

                peers.add(object);
            }
        }
    }

    void connectToParents() {

        // to-one's were connected earlier...
        if (isPartitionedByParent()) {

            // depending on whether parent is a "phantom" node,
            // use different strategy

            PrefetchProcessorNode parent = (PrefetchProcessorNode) getParent();
            boolean parentObjectsExist = parent.getObjects() != null
                    && parent.getObjects().size() > 0;
            if (incoming.getRelationship().isToMany()) {
                if (parentObjectsExist) {
                    connectToNodeParents(parent.getObjects());
                }
                else {
                    connectToFaultedParents();
                }
            }
            else {
                // optional to-one ... need to fill in unresolved relationships with
                // null...
                if (parentObjectsExist) {
                    clearNullRelationships(parent.getObjects());
                }
            }
        }
    }

    private final void clearNullRelationships(List parentObjects) {
        Iterator it = parentObjects.iterator();
        while (it.hasNext()) {
            Object object = it.next();
            if (incoming.readPropertyDirectly(object) instanceof Fault) {
                incoming.writePropertyDirectly(object, null, null);
            }
        }
    }

    private final void connectToNodeParents(List parentObjects) {

        Iterator it = parentObjects.iterator();
        while (it.hasNext()) {
            Persistent object = (Persistent) it.next();
            List related = (List) partitionByParent.get(object);
            connect(object, related);
        }
    }

    private final void connectToFaultedParents() {
        Iterator it = partitionByParent.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            Persistent object = (Persistent) entry.getKey();
            List related = (List) entry.getValue();
            connect(object, related);
        }
    }

    private final void connect(Persistent object, List related) {
        if (incoming.getRelationship().isToMany()) {
            ValueHolder toManyList = (ValueHolder) incoming.readProperty(object);

            // TODO, Andrus 11/15/2005 - if list is modified, shouldn't we attempt to
            // merge the changes instead of overwriting?
            toManyList.setValueDirectly(related != null ? related : new ArrayList(1));
        }
        else {
            // this should've been handled elsewhere
            throw new CayenneRuntimeException(
                    "To-one relationship wasn't handled properly: " + incoming.getName());
        }
    }

    List getDataRows() {
        return dataRows;
    }

    List getObjects() {
        return objects;
    }

    void setResolver(ObjectResolver resolver) {
        this.resolver = resolver;
    }

    ObjectResolver getResolver() {
        return resolver;
    }

    ArcProperty getIncoming() {
        return incoming;
    }

    void setIncoming(ArcProperty incoming) {
        this.incoming = incoming;
    }

    void setDataRows(List dataRows) {
        this.dataRows = dataRows;
    }

    void setObjects(List objects) {
        this.objects = objects;
    }

    boolean isJointChildren() {
        return jointChildren;
    }

    void setJointChildren(boolean jointChildren) {
        this.jointChildren = jointChildren;
    }

    boolean isPartitionedByParent() {
        return parent != null;
    }

    Persistent getLastResolved() {
        return lastResolved;
    }

    void setLastResolved(Persistent lastResolved) {
        this.lastResolved = lastResolved;
    }

    @Override
    public String toString() {
        String label = incoming != null ? incoming.getName() : "<root>";

        return new ToStringBuilder(this).append("incoming", label).append(
                "phantom",
                phantom).toString();
    }

    ParentAttachmentStrategy getParentAttachmentStrategy() {
        return parentAttachmentStrategy;
    }

    void setParentAttachmentStrategy(ParentAttachmentStrategy parentAttachmentStrategy) {
        this.parentAttachmentStrategy = parentAttachmentStrategy;
    }
}
