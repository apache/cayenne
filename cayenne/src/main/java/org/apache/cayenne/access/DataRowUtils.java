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
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.Util;

import java.util.Map;

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
     * @param plan the plan of the object's descriptor
     */
    static void mergeObjectWithSnapshot(
            DataContext context,
            Persistent object,
            DataRow snapshot,
            MergePlan plan) {

        int state = object.getPersistenceState();

        if (state == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(plan, object, snapshot, true);
        } else if (state != PersistenceState.COMMITTED && !plan.descriptor.getEntity().isReadOnly()) {
            forceMergeWithSnapshot(context, plan.descriptor, object, snapshot);
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
     * @param plan the plan of the object's descriptor
     */
    static void refreshObjectWithSnapshot(
            MergePlan plan,
            Persistent object,
            DataRow snapshot,
            boolean invalidateRelationships) {

        boolean isPartialSnapshot = false;

        DataRowLayout.Reader reader = plan.reader;
        AttributeProperty[] attributes = plan.attributes;
        for (int i = 0; i < attributes.length; i++) {
            AttributeProperty property = attributes[i];
            int column = plan.attributeColumns[i];
            Object value = reader.get(snapshot, column);

            // a null value is valid, so the column is only checked for when the value is null
            if (value != null || reader.has(snapshot, column)) {
                property.writePropertyDirectly(object, null, value);
            } else if (property.getAttribute().isLazy()) {
                property.writePropertyDirectly(object, null, new AttributeFault(property));
            } else {
                property.writePropertyDirectly(object, null, null);
                isPartialSnapshot = true;
            }
        }

        if (invalidateRelationships) {
            // "to many" relationships have no information to collect from snapshot
            for (ToManyProperty property : plan.toManys) {
                property.invalidate(object);
            }
            for (ToOneProperty property : plan.toOnes) {
                property.invalidate(object);
            }
        } else {
            ToOneProperty[] toOnes = plan.toOnes;
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
    private static void refreshToOne(ToOneProperty property, Persistent object, DataRow snapshot,
                                     DataRowLayout.Reader reader, int[] fkColumns, String[] targetNames) {
        Object current = property.readPropertyDirectly(object);
        if (property.isFaultValue(current)) {
            return;
        }

        if (fkColumns == null) {
            property.invalidate(object);
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
                    property.invalidate(object);
                    return;
                }
                nullFks++;
            } else if (currentPk == null || !Util.idValueEquals(fk, currentPk.get(targetNames[i]))) {
                property.invalidate(object);
                return;
            }
        }

        if (nullFks == fkColumns.length) {
            if (current != null) {
                property.writePropertyDirectly(object, current, null);
            }
        } else if (nullFks > 0) {
            // a multi-column FK that is only partially null is inconclusive
            property.invalidate(object);
        }
    }

    /**
     * Merges a snapshot, or a diff of one, into a MODIFIED or DELETED object: the properties the object has not
     * changed take the snapshot values.
     */
    static void forceMergeWithSnapshot(
            DataContext context,
            ClassDescriptor descriptor,
            Persistent object,
            Map<String, Object> snapshot) {

        ObjectDiff diff = context
                .getObjectStore()
                .getChangesByObjectId()
                .get(object.getObjectId());

        // to-many relationships have nothing to merge
        for (PropertyDescriptor property : descriptor.getProperties()) {
            switch (property) {
                case AttributeProperty attribute -> mergeAttribute(attribute, object, snapshot, diff);
                case ToOneProperty toOne -> mergeToOne(context, toOne, object, snapshot, diff);
                default -> {
                }
            }
        }
    }

    private static void mergeAttribute(
            AttributeProperty property,
            Persistent object,
            Map<String, Object> snapshot,
            ObjectDiff diff) {

        String dbAttrPath = property.getAttribute().getDbAttributePath().value();

        // supports merging of partial snapshots...
        // check for null is cheaper than double lookup for a key...
        // so check for partial snapshot only if the value is null
        Object newValue = snapshot.get(dbAttrPath);
        if (newValue != null || snapshot.containsKey(dbAttrPath)) {

            Object curValue = property.readPropertyDirectly(object);
            Object oldValue = diff != null ? diff.getSnapshotValue(property.getName()) : null;

            // if value not modified, update it from snapshot, otherwise leave it alone
            if (property.equals(curValue, oldValue) && !property.equals(newValue, curValue)) {
                property.writePropertyDirectly(object, oldValue, newValue);
            }
        }
    }

    private static void mergeToOne(
            DataContext context,
            ToOneProperty property,
            Persistent object,
            Map<String, Object> snapshot,
            ObjectDiff diff) {

        ObjRelationship relationship = property.getRelationship();
        if (!relationship.isToPK()) {
            return;
        }

        // TODO: will this work for flattened, how do we save snapshots for them?

        // if value not modified, update it from snapshot, otherwise leave it alone
        if (isToOneTargetModified(property, object, diff)) {
            return;
        }

        DbRelationship dbRelationship = relationship.getDbRelationships().getFirst();

        // must check before creating ObjectId because of partial snapshots
        if (!hasFK(dbRelationship, snapshot)) {
            return;
        }

        Map<String, Object> targetPk = dbRelationship.targetPkSnapshotWithSrcSnapshot(snapshot);
        ObjectId id = targetPk != null ? ObjectId.of(relationship.getTargetEntityName(), targetPk) : null;

        if (diff == null
                || !diff.containsArcSnapshot(relationship.getName())
                || !property.equals(id, diff.getArcSnapshotValue(relationship.getName()))) {

            if (id == null) {
                property.writeProperty(object, null, null);
            } else if (!relationship.isSourceDefiningTargetPrecenseAndType(context.getEntityResolver())) {
                // we can't use 'localObject' if relationship is optional or inheritance is involved
                // .. must turn to fault instead
                property.invalidate(object);
            } else {
                property.writeProperty(object, null, context.findOrCreateObject(id));
            }
        }
    }

    static boolean hasFK(DbRelationship relationship, Map<String, Object> snapshot) {
        for (final DbJoin join : relationship.getJoins()) {
            if (snapshot.get(join.getSourceName()) == null) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if an object has its to-one relationship target modified in memory.
     */
    static boolean isToOneTargetModified(
            ArcProperty property,
            Persistent object,
            ObjectDiff diff) {

        if (object.getPersistenceState() != PersistenceState.MODIFIED || diff == null) {
            return false;
        }

        Object value = property.readPropertyDirectly(object);
        if (property.isFaultValue(value)) {
            return false;
        }

        Persistent toOneTarget = (Persistent) value;
        ObjectId currentId = (toOneTarget != null) ? toOneTarget.getObjectId() : null;

        // if ObjectId is temporary, target is definitely modified...
        // this would cover NEW objects (what are the other cases of temp id??)
        if (currentId != null && currentId.isTemporary()) {
            return true;
        }

        if (!diff.containsArcSnapshot(property.getName())) {
            return false;
        }

        ObjectId targetId = diff.getArcSnapshotValue(property.getName());
        return !property.equals(currentId, targetId);
    }

    // not for instantiation
    DataRowUtils() {
    }
}
