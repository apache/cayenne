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
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A dynamic GraphDiff that represents a delta between object simple properties
 * at diff creation time and its current state.
 */
public class ObjectDiff extends NodeDiff {

    private final String entityName;

    private transient ClassDescriptor classDescriptor;

    private Collection<NodeDiff> otherDiffs;

    private Map<String, Object> snapshot;
    private Map<String, Object> arcSnapshot;
    private Map<String, Object> currentArcSnapshot;
    private Map<ArcOperation, ArcOperation> flatIds;
    private Map<ArcOperation, ArcOperation> phantomFks;

    private final ObjectStoreEntry entry;

    ObjectDiff(final ObjectStoreEntry entry) {

        super(entry.persistent().getObjectId());

        // retain the object, as ObjectStore may have weak references to registered objects,
        // and we can't allow it to deallocate dirty objects.
        this.entry = entry;

        EntityResolver entityResolver = object().getObjectContext().getEntityResolver();

        this.entityName = object().getObjectId().getEntityName();
        this.classDescriptor = entityResolver.getClassDescriptor(entityName);

        int state = object().getPersistenceState();

        // take snapshot of simple properties and arcs used for optimistic
        // locking..

        if (state == PersistenceState.COMMITTED || state == PersistenceState.DELETED
                || state == PersistenceState.MODIFIED) {

            final ObjEntity entity = entityResolver.getObjEntity(entityName);
            final boolean lock = entity.getLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC;

            this.snapshot = new HashMap<>();
            this.arcSnapshot = new HashMap<>();

            classDescriptor.visitProperties(new PropertyVisitor() {

                @Override
                public boolean visitAttribute(AttributeProperty property) {
                    snapshot.put(property.getName(), property.readProperty(object()));
                    return true;
                }

                @Override
                public boolean visitToMany(ToManyProperty property) {
                    return true;
                }

                @Override
                public boolean visitToOne(ToOneProperty property) {
                    boolean isUsedForLocking = entity.getRelationship(property.getName()).isUsedForLocking();
                    
                    // eagerly resolve optimistically locked relationships
                    Object target = (lock && isUsedForLocking)
                            ? property.readProperty(object())
                            : property.readPropertyDirectly(object());

                    if (target instanceof Persistent) {
                        target = ((Persistent) target).getObjectId();
                    }
                    // else - null || Fault

                    arcSnapshot.put(property.getName(), target);
                    return true;
                }
            });
        }
    }

    Persistent object() {
        return entry.persistent();
    }

    ClassDescriptor getClassDescriptor() {
        // class descriptor is initiated in constructor, but is nullified on
        // serialization
        if (classDescriptor == null) {
            EntityResolver entityResolver = object().getObjectContext().getEntityResolver();
            this.classDescriptor = entityResolver.getClassDescriptor(entityName);
        }

        return classDescriptor;
    }

    public Object getSnapshotValue(String propertyName) {
        return snapshot != null ? snapshot.get(propertyName) : null;
    }

    public ObjectId getArcSnapshotValue(String propertyName) {
        Object value = arcSnapshot != null ? arcSnapshot.get(propertyName) : null;

        if (value instanceof Fault) {
            Persistent target = (Persistent) ((Fault) value).resolveFault(object(), propertyName);

            value = target != null ? target.getObjectId() : null;
            arcSnapshot.put(propertyName, value);
        }

        return (ObjectId) value;
    }

    /**
     * @since 4.2
     */
    public ObjectId getCurrentArcSnapshotValue(String propertyName) {
        Object value = currentArcSnapshot != null ? currentArcSnapshot.get(propertyName) : null;
        if (value instanceof Fault) {
            Persistent target = (Persistent) ((Fault) value).resolveFault(object(), propertyName);

            value = target != null ? target.getObjectId() : null;
            currentArcSnapshot.put(propertyName, value);
        }
        return (ObjectId) value;
    }

    boolean containsArcSnapshot(String propertyName) {
        return arcSnapshot != null && arcSnapshot.containsKey(propertyName);
    }

    /**
     * Appends individual diffs to collection.
     */
    void appendDiffs(Collection<NodeDiff> collection) {

        if (otherDiffs != null) {
            collection.addAll(otherDiffs);
        }

        collection.add(new NodeDiff(nodeId, diffId) {

            @Override
            public void apply(GraphChangeHandler tracker) {
                applySimplePropertyChanges(tracker);
            }

            @Override
            public void undo(GraphChangeHandler tracker) {
                throw new UnsupportedOperationException();
            }
        });
    }

