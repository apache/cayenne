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
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A specialized PrefetchTreeNode used for disjoint prefetch resolving.
 * 
 * @since 1.2
 */
class PrefetchProcessorNode extends PrefetchTreeNode {

    List<DataRow> dataRows;
    List<Persistent> objects;

    ArcProperty incoming;
    ObjectResolver resolver;

    Map<Persistent, Partition> partitionByParent;
    boolean jointChildren;

    private Persistent lastResolved;
    private ParentAttachmentStrategy parentAttachmentStrategy;

    private boolean rowsVersionResolved;
    private long rowsVersion;

    PrefetchProcessorNode(PrefetchProcessorNode parent, String segmentPath) {
        super(parent, segmentPath);
    }

    /**
     * Sets up derived flags and values for faster lookup during traversal. Called after
     * all properties are initialized.
     */
    void afterInit() {
        if (isPartitionedByParent()) {
            partitionByParent = new HashMap<>();
        }
    }

    /**
     * Returns the snapshot version of the relationship data of this node: the version of the oldest statement among
     * its rows, or the parent node's version if this node has no rows. A relationship is written to a parent object
     * only if the object does not already reflect a newer snapshot than the relationship data.
     */
    long rowsVersion() {
        if (!rowsVersionResolved) {
            rowsVersionResolved = true;

            List<DataRow> rows = dataRows;
            if ((rows == null || rows.isEmpty()) && this instanceof PrefetchProcessorJointNode jointNode) {
                rows = jointNode.getResolvedRows();
            }

            if (rows != null && !rows.isEmpty()) {
                long version = Long.MAX_VALUE;
                for (DataRow row : rows) {
                    version = Math.min(version, row.getVersion());
                }
                rowsVersion = version;
            } else if (parent != null) {
                rowsVersion = ((PrefetchProcessorNode) parent).rowsVersion();
            } else {
                rowsVersion = SnapshotVersion.NONE;
            }
        }

        return rowsVersion;
    }

    /**
     * Creates a temporary association between child and parent objects. Permanent
     * relationship is set using the information created here, by calling
     * 'connectToParents'. The version is that of the snapshot the association was read from.
     */
    void linkToParent(Persistent object, Persistent parent, long version) {
        if (parent != null && parent.getPersistenceState() != PersistenceState.HOLLOW) {
            // if a relationship is to-one (i.e. flattened to-one), can connect right away....
            // write directly to prevent changing persistence state.
            if (incoming instanceof ToOneProperty) {
                store().withObjectLock(parent.getObjectId(), () -> {
                    if (parent.getPersistenceState() != PersistenceState.HOLLOW
                            && version >= parent.getSnapshotVersion()
                            && relationshipNotModified(parent, incoming)) {
                        incoming.writePropertyDirectly(parent, null, object);
                    }
                    return null;
                });
            } else {
                Partition partition = partitionByParent.computeIfAbsent(parent, p -> new Partition());
                if (partition.objects.contains(object)) {
                    // checking for duplicates is needed in case of nested joint prefetches
                    // when there is more than one row with the same combination of adjacent
                    // parent and child...
                    return;
                }
                partition.objects.add(object);
                partition.version = Math.min(partition.version, version);
            }
        }
    }

    // every node of a tree belongs to the one context being resolved
    private DataContextObjectStore store() {
        return resolver.context.getObjectStore();
    }

    boolean relationshipNotModified(Persistent object, ArcProperty property) {
        if(object.getPersistenceState() != PersistenceState.MODIFIED) {
            return true;
        }

        ObjectDiff diff = store().getChangesByObjectId().get(object.getObjectId());
        // check that there are no pending changes for that property
        if(diff == null) {
            return true;
        }

        if(diff.containsArcSnapshot(property.getName())) {
            return false;
        }

        PropertyDiffTester tester = new PropertyDiffTester(property);
        diff.apply(tester);
        return !tester.hasArcDiff();
    }

    void connectToParents() {

        // to-one's were connected earlier...
        if (isPartitionedByParent()) {

            // depending on whether parent is a "phantom" node,
            // use different strategy

            PrefetchProcessorNode parent = (PrefetchProcessorNode) getParent();
            boolean parentObjectsExist = parent.getObjects() != null && !parent.getObjects().isEmpty();
            if (incoming.getRelationship().isToMany()) {
                if (parentObjectsExist) {
                    connectToNodeParents(parent.getObjects());
                } else {
                    connectToFaultedParents();
                }
            } else {
                // optional to-one ...
                // need to fill in unresolved relationships with null...
                if (parentObjectsExist) {
                    clearNullRelationships(parent.getObjects());
                }
            }
        }
    }

    private void clearNullRelationships(List<Persistent> parentObjects) {
        // an unresolved to-one means a null FK in the parent row, so the parent rows version applies
        long version = ((PrefetchProcessorNode) parent).rowsVersion();

        for (Persistent object : parentObjects) {
            store().withObjectLock(object.getObjectId(), () -> {
                if (version >= object.getSnapshotVersion()
                        && incoming.readPropertyDirectly(object) instanceof Fault) {
                    incoming.writePropertyDirectly(object, null, null);
                }
                return null;
            });
        }
    }

    private void connectToNodeParents(List<Persistent> parentObjects) {
        for (Persistent parentObject : parentObjects) {
            connect(parentObject, partitionByParent.get(parentObject));
        }
    }

    private void connectToFaultedParents() {
        for (Map.Entry<Persistent, Partition> entry : partitionByParent.entrySet()) {
            connect(entry.getKey(), entry.getValue());
        }
    }

    private void connect(Persistent object, Partition partition) {
        if (!incoming.getRelationship().isToMany()) {
            // this should've been handled elsewhere
            throw new CayenneRuntimeException("To-one relationship wasn't handled properly: %s", incoming.getName());
        }

        // an empty list is the result of this node's rows too, so it carries their version
        List<Persistent> related = partition != null ? partition.objects : new ArrayList<>(1);
        long version = partition != null ? partition.version : rowsVersion();

        store().withObjectLock(object.getObjectId(), () -> {
            // a HOLLOW parent would need a fetch to get a list to resolve, so it is left to resolve lazily
            if (object.getPersistenceState() != PersistenceState.HOLLOW
                    && version >= object.getSnapshotVersion()
                    && relationshipNotModified(object, incoming)) {
                @SuppressWarnings("unchecked")
                ToManyHolder<Persistent> toMany = (ToManyHolder<Persistent>) incoming.readProperty(object);
                toMany.resolveWith(related);
            }
            return null;
        });
    }

    List<DataRow> getDataRows() {
        return dataRows;
    }

    List<Persistent> getObjects() {
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

    void setDataRows(List<DataRow> dataRows) {
        this.dataRows = dataRows;
    }

    void setObjects(List<Persistent> objects) {
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

    private static class PropertyDiffTester implements GraphChangeHandler {
        private final ArcProperty property;
        private boolean hasArcDiff;

        public PropertyDiffTester(ArcProperty property) {
            this.property = property;
        }

        boolean hasArcDiff() {
            return hasArcDiff;
        }

        @Override
        public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
            if(arcId.getForwardArc().equals(property.getName())) {
                hasArcDiff = true;
            }
        }

        @Override
        public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
            if(arcId.getForwardArc().equals(property.getName())) {
                hasArcDiff = true;
            }
        }
    }

    /**
     * Objects linked to one parent, and the version of the oldest snapshot the links were read from.
     */
    static final class Partition {
        final List<Persistent> objects = new ArrayList<>();
        long version = Long.MAX_VALUE;
    }
}
