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

import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledEmbeddedAttribute;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToManyRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;
import org.apache.cayenne.util.Util;

import java.util.Map;
import java.util.Objects;

/**
 * DataRowUtils contains a number of static methods to work with DataRows. This is a
 * helper class for DataContext and ObjectStore.
 * 
 * @since 1.1
 */
class DataRowUtils {

    /**
     * Merges changes reflected in snapshot map to the object. Changes made to attributes
     * and to-one relationships will be merged. In case an object is already modified,
     * modified properties will not be overwritten.
     *
     * @param plan the plan of the object's entity
     */
    static void mergeObjectWithSnapshot(
            DataContext context,
            Persistent object,
            DataRow snapshot,
            MergePlan plan) {

        int state = object.getPersistenceState();

        if (state == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(plan, object, snapshot, true);
        } else if (state != PersistenceState.COMMITTED && !plan.entity.isReadOnly()) {
            forceMergeWithSnapshot(context, plan.entity, object, snapshot);
        } else {
            // do not invalidate relationships: to-many lists might have just been prefetched, and a to-one target
            // whose FK did not change is still valid
            refreshObjectWithSnapshot(plan, object, snapshot, false);
        }
    }

    /**
     * Replaces all object attribute values with snapshot values. Sets object state to
     * COMMITTED, unless the snapshot is partial in which case the state is set to HOLLOW.
     * With 'invalidateRelationships' all relationships are turned into faults. Otherwise to-many relationships
     * are left alone, and a resolved to-one target is kept when the snapshot FK still points to it.
     *
     * @param plan the plan of the object's entity
     */
    static void refreshObjectWithSnapshot(
            MergePlan plan,
            Persistent object,
            DataRow snapshot,
            boolean invalidateRelationships) {

        boolean isPartialSnapshot = false;

        // the embeddables must exist before their attributes are written
        for (CompiledEmbeddedAttribute embedded : plan.embeddedAttributes) {
            embedded.injectValueHolder(object);
        }

        DataRowLayout.Reader reader = plan.reader;
        CompiledAttribute[] attributes = plan.attributes;
        for (int i = 0; i < attributes.length; i++) {
            CompiledAttribute attribute = attributes[i];
            int column = plan.attributeColumns[i];
            Object value = reader.get(snapshot, column);

            // a null value is valid, so the column is only checked for when the value is null
            if (value != null || reader.has(snapshot, column)) {
                attribute.writePropertyDirectly(object, null, value);
            } else if (attribute.isLazy()) {
                attribute.writePropertyDirectly(object, null, attribute.fault());
            } else {
                attribute.writePropertyDirectly(object, null, null);
                isPartialSnapshot = true;
            }
        }

        if (invalidateRelationships) {
            // "to many" relationships have no information to collect from snapshot
            for (CompiledToManyRelationship toMany : plan.toManys) {
                toMany.invalidate(object);
            }
            for (CompiledToOneRelationship toOne : plan.toOnes) {
                toOne.invalidate(object);
            }
        } else {
            CompiledToOneRelationship[] toOnes = plan.toOnes;
            for (int i = 0; i < toOnes.length; i++) {
                refreshToOne(toOnes[i], object, snapshot, reader, plan.toOneFkColumns[i], plan.toOneTargetNames[i]);
            }
        }

        object.setPersistenceState(isPartialSnapshot
                ? PersistenceState.HOLLOW
                : PersistenceState.COMMITTED);
    }

    /**
     * Keeps a resolved to-one target when the snapshot FK still points to it, otherwise replaces it with a fault.
     * Replacing an unchanged target with a fault would be pointless: the fault resolves from the cached snapshot to
     * the very same object. It is also harmful on a shared DataContext, where other threads would observe the
     * transient fault and have to resolve it, each contending for the ObjectStore lock.
     *
     * @param fkColumns   the FK columns, join by join, or null when the FK does not define the arc. A flattened or a
     *                    to-dependent-PK relationship is defined by the target rows, so it is invalidated as before
     * @param targetNames the target PK column of each join
     */
    private static void refreshToOne(CompiledToOneRelationship toOne, Persistent object, DataRow snapshot,
                                     DataRowLayout.Reader reader, int[] fkColumns, String[] targetNames) {
        Object current = toOne.readPropertyDirectly(object);
        if (toOne.isFaultValue(current)) {
            return;
        }

        if (fkColumns == null) {
            toOne.invalidate(object);
            return;
        }

        // the FK is compared with the current target id join by join, without building an intermediate id
        Map<String, Object> currentPk = current != null ? ((Persistent) current).getObjectId().getIdSnapshot() : null;
        int nullFks = 0;

        for (int i = 0; i < fkColumns.length; i++) {
            Object fk = reader.get(snapshot, fkColumns[i]);
            if (fk == null) {
                if (!reader.has(snapshot, fkColumns[i])) {
                    // partial snapshot
                    toOne.invalidate(object);
                    return;
                }
                nullFks++;
            } else if (currentPk == null || !Util.idValueEquals(fk, currentPk.get(targetNames[i]))) {
                toOne.invalidate(object);
                return;
            }
        }

        if (nullFks == fkColumns.length) {
            if (current != null) {
                toOne.writePropertyDirectly(object, current, null);
            }
        } else if (nullFks > 0) {
            // a multi-column FK that is only partially null is inconclusive
            toOne.invalidate(object);
        }
    }