    void addDiff(NodeDiff diff, ObjectStore parent) {

        boolean addDiff = true;

        if (diff instanceof ArcOperation) {

            ArcOperation arcDiff = (ArcOperation) diff;
            Object targetId = arcDiff.getTargetNodeId();
            ArcId arcId = arcDiff.getArcId();

            ArcProperty property = (ArcProperty) getClassDescriptor().getProperty(arcId.getForwardArc());

            // note that some collection properties implement
            // 'SingleObjectArcProperty',
            // so we cant't do 'instanceof SingleObjectArcProperty'
            // TODO: andrus, 3.22.2006 - should we consider this a bug?

            if (property == null && arcId.getForwardArc().startsWith(ASTDbPath.DB_PREFIX)) {
                addPhantomFkDiff(arcDiff);
                addDiff = false;
            } else if (property instanceof ToManyProperty) {

                // record flattened op changes

                ObjRelationship relationship = property.getRelationship();
                if (relationship.isFlattened()) {

                    if (flatIds == null) {
                        flatIds = new HashMap<>();
                    }

                    ArcOperation oldOp = flatIds.put(arcDiff, arcDiff);

                    // "delete" cancels "create" and vice versa...
                    if (oldOp != null && oldOp.isDelete() != arcDiff.isDelete()) {
                        addDiff = false;
                        flatIds.remove(arcDiff);

                        if (otherDiffs != null) {
                            otherDiffs.remove(oldOp);
                        }
                    }
                } else if (property.getComplimentaryReverseArc() == null) {

                    // register complimentary arc diff
                    ArcId arc = arcId.getReverseId();
                    //new ArcId(ASTDbPath.DB_PREFIX + property.getComplimentaryReverseDbRelationshipPath(), property.getName());
                    ArcOperation complimentaryOp = new ArcOperation(targetId, arcDiff.getNodeId(), arc, arcDiff.isDelete());
                    parent.registerDiff(targetId, complimentaryOp);
                }

            } else if (property instanceof ToOneProperty) {

                if (currentArcSnapshot == null) {
                    currentArcSnapshot = new HashMap<>();
                }

                currentArcSnapshot.put(arcId.getForwardArc(), targetId);
            } else {
                String message = (property == null) ? "No property for arcId " + arcId
                        : "Unrecognized property for arcId " + arcId + ": " + property;
                throw new CayenneRuntimeException(message);
            }
        }

        if (addDiff) {
            if (otherDiffs == null) {
                otherDiffs = new ArrayList<>(3);
            }

            otherDiffs.add(diff);
        }
    }

    private void addPhantomFkDiff(ArcOperation arcDiff) {
        String arcId = arcDiff.getArcId().toString();

        DbEntity dbEntity = classDescriptor.getEntity().getDbEntity();
        DbRelationship dbRelationship = dbEntity.getRelationship(arcId.substring(ASTDbPath.DB_PREFIX.length()));

        if (dbRelationship.isToMany()) {
            return;
        }

        if (currentArcSnapshot == null) {
            currentArcSnapshot = new HashMap<>();
        }

        currentArcSnapshot.put(arcId, arcDiff.getTargetNodeId());

        if (phantomFks == null) {
            phantomFks = new HashMap<>();
        }

        ArcOperation oldOp = phantomFks.put(arcDiff, arcDiff);

        // "delete" cancels "create" and vice versa...
        if (oldOp != null && oldOp.isDelete() != arcDiff.isDelete()) {
            phantomFks.remove(arcDiff);

            if (otherDiffs != null) {
                otherDiffs.remove(oldOp);
            }
        }

    }

