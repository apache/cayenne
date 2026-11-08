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
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToManyRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A dynamic GraphDiff that represents a delta between object simple properties
 * at diff creation time and its current state.
 */
public class ObjectDiff extends NodeDiff {

    private final String entityName;

    private final CompiledEntity entity;

    private Collection<NodeDiff> otherDiffs;

    private Map<String, Object> snapshot;
    private Map<String, Object> arcSnapshot;
    private Map<String, Object> currentArcSnapshot;
    private Map<ArcOperation, ArcOperation> flatIds;
    private Map<ArcOperation, ArcOperation> phantomFks;

    private final Persistent object;

    ObjectDiff(final Persistent object) {

        super(object.getObjectId());

        // retain the object, as ObjectStore may have weak references to
        // registered
        // objects and we can't allow it to deallocate dirty objects.
        this.object = object;

        CompiledMapping mapping = object.getObjectContext().getMapping();

        this.entityName = object.getObjectId().getEntityName();
        this.entity = mapping.entity(entityName);

        int state = object.getPersistenceState();

        // take snapshot of simple properties and arcs used for optimistic
        // locking..

        if (state == PersistenceState.COMMITTED || state == PersistenceState.DELETED
                || state == PersistenceState.MODIFIED) {

            boolean lock = entity.lockType() == CompiledEntity.LOCK_TYPE_OPTIMISTIC;

            this.snapshot = new HashMap<>();
            this.arcSnapshot = new HashMap<>();

            for (CompiledAttribute attribute : entity.attributes()) {
                snapshot.put(attribute.name(), attribute.readProperty(object));
            }

            for (CompiledRelationship relationship : entity.relationships()) {
                if (relationship instanceof CompiledToOneRelationship toOne) {
                    boolean isUsedForLocking = toOne.isUsedForLocking();

                    // eagerly resolve optimistically locked relationships
                    Object target = (lock && isUsedForLocking)
                            ? toOne.readProperty(object)
                            : toOne.readPropertyDirectly(object);

                    if (target instanceof Persistent persistentTarget) {
                        target = persistentTarget.getObjectId();
                    }
                    // else - null || Fault

                    arcSnapshot.put(toOne.name(), target);
                }
            }
        }
    }

    Object getObject() {
        return object;
    }

    CompiledEntity getEntity() {
        return entity;
    }

    public Object getSnapshotValue(String propertyName) {
        return snapshot != null ? snapshot.get(propertyName) : null;
    }

    public ObjectId getArcSnapshotValue(String propertyName) {
        Object value = arcSnapshot != null ? arcSnapshot.get(propertyName) : null;

        if (value instanceof Fault fault) {
            Persistent target = (Persistent) fault.resolveFault(object);

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
        if (value instanceof Fault fault) {
            Persistent target = (Persistent) fault.resolveFault(object);

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

        collection.add(new NodeDiff(id, diffId) {

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

    void addDiff(NodeDiff diff, DataContextObjectStore parent) {

        boolean addDiff = true;

        if (diff instanceof ArcOperation arcDiff) {
            ObjectId targetId = arcDiff.getTargetNodeId();
            ArcId arcId = arcDiff.getArcId();

            CompiledRelationship property = getEntity().relationship(arcId.getForwardArc());

            // note that some collection properties implement
            // 'SingleObjectArcProperty',
            // so we cant't do 'instanceof SingleObjectArcProperty'
            // TODO: andrus, 3.22.2006 - should we consider this a bug?

            if (property == null && arcId.getForwardArc().startsWith(DbPathExp.DB_PREFIX)) {
                addPhantomFkDiff(arcDiff);
                addDiff = false;
            } else if (property instanceof CompiledToManyRelationship) {

                // record flattened op changes

                CompiledRelationship relationship = property;
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
                } else if (property.reverse() == null) {

                    // register complementary arc diff
                    ArcId arc = arcId.getReverseId();
                    //new ArcId(DbPathExp.DB_PREFIX + property.reverseDbPath(), property.name());
                    ArcOperation complementaryOp = new ArcOperation(targetId, arcDiff.getNodeId(), arc, arcDiff.isDelete());
                    parent.registerDiff(targetId, complementaryOp);
                }

            } else if (property instanceof CompiledToOneRelationship) {

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

        CompiledTable table = entity.table();
        CompiledJoin join = table.join(arcId.substring(DbPathExp.DB_PREFIX.length()));

        if (join.isToMany()) {
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

        int state = object.getPersistenceState();
        if (state == PersistenceState.NEW || state == PersistenceState.DELETED) {
            return false;
        }

        // check phantom mods. To-many changes are tracked as flattened diffs
        for (CompiledAttribute attribute : getEntity().attributes()) {
            if (isModified(attribute)) {
                return false;
            }
        }

        for (CompiledRelationship relationship : getEntity().relationships()) {
            if (relationship instanceof CompiledToOneRelationship toOne && isModified(toOne)) {
                return false;
            }
        }

        return true;
    }

    private boolean isModified(CompiledAttribute attribute) {
        Object oldValue = snapshot.get(attribute.name());
        Object newValue = attribute.readProperty(object);
        return !attribute.equals(oldValue, newValue);
    }

    private boolean isModified(CompiledToOneRelationship toOne) {
        if (arcSnapshot == null) {
            return false;
        }

        Object newValue = toOne.readPropertyDirectly(object);
        if (newValue instanceof Fault) {
            return false;
        }

        Object oldValue = arcSnapshot.get(toOne.name());
        return !Objects.equals(oldValue, newValue != null ? ((Persistent) newValue).getObjectId() : null);
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

    private void applySimplePropertyChanges(GraphChangeHandler handler) {

        for (CompiledAttribute attribute : getEntity().attributes()) {
            Object newValue = attribute.readProperty(object);

            // no baseline to compare
            if (snapshot == null) {

                if (newValue != null) {
                    handler.nodePropertyChanged(id, attribute.name(), null, newValue);
                }
            }
            // have baseline to compare
            else {
                Object oldValue = snapshot.get(attribute.name());

                if (!attribute.equals(oldValue, newValue)) {
                    handler.nodePropertyChanged(id, attribute.name(), oldValue, newValue);

                }
            }
        }
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

        private final ObjectId targetId;
        private final ArcId arcId;
        private final boolean delete;

        ArcOperation(ObjectId id, ObjectId targetId, ArcId arcId, boolean delete) {

            super(id);
            this.targetId = targetId;
            this.arcId = arcId;
            this.delete = delete;
        }

        boolean isDelete() {
            return delete;
        }

        @Override
        public int hashCode() {
            // assuming String and ObjectId provide a good hashCode
            return 31 * arcId.hashCode() + targetId.hashCode();
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

            if (!(object instanceof ArcOperation other)) {
                return false;
            }
            return arcId.equals(other.arcId) && Objects.equals(targetId, other.targetId);
        }

        @Override
        public void apply(GraphChangeHandler tracker) {

            if (delete) {
                tracker.arcDeleted(id, targetId, arcId);
            } else {
                tracker.arcCreated(id, targetId, arcId);
            }
        }

        @Override
        public void undo(GraphChangeHandler tracker) {
            throw new UnsupportedOperationException();
        }

        public ArcId getArcId() {
            return arcId;
        }

        public ObjectId getTargetNodeId() {
            return targetId;
        }
    }
}
