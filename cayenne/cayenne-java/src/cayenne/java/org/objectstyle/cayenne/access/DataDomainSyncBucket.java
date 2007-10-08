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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.Factory;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.NodeIdChangeOperation;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;

/**
 * A superclass of batch query wrappers.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
abstract class DataDomainSyncBucket {

    final Map objectsByEntity;
    final DataDomainFlushAction parent;

    List dbEntities;
    Map objEntitiesByDbEntity;

    DataDomainSyncBucket(DataDomainFlushAction parent) {
        this.objectsByEntity = new HashMap();
        this.parent = parent;
    }

    boolean isEmpty() {
        return objectsByEntity.isEmpty();
    }

    abstract void appendQueriesInternal(Collection queries);

    /**
     * Appends all queries originated in the bucket to provided collection.
     */
    void appendQueries(Collection queries) {

        if (!objectsByEntity.isEmpty()) {
            groupObjEntitiesBySpannedDbEntities();
            appendQueriesInternal(queries);
        }
    }

    void checkReadOnly(ObjEntity entity) throws CayenneRuntimeException {

        if (entity.isReadOnly()) {

            StringBuffer message = new StringBuffer();
            message
                    .append("Attempt to modify object(s) mapped to a read-only entity: ")
                    .append(entity.getName());

            if (entity != null) {
                message.append(" '").append(entity.getName()).append("'");
            }

            message.append(". Can't commit changes.");
            throw new CayenneRuntimeException(message.toString());
        }
    }

    private void groupObjEntitiesBySpannedDbEntities() {

        dbEntities = new ArrayList(objectsByEntity.size());
        objEntitiesByDbEntity = new HashMap(objectsByEntity.size() * 2);

        Iterator i = objectsByEntity.keySet().iterator();
        while (i.hasNext()) {
            ObjEntity objEntity = (ObjEntity) i.next();
            DbEntity dbEntity = objEntity.getDbEntity();

            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            if (objEntitiesForDbEntity == null) {
                objEntitiesForDbEntity = new ArrayList(1);
                dbEntities.add(dbEntity);
                objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
            }

            if (!objEntitiesForDbEntity.contains(objEntity)) {
                objEntitiesForDbEntity.add(objEntity);
            }

            // Note that this logic won't allow flattened attributes to span multiple
            // databases...
            Iterator j = objEntity.getAttributeMap().values().iterator();
            while (j.hasNext()) {
                ObjAttribute objAttribute = (ObjAttribute) j.next();
                if (!objAttribute.isCompound())
                    continue;
                dbEntity = (DbEntity) objAttribute.getDbAttribute().getEntity();
                objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);

                if (objEntitiesForDbEntity == null) {
                    objEntitiesForDbEntity = new ArrayList(1);
                    dbEntities.add(dbEntity);
                    objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
                }

                if (!objEntitiesForDbEntity.contains(objEntity)) {
                    objEntitiesForDbEntity.add(objEntity);
                }
            }
        }
    }

    void addDirtyObject(Object object, ObjEntity entity) {

        Collection objects = (Collection) objectsByEntity.get(entity);
        if (objects == null) {

            objects = new ArrayList();
            objectsByEntity.put(entity, objects);
        }

        objects.add(object);
    }

    void postprocess() {

        if (!objectsByEntity.isEmpty()) {

            CompoundDiff result = parent.getResultDiff();
            Map modifiedSnapshots = parent.getResultModifiedSnapshots();
            Collection deletedIds = parent.getResultDeletedIds();

            Iterator it = objectsByEntity.values().iterator();
            while (it.hasNext()) {

                Iterator objects = ((Collection) it.next()).iterator();
                while (objects.hasNext()) {
                    DataObject object = (DataObject) objects.next();
                    ObjectId id = object.getObjectId();

                    DataRow dataRow = object.getDataContext().currentSnapshot(object);
                    dataRow.setReplacesVersion(object.getSnapshotVersion());
                    object.setSnapshotVersion(dataRow.getVersion());

                    // record id change
                    if (id.isReplacementIdAttached()) {
                        ObjectId replacementId = id.createReplacementId();
                        result.add(new NodeIdChangeOperation(id, replacementId));

                        // classify replaced permanent ids as "deleted", as
                        // DataRowCache has no notion of replaced id...
                        if (!id.isTemporary()) {
                            deletedIds.add(id);
                        }

                        modifiedSnapshots.put(replacementId, dataRow);
                    }
                    else if (id.isTemporary()) {
                        throw new CayenneRuntimeException(
                                "Temporary ID hasn't been replaced on commit: " + object);
                    }
                    else {
                        modifiedSnapshots.put(id, dataRow);
                    }
                }
            }
        }
    }

    // a factory for extracting PKs generated on commit.
    final static class PropagatedValueFactory implements Factory {

        ObjectId masterID;
        String masterKey;

        PropagatedValueFactory(ObjectId masterID, String masterKey) {
            this.masterID = masterID;
            this.masterKey = masterKey;
        }

        public Object create() {
            Object value = masterID.getIdSnapshot().get(masterKey);
            if (value == null) {
                throw new CayenneRuntimeException("Can't extract a master key. "
                        + "Missing key ("
                        + masterKey
                        + "), master ID ("
                        + masterID
                        + ")");
            }

            return value;
        }
    }
}
