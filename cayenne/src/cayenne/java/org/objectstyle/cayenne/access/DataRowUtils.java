/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * DataRowUtils contains a number of static methods to work with DataRows. This
 * is a helper class for DataContext and ObjectStore
 * 
 * @author Andrei Adamchik
 * @since 1.1
 */
class DataRowUtils {

    /**
     * Replaces all object attribute values with snapshot values. Sets object
     * state to COMMITTED, unless the snapshot is partial in which case the
     * state is set to HOLLOW
     */
    static void refreshObjectWithSnapshot(
        ObjEntity objEntity,
        DataObject object,
        DataRow snapshot,
        boolean invalidateToManyRelationships) {

        Map attrMap = objEntity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        boolean isPartialSnapshot = false;
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);
            String dbAttrPath = attr.getDbAttributePath();
            object.writePropertyDirectly(attrName, snapshot.get(dbAttrPath));
            if (!snapshot.containsKey(dbAttrPath)) {
                //Note the distinction between
                // 1) the map returning null because there was no mapping
                // for that key and
                // 2) returning null because 'null' was the value mapped
                // for that key.
                // If the first case (this clause) then snapshot is only
                // partial
                isPartialSnapshot = true;
            }
        }

        Iterator rit = objEntity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {

                // "to many" relationships have no information to collect from
                // snapshot
                // initialize a new empty list if requested, but otherwise
                // ignore snapshot data

                Object toManyList = object.readPropertyDirectly(rel.getName());

                if (toManyList == null) {
                    object.writePropertyDirectly(rel.getName(), Fault.getToManyFault());
                }
                else if (
                    invalidateToManyRelationships && toManyList instanceof ToManyList) {
                    ((ToManyList) toManyList).invalidateObjectList();
                }

                continue;
            }

            // set a shared fault to indicate any kind of unresolved to-one
            object.writePropertyDirectly(rel.getName(), Fault.getToOneFault());
            /*
             * ObjEntity targetEntity = (ObjEntity) rel.getTargetEntity();
             * Class targetClass = targetEntity.getJavaClass(); // handle toOne
             * flattened relationship if (rel.isFlattened()) { // A flattened
             * toOne relationship must be a series of // toOne dbRelationships.
             * Initialize fault for it, since // creating a hollow object won't
             * be right...
             * 
             * continue; }
             * 
             * DbRelationship dbRel = (DbRelationship)
             * rel.getDbRelationships().get(0); // dependent to one
             * relationship is optional // use fault, since we do not know
             * whether it is null or not... if (dbRel.isToDependentPK()) {
             * object.writePropertyDirectly(rel.getName(),
             * RelationshipFault.getInstance()); continue; }
             * 
             * ObjectId id = snapshot.createTargetObjectId(targetClass, dbRel);
             * DataObject targetObject = (id != null) ?
             * context.registeredObject(id) : null;
             *  
             */
        }

        if (isPartialSnapshot) {
            object.setPersistenceState(PersistenceState.HOLLOW);
        }
        else {
            object.setPersistenceState(PersistenceState.COMMITTED);
            object.setSnapshotVersion(snapshot.getVersion());
        }

    }

    static void forceMergeWithSnapshot(
        ObjEntity entity,
        DataObject anObject,
        DataRow snapshot) {

        DataContext context = anObject.getDataContext();
        Map oldSnap =
            context.getObjectStore().getSnapshot(anObject.getObjectId(), context);

        // attributes
        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute attr = (ObjAttribute) attrMap.get(attrName);

            //processing compound attributes correctly
            String dbAttrPath = attr.getDbAttributePath();

            // supports merging of partial snapshots...
            // check for null is cheaper than double lookup
            // for a key... so check for partial snapshot
            // only if the value is null
            Object newVal = snapshot.get(dbAttrPath);
            if (newVal == null && !snapshot.containsKey(dbAttrPath)) {
                continue;
            }

            Object curVal = anObject.readPropertyDirectly(attrName);
            Object oldVal = oldSnap.get(dbAttrPath);

            // if value not modified, update it from snapshot,
            // otherwise leave it alone
            if (Util.nullSafeEquals(curVal, oldVal)
                && !Util.nullSafeEquals(newVal, curVal)) {
                anObject.writePropertyDirectly(attrName, newVal);
            }
        }

        // merge to-one relationships
        Iterator rit = entity.getRelationships().iterator();
        while (rit.hasNext()) {
            ObjRelationship rel = (ObjRelationship) rit.next();
            if (rel.isToMany()) {
                continue;
            }

            // TODO: will this work for flattened, how do we save snapshots for
            // them?

            // if value not modified, update it from snapshot,
            // otherwise leave it alone
            if (!isToOneTargetModified(rel, anObject, oldSnap)
                && isJoinAttributesModified(rel, snapshot, oldSnap)) {

                DbRelationship dbRelationship =
                    (DbRelationship) rel.getDbRelationships().get(0);

                ObjectId id =
                    snapshot.createTargetObjectId(
                        ((ObjEntity) rel.getTargetEntity()).getJavaClass(
                            Configuration.getResourceLoader()),
                        dbRelationship);
                DataObject target = (id != null) ? context.registeredObject(id) : null;

                anObject.writePropertyDirectly(rel.getName(), target);
            }
        }
    }

    /**
     * Merges changes reflected in snapshot map to the object. Changes made to
     * attributes and to-one relationships will be merged. In case an object is
     * already modified, modified properties will not be overwritten.
     */
    static void mergeObjectWithSnapshot(
        ObjEntity entity,
        DataObject anObject,
        Map snapshot) {

        // TODO: once we use DataRow consistently instead of a Map, this line
        // should go away.
        // Instead method signiture should include "DataRow".
        DataRow dataRow =
            (snapshot instanceof DataRow) ? (DataRow) snapshot : new DataRow(snapshot);

        if (entity.isReadOnly()
            || anObject.getPersistenceState() == PersistenceState.HOLLOW) {
            refreshObjectWithSnapshot(entity, anObject, dataRow, true);
        }
        else if (anObject.getPersistenceState() == PersistenceState.COMMITTED) {
            // do not invalidate to-many relationships, since they might have
            // just been prefetched...
            refreshObjectWithSnapshot(entity, anObject, dataRow, false);
        }
        else {
            forceMergeWithSnapshot(entity, anObject, dataRow);
        }
    }

    /**
     * Checks if a new snapshot has a modified to-one relationship compared to
     * the cached snapshot.
     */
    static boolean isJoinAttributesModified(
        ObjRelationship relationship,
        Map newSnapshot,
        Map storedSnapshot) {

        Iterator it =
            ((DbRelationship) relationship.getDbRelationships().get(0))
                .getJoins()
                .iterator();
        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            String propertyName = join.getSourceName();

            // for equality to be true, snapshot must contain all matching pk
            // values
            if (!Util
                .nullSafeEquals(
                    newSnapshot.get(propertyName),
                    storedSnapshot.get(propertyName))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if an object has its to-one relationship target modified in
     * memory.
     */
    static boolean isToOneTargetModified(
        ObjRelationship relationship,
        DataObject object,
        Map storedSnapshot) {

        if (object.getPersistenceState() != PersistenceState.MODIFIED) {
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

        // check if ObjectId map is a subset of a stored snapshot;
        // this is an equality condition
        Iterator it =
            ((DbRelationship) relationship.getDbRelationships().get(0))
                .getJoins()
                .iterator();

        while (it.hasNext()) {
            DbJoin join = (DbJoin) it.next();
            String propertyName = join.getSourceName();

            if (currentId == null) {
                // for equality to be true, snapshot must contain no pk values
                if (storedSnapshot.get(propertyName) != null) {
                    return true;
                }
            }
            else {
                // for equality to be true, snapshot must contain all matching
                // pk values
                // note that we must use target entity names to extract id
                // values.
                if (!Util
                    .nullSafeEquals(
                        currentId.getValueForAttribute(join.getTarget().getName()),
                        storedSnapshot.get(propertyName))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Instantiation is not allowed.
     */
    DataRowUtils() {
        super();
    }
}
