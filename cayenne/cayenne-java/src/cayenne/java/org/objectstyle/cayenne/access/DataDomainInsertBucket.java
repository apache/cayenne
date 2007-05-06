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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.InsertBatchQuery;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainInsertBucket extends DataDomainSyncBucket {

    DataDomainInsertBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    void appendQueriesInternal(Collection queries) {

        DataDomainDBDiffBuilder diffBuilder = new DataDomainDBDiffBuilder();

        EntitySorter sorter = parent.getDomain().getEntitySorter();
        sorter.sortDbEntities(dbEntities, false);

        Iterator i = dbEntities.iterator();
        while (i.hasNext()) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);

            InsertBatchQuery batch = new InsertBatchQuery(dbEntity, 27);

            Iterator j = objEntitiesForDbEntity.iterator();
            while (j.hasNext()) {
                ObjEntity entity = (ObjEntity) j.next();

                diffBuilder.reset(entity, dbEntity);

                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);

                // remove object set for dependent entity, so that it does not show up
                // on post processing
                List objects = (List) (isMasterDbEntity
                        ? objectsByEntity.get(entity)
                        : objectsByEntity.remove(entity));

                if (objects.isEmpty()) {
                    continue;
                }

                checkReadOnly(entity);

                if (isMasterDbEntity) {
                    createPermIdsForObjEntity(entity, objects);
                    sorter.sortObjectsForEntity(entity, objects, false);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    Map snapshot = diffBuilder.buildDBDiff(parent.objectDiff(o
                            .getObjectId()));

                    batch.add(snapshot, o.getObjectId());
                }
            }

            queries.add(batch);
        }
    }

    void createPermIdsForObjEntity(ObjEntity objEntity, List dataObjects) {

        if (dataObjects.isEmpty()) {
            return;
        }

        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode node = parent.getDomain().lookupDataNode(dbEntity.getDataMap());
        Collection generatedPks = getAutogeneratedKeys(node, dbEntity);

        if (generatedPks.isEmpty()) {
            return;
        }

        PkGenerator pkGenerator = node.getAdapter().getPkGenerator();

        Iterator i = dataObjects.iterator();
        while (i.hasNext()) {

            DataObject object = (DataObject) i.next();
            ObjectId id = object.getObjectId();
            if (id == null || !id.isTemporary()) {
                continue;
            }

            // modify replacement id directly...
            Map idMap = id.getReplacementIdMap();

            boolean autoPkDone = false;
            Iterator it = generatedPks.iterator();
            while (it.hasNext()) {
                DbAttribute dbAttr = (DbAttribute) it.next();
                String dbAttrName = dbAttr.getName();

                if (idMap.containsKey(dbAttrName)) {
                    continue;
                }

                // handle meaningful PK
                ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
                if (objAttr != null) {
                    idMap.put(dbAttrName, object.readPropertyDirectly(objAttr.getName()));
                    continue;
                }

                // only a single key can be generated from DB... if this is done already
                // in this loop, we must bail out.
                if (autoPkDone) {
                    throw new CayenneRuntimeException(
                            "Primary Key autogeneration only works for a single attribute.");
                }

                // finally, use database generation mechanism
                try {
                    Object pkValue = pkGenerator.generatePkForDbEntity(node, dbEntity);
                    idMap.put(dbAttrName, pkValue);
                    autoPkDone = true;
                }
                catch (Exception ex) {
                    throw new CayenneRuntimeException("Error generating PK: "
                            + ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Returns a collection of DbAttributes that should be generated by Cayenne.
     */
    // TODO, andrus 4/12/2006 - move to DbEntity in 2.0+
    Collection getAutogeneratedKeys(DataNode node, DbEntity entity) {
        boolean supportsGeneratedKeys = node.getAdapter().supportsGeneratedKeys();
        Iterator it = entity.getPrimaryKey().iterator();

        Collection generated = new ArrayList(1);
        while (it.hasNext()) {
            DbAttribute next = (DbAttribute) it.next();

            if (supportsGeneratedKeys && next.isGenerated()) {
                continue;
            }

            if (isPropagated(next)) {
                continue;
            }

            generated.add(next);
        }

        return generated;
    }

    // TODO, andrus 4/12/2006 - move to DbAttribute in 2.0+
    boolean isPropagated(DbAttribute attribute) {
        Iterator it = attribute.getEntity().getRelationships().iterator();
        while (it.hasNext()) {

            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            Iterator joins = dbRel.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
                if (attribute.getName().equals(join.getSourceName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