    /**
     * Checks whether at least a single property is modified.
     */
    @Override
    public boolean isNoop() {

        // if we have no baseline to compare with, assume that there are changes
        if (snapshot == null) {
            return false;
        }

        if (flatIds != null && !flatIds.isEmpty()) {
            return false;
        }

        if (phantomFks != null && !phantomFks.isEmpty()) {
            return false;
        }

        int state = object().getPersistenceState();
        if (state == PersistenceState.NEW || state == PersistenceState.DELETED) {
            return false;
        }

        // check phantom mods

        final boolean[] modFound = new boolean[1];
        getClassDescriptor().visitProperties(new PropertyVisitor() {

            @Override
            public boolean visitAttribute(AttributeProperty property) {

                Object oldValue = snapshot.get(property.getName());
                Object newValue = property.readProperty(object());

                if (!property.equals(oldValue, newValue)) {
                    modFound[0] = true;
                }

                return !modFound[0];
            }

            @Override
            public boolean visitToMany(ToManyProperty property) {
                // flattened changes
                return true;
            }

            @Override
            public boolean visitToOne(ToOneProperty property) {
                if (arcSnapshot == null) {
                    return true;
                }

                Object newValue = property.readPropertyDirectly(object());
                if (newValue instanceof Fault) {
                    return true;
                }

                Object oldValue = arcSnapshot.get(property.getName());
                if (!property.equals(oldValue, newValue != null ? ((Persistent) newValue).getObjectId() : null)) {
                    modFound[0] = true;
                }

                return !modFound[0];
            }
        });

        return !modFound[0];
    }

    @Override
    public void undo(GraphChangeHandler handler) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void apply(final GraphChangeHandler handler) {

        if (otherDiffs != null) {
            for (final GraphDiff diff : otherDiffs) {
                diff.apply(handler);
            }
        }

        // phantomFks are not in 'otherDiffs', while flattened diffs are
        if (phantomFks != null) {
            for (GraphDiff diff : phantomFks.keySet()) {
                diff.apply(handler);
            }
        }

        applySimplePropertyChanges(handler);
    }

    private void applySimplePropertyChanges(final GraphChangeHandler handler) {

        getClassDescriptor().visitProperties(new PropertyVisitor() {

            @Override
            public boolean visitAttribute(AttributeProperty property) {

                Object newValue = property.readProperty(object());

                // no baseline to compare
                if (snapshot == null) {

                    if (newValue != null) {
                        handler.nodePropertyChanged(nodeId, property.getName(), null, newValue);
                    }
                }
                // have baseline to compare
                else {
                    Object oldValue = snapshot.get(property.getName());

                    if (!property.equals(oldValue, newValue)) {
                        handler.nodePropertyChanged(nodeId, property.getName(), oldValue, newValue);
                    }
                }

                return true;
            }

            @Override
            public boolean visitToMany(ToManyProperty property) {
                return true;
            }

            @Override
            public boolean visitToOne(ToOneProperty property) {
                return true;
            }
        });
    }

    /**
     * This is used to update faults.
     */
    void updateArcSnapshot(String propertyName, Persistent object) {
        if (arcSnapshot == null) {
            arcSnapshot = new HashMap<>();
        }

        arcSnapshot.put(propertyName, object != null ? object.getObjectId() : null);
    }

    static final class ArcOperation extends NodeDiff {

        private Object targetNodeId;
        private ArcId arcId;
        private boolean delete;

        ArcOperation(Object nodeId, Object targetNodeId, ArcId arcId, boolean delete) {

            super(nodeId);
            this.targetNodeId = targetNodeId;
            this.arcId = arcId;
            this.delete = delete;
        }

        boolean isDelete() {
            return delete;
        }

        @Override
        public int hashCode() {
            // assuming String and ObjectId provide a good hashCode
            return 31 * arcId.hashCode() + targetNodeId.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            // compare ignoring op type...
            if (object == this) {
                return true;
            }

            if (object == null) {
                return false;
            }

            if (!(object instanceof ArcOperation)) {
                return false;
            }

            ArcOperation other = (ArcOperation) object;
            return arcId.equals(other.arcId) && Util.nullSafeEquals(targetNodeId, other.targetNodeId);
        }

        @Override
        public void apply(GraphChangeHandler tracker) {

            if (delete) {
                tracker.arcDeleted(nodeId, targetNodeId, arcId);
            } else {
                tracker.arcCreated(nodeId, targetNodeId, arcId);
            }
        }

        @Override
        public void undo(GraphChangeHandler tracker) {
            throw new UnsupportedOperationException();
        }

        public ArcId getArcId() {
            return arcId;
        }

        public Object getTargetNodeId() {
            return targetNodeId;
        }
    }
}
