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

import java.util.Iterator;
import java.util.Map;

import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * DataRowUtils contains a number of static methods to work with DataRows. This is a
 * helper class for DataContext and ObjectStore.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
class DataRowUtils {

    /**
     * Merges changes reflected in snapshot map to the object. Changes made to attributes
     * and to-one relationships will be merged. In case an object is already modified,
     * modified properties will not be overwritten.
     */
    static void mergeObjectWithSnapshot(
            ObjEntity entity,
            DataObject object,
            DataRow snapshot) {

        int state = object.getPersistenceState();

        if (entity.isReadOnly() || state == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(entity, object, snapshot, true);
        }
        else if (state != PersistenceState.COMMITTED) {
            forceMergeWithSnapshot(entity, object, snapshot);
        }
        else {
            // do not invalidate to-many relationships, since they might have
            // just been prefetched...
            refreshObjectWithSnapshot(entity, object, snapshot, false);
        }
    }

    /**
     * Replaces all object attribute values with snapshot values. Sets object state to
     * COMMITTED, unless the snapshot is partial in which case the state is set to HOLLOW
     */
    static void refreshObjectWithSnapshot(
            ObjEntity objEntity,
            DataObject object,
            DataRow snapshot,
            boolean invalidateToManyRelationships) {

        boolean isPartialSnapshot = false;

        Map attrMap = objEntity.getAttributeMap();
        if (!attrMap.isEmpty()) {
            Iterator it = attrMap.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                ObjAttribute attr = (ObjAttribute) entry.getValue();
                String dbAttrPath = attr.getDbAttributePath();

                Object value = snapshot.get(dbAttrPath);
                object.writePropertyDirectly(attr.getName(), value);

                // note that a check "snaphsot.get(..) == null" would be incorrect in this
                // case, as NULL value is entirely valid; still save a map lookup by
                // checking for the null value first
                if (value == null && !snapshot.containsKey(dbAttrPath)) {
                    isPartialSnapshot = true;
                }
            }
        }

        Map rMap = objEntity.getRelationshipMap();
        if (!rMap.isEmpty()) {
            Iterator it = rMap.entrySet().iterator();
            while (it.hasNext()) {

                Map.Entry e = (Map.Entry) it.next();
                ObjRelationship rel = (ObjRelationship) e.getValue();

                if (rel.isToMany()) {

                    // "to many" relationships have no information to collect from
                    // snapshot initialize a new empty list if requested, but otherwise
                    // ignore snapshot data

                    Object toManyList = object.readPropertyDirectly(rel.getName());

                    if (toManyList == null) {
                        object.writePropertyDirectly(rel.getName(), Fault
                                .getToManyFault());
                    }
                    else if (invalidateToManyRelationships
                            && toManyList instanceof ValueHolder) {
                        ((ValueHolder) toManyList).invalidate();
                    }
                }
                else {
                    // set a shared fault to indicate any kind of unresolved to-one
                    object.writePropertyDirectly(rel.getName(), Fault.getToOneFault());
                }
            }
        }

        object.setPersistenceState(isPartialSnapshot
                ? PersistenceState.HOLLOW
                : PersistenceState.COMMITTED);
    }

    static void forceMergeWithSnapshot(
            ObjEntity entity,
            DataObject object,
            DataRow snapshot) {

        DataContext context = object.getDataContext();
        ObjectDiff diff = (ObjectDiff) context
                .getObjectStore()
                .getChangesByObjectId()
                .get(object.getObjectId());

        // attributes
        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();

            String attrName = (String) entry.getKey();
            ObjAttribute attr = (ObjAttribute) entry.getValue();

            // processing compound attributes correctly
            String dbAttrPath = attr.getDbAttributePath();

            // supports merging of partial snapshots...
            // check for null is cheaper than double lookup
            // for a key... so check for partial snapshot
            // only if the value is null
            Object newVal = snapshot.get(dbAttrPath);
            if (newVal == null && !snapshot.containsKey(dbAttrPath)) {
                continue;
            }

            Object curVal = object.readPropertyDirectly(attrName);
            Object oldVal = diff != null ? diff.getSnapshotValue(attrName) : null;

            // if value not modified, update it from snapshot,
            // otherwise leave it alone
            if (Util.nullSafeEquals(curVal, oldVal)
                    && !Util.nullSafeEquals(newVal, curVal)) {
                object.writePropertyDirectly(attrName, newVal);
            }
        }

        // merge to-one relationships
        Iterator rit = entity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            
            // !isToPK check is for handling one-to-one rels pointing to FK... this
            // feature will require more work in the future
            if (rel.isToMany() || !rel.isToPK()) {
                continue;
            }

            // TODO: will this work for flattened, how do we save snapshots for
            // them?

            // if value not modified and snapshot contains an FK, update it from snapshot,
            // otherwise leave it alone
            if (!isToOneTargetModified(rel, object, diff)) {

                DbRelationship dbRelationship = (DbRelationship) rel
                        .getDbRelationships()
                        .get(0);

                // must check before creating ObjectId because of partial snapshots
                if (hasFK(dbRelationship, snapshot)) {

                    ObjectId id = snapshot.createTargetObjectId(
                            rel.getTargetEntityName(),
                            dbRelationship);

                    if (diff == null
                            || !diff.containsArcSnapshot(rel.getName())
                            || !Util.nullSafeEquals(id, diff.getArcSnapshotValue(rel
                                    .getName()))) {

                        Object target;
                        if (id == null) {
                            target = null;
                        }
                        else {
                            // if inheritance is involved, we can't use 'localObject' ..
                            // must
                            // turn to fault instead
                            ObjEntity targetEntity = (ObjEntity) rel.getTargetEntity();
                            if (context.getEntityResolver().lookupInheritanceTree(
                                    targetEntity) != null) {
                                target = Fault.getToOneFault();
                            }
                            else {
                                target = context.localObject(id, null);
                            }
                        }

                        object.writeProperty(rel.getName(), target);
                    }
                }
            }
        }
    }
    
    static boolean hasFK(DbRelationship relationship, Map snapshot) {
        Iterator joins = relationship.getJoins().iterator();
        while(joins.hasNext()) {
            DbJoin join = (DbJoin) joins.next();
            if(!snapshot.containsKey(join.getSourceName())) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Checks if an object has its to-one relationship target modified in memory.
     */
    static boolean isToOneTargetModified(
            ObjRelationship relationship,
            DataObject object,
            ObjectDiff diff) {

        if (object.getPersistenceState() != PersistenceState.MODIFIED || diff == null) {
            return false;
        }

        Object targetObject = object.readPropertyDirectly(relationship.getName());
        if (targetObject instanceof Fault) {
            return false;
        }

        DataObject toOneTarget = (DataObject) targetObject;
        ObjectId currentId = (toOneTarget != null) ? toOneTarget.getObjectId() : null;

        // if ObjectId is temporary, target is definitely modified...
        // this would cover NEW objects (what are the other cases of temp id??)
        if (currentId != null && currentId.isTemporary()) {
            return true;
        }

        if (!diff.containsArcSnapshot(relationship.getName())) {
            return false;
        }

        ObjectId targetId = diff.getArcSnapshotValue(relationship.getName());
        return !Util.nullSafeEquals(currentId, targetId);
    }

    // not for instantiation
    DataRowUtils() {
    }
}
