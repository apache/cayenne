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

import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;
import org.apache.cayenne.util.Util;

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
     */
    static void mergeObjectWithSnapshot(
            DataContext context,
            ClassDescriptor descriptor,
            Persistent object,
            DataRow snapshot) {

        int state = object.getPersistenceState();

        if (state == PersistenceState.HOLLOW || descriptor.getEntity().isReadOnly()) {
            refreshObjectWithSnapshot(descriptor, object, snapshot, true);
        }
        else if (state != PersistenceState.COMMITTED) {
            forceMergeWithSnapshot(context, descriptor, object, snapshot);
        }
        else {
            // do not invalidate to-many relationships, since they might have
            // just been prefetched...
            refreshObjectWithSnapshot(descriptor, object, snapshot, false);
        }
    }

    /**
     * Replaces all object attribute values with snapshot values. Sets object state to
     * COMMITTED, unless the snapshot is partial in which case the state is set to HOLLOW
     */
    static void refreshObjectWithSnapshot(
            ClassDescriptor descriptor,
            final Persistent object,
            final DataRow snapshot,
            final boolean invalidateToManyRelationships) {

        final boolean[] isPartialSnapshot = new boolean[1];

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute attr = property.getAttribute();
                String dbAttrPath = attr.getDbAttributePath();

                Object value = snapshot.get(dbAttrPath);
                property.writePropertyDirectly(object, null, value);

                // note that a check "snaphsot.get(..) == null" would be incorrect in this
                // case, as NULL value is entirely valid; still save a map lookup by
                // checking for the null value first
                if (value == null && !snapshot.containsKey(dbAttrPath)) {
                    isPartialSnapshot[0] = true;
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                // "to many" relationships have no information to collect from
                // snapshot
                if (invalidateToManyRelationships) {
                    property.invalidate(object);
                }

                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                property.invalidate(object);
                return true;
            }
        });

        object.setPersistenceState(isPartialSnapshot[0]
                ? PersistenceState.HOLLOW
                : PersistenceState.COMMITTED);
    }

    static void forceMergeWithSnapshot(
            final DataContext context,
            ClassDescriptor descriptor,
            final Persistent object,
            final DataRow snapshot) {

        final ObjectDiff diff = context
                .getObjectStore()
                .getChangesByObjectId()
                .get(object.getObjectId());

        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                String dbAttrPath = property.getAttribute().getDbAttributePath();

                // supports merging of partial snapshots...
                // check for null is cheaper than double lookup
                // for a key... so check for partial snapshot
                // only if the value is null
                Object newValue = snapshot.get(dbAttrPath);
                if (newValue != null || snapshot.containsKey(dbAttrPath)) {

                    Object curValue = property.readPropertyDirectly(object);
                    Object oldValue = diff != null ? diff.getSnapshotValue(property
                            .getName()) : null;

                    // if value not modified, update it from snapshot,
                    // otherwise leave it alone
                    if (Util.nullSafeEquals(curValue, oldValue)
                            && !Util.nullSafeEquals(newValue, curValue)) {
                        property.writePropertyDirectly(object, oldValue, newValue);
                    }
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                // noop - nothing to merge
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                ObjRelationship relationship = property.getRelationship();
                if (relationship.isToPK()) {
                    // TODO: will this work for flattened, how do we save snapshots for
                    // them?

                    // if value not modified, update it from snapshot,
                    // otherwise leave it alone
                    if (!isToOneTargetModified(property, object, diff)) {

                        DbRelationship dbRelationship = relationship
                                .getDbRelationships()
                                .get(0);

                        // must check before creating ObjectId because of partial
                        // snapshots
                        if (hasFK(dbRelationship, snapshot)) {
                            ObjectId id = snapshot.createTargetObjectId(
                                    relationship.getTargetEntityName(),
                                    dbRelationship);

                            if (diff == null
                                    || !diff.containsArcSnapshot(relationship.getName())
                                    || !Util.nullSafeEquals(id, diff
                                            .getArcSnapshotValue(relationship.getName()))) {

                                if (id == null) {
                                    property.writeProperty(object, null, null);
                                }
                                else {
                                    // we can't use 'localObject' if relationship is
                                    // optional or inheritance is involved
                                    // .. must turn to fault instead
                                    if (!relationship
                                            .isSourceDefiningTargetPrecenseAndType(context
                                                    .getEntityResolver())) {
                                        property.invalidate(object);
                                    }
                                    else {
                                        property.writeProperty(
                                                object,
                                                null,
                                                context.findOrCreateObject(id));
                                    }
                                }
                            }
                        }
                    }
                }
                return true;
            }
        });
    }

    static boolean hasFK(DbRelationship relationship, Map<String, Object> snapshot) {
        for (final DbJoin join : relationship.getJoins()) {
            if (!snapshot.containsKey(join.getSourceName())) {
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

        if (property.isFault(object)) {
            return false;
        }

        Persistent toOneTarget = (Persistent) property.readPropertyDirectly(object);
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
        return !Util.nullSafeEquals(currentId, targetId);
    }

    // not for instantiation
    DataRowUtils() {
    }
}