    /**
     * Merges a snapshot, or a diff of one, into a MODIFIED or DELETED object: the properties the object has not
     * changed take the snapshot values.
     */
    static void forceMergeWithSnapshot(
            DataContext context,
            CompiledEntity entity,
            Persistent object,
            Map<String, Object> snapshot) {

        ObjectDiff diff = context
                .getObjectStore()
                .getChangesByObjectId()
                .get(object.getObjectId());

        // an embeddable the application reset to null is recreated before the snapshot is merged into it
        for (CompiledEmbeddedAttribute embedded : entity.embeddedAttributes()) {
            embedded.injectValueHolder(object);
        }
        for (CompiledAttribute attribute : entity.attributes()) {
            mergeAttribute(attribute, object, snapshot, diff);
        }

        // to-many relationships have nothing to merge
        for (CompiledRelationship relationship : entity.relationships()) {
            if (relationship instanceof CompiledToOneRelationship toOne) {
                mergeToOne(context, toOne, object, snapshot, diff);
            }
        }
    }

    private static void mergeAttribute(
            CompiledAttribute attribute,
            Persistent object,
            Map<String, Object> snapshot,
            ObjectDiff diff) {

        String dbAttrPath = attribute.dbPath().value();

        // supports merging of partial snapshots...
        // check for null is cheaper than double lookup for a key...
        // so check for partial snapshot only if the value is null
        Object newValue = snapshot.get(dbAttrPath);
        if (newValue != null || snapshot.containsKey(dbAttrPath)) {

            Object curValue = attribute.readPropertyDirectly(object);
            Object oldValue = diff != null ? diff.getSnapshotValue(attribute.name()) : null;

            // if value not modified, update it from snapshot, otherwise leave it alone
            if (attribute.equals(curValue, oldValue) && !attribute.equals(newValue, curValue)) {
                attribute.writePropertyDirectly(object, oldValue, newValue);
            }
        }
    }

    private static void mergeToOne(
            DataContext context,
            CompiledToOneRelationship toOne,
            Persistent object,
            Map<String, Object> snapshot,
            ObjectDiff diff) {

        CompiledJoin join = toOne.joins().getFirst();

        // the inverse side of a one-to-one is left alone: the snapshot says nothing about it
        if (!join.isToPrimaryKey()) {
            return;
        }

        // if value not modified, update it from snapshot, otherwise leave it alone
        if (isToOneTargetModified(toOne, object, diff)) {
            return;
        }

        // must check before creating ObjectId because of partial snapshots
        if (!hasFK(join, snapshot)) {
            return;
        }

        // the first hop FK doesn't determine the target of a flattened to-one, so it is turned into a fault
        if (toOne.isFlattened()) {
            toOne.invalidate(object);
            return;
        }

        ObjectId id = ObjectSnapshots.targetId(toOne, snapshot);

        if (diff == null
                || !diff.containsArcSnapshot(toOne.name())
                || !Objects.equals(id, diff.getArcSnapshotValue(toOne.name()))) {

            if (id == null) {
                toOne.writeProperty(object, null, null);
            } else if (!isTargetDeterminedByFK(toOne)) {
                // we can't use 'localObject' if relationship is optional or inheritance is involved
                // .. must turn to fault instead
                toOne.invalidate(object);
            } else {
                toOne.writeProperty(object, null, context.findOrCreateObject(id));
            }
        }
    }

    static boolean hasFK(CompiledJoin relationship, Map<String, Object> snapshot) {
        for (final CompiledColumnPair join : relationship.columnPairs()) {
            if (snapshot.get(join.source().name()) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if a non-null FK on the source fully determines the target of a relationship: the target row is
     * guaranteed to exist by referential integrity, and its entity is known. Such a target can be materialized as a
     * HOLLOW object from the FK alone, without a query. This requires the FK to be on the source side (see
     * {@link CompiledRelationship#isForeignKey()}) and referencing the target PK, no qualifier on the target entity
     * or its super entities that could filter out an existing row, and no subclasses of the target entity.
     */
    static boolean isTargetDeterminedByFK(CompiledRelationship relationship) {
        if (!relationship.isForeignKey()) {
            return false;
        }

        // an FK to a non-PK column doesn't give the target ObjectId
        if (!relationship.isToPrimaryKey()) {
            return false;
        }

        if (isQualifiedEntity(relationship.target())) {
            return false;
        }

        return !relationship.target().hasSubEntities();
    }

    // true if the entity or any of its super entities has a limiting qualifier
    private static boolean isQualifiedEntity(CompiledEntity entity) {
        for (CompiledEntity e = entity; e != null; e = e.superEntity()) {
            if (e.declaredQualifier() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if an object has its to-one relationship target modified in memory.
     */
    static boolean isToOneTargetModified(
            CompiledRelationship relationship,
            Persistent object,
            ObjectDiff diff) {

        if (object.getPersistenceState() != PersistenceState.MODIFIED || diff == null) {
            return false;
        }

        Object value = relationship.readPropertyDirectly(object);
        if (relationship.isFaultValue(value)) {
            return false;
        }

        Persistent toOneTarget = (Persistent) value;
        ObjectId currentId = (toOneTarget != null) ? toOneTarget.getObjectId() : null;

        // if ObjectId is temporary, target is definitely modified...
        // this would cover NEW objects (what are the other cases of temp id??)
        if (currentId != null && currentId.isTemporary()) {
            return true;
        }

        if (!diff.containsArcSnapshot(relationship.name())) {
            return false;
        }

        ObjectId targetId = diff.getArcSnapshotValue(relationship.name());
        return !Objects.equals(currentId, targetId);

    }

    // not for instantiation
    DataRowUtils() {
    }
}
