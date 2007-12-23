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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainInsertBucket extends DataDomainSyncBucket {

    DataDomainInsertBucket(DataDomainFlushAction parent) {
        super(parent);
    }

    @Override
    void appendQueriesInternal(Collection<Query> queries) {

        DataDomainDBDiffBuilder diffBuilder = new DataDomainDBDiffBuilder();

        EntitySorter sorter = parent.getDomain().getEntitySorter();
        sorter.sortDbEntities(dbEntities, false);

        for (DbEntity dbEntity : dbEntities) {

            Collection<ClassDescriptor> objEntitiesForDbEntity = descriptorsByDbEntity
                    .get(dbEntity);

            InsertBatchQuery batch = new InsertBatchQuery(dbEntity, 27);
            for (ClassDescriptor descriptor : objEntitiesForDbEntity) {

                diffBuilder.reset(descriptor.getEntity(), dbEntity);

                boolean isMasterDbEntity = (descriptor.getEntity().getDbEntity() == dbEntity);

                // remove object set for dependent entity, so that it does not show up
                // on post processing
                List<Persistent> objects = isMasterDbEntity ? objectsByDescriptor
                        .get(descriptor) : objectsByDescriptor.remove(descriptor);

                if (objects.isEmpty()) {
                    continue;
                }

                checkReadOnly(descriptor.getEntity());

                if (isMasterDbEntity) {
                    createPermIdsForObjEntity(descriptor, objects);
                    sorter.sortObjectsForEntity(descriptor.getEntity(), objects, false);
                }

                for (Persistent o : objects) {

                    Map<Object, Object> snapshot = diffBuilder.buildDBDiff(parent
                            .objectDiff(o.getObjectId()));

                    batch.add(snapshot, o.getObjectId());
                }
            }

            queries.add(batch);
        }
    }

    void createPermIdsForObjEntity(
            ClassDescriptor descriptor,
            Collection<Persistent> objects) {

        if (objects.isEmpty()) {
            return;
        }

        ObjEntity objEntity = descriptor.getEntity();
        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode node = parent.getDomain().lookupDataNode(dbEntity.getDataMap());
        boolean supportsGeneratedKeys = node.getAdapter().supportsGeneratedKeys();

        PkGenerator pkGenerator = node.getAdapter().getPkGenerator();

        for (Persistent object : objects) {
            ObjectId id = object.getObjectId();
            if (id == null || !id.isTemporary()) {
                continue;
            }

            // modify replacement id directly...
            Map<String, Object> idMap = id.getReplacementIdMap();

            boolean autoPkDone = false;

            for (DbAttribute dbAttr : dbEntity.getPrimaryKeys()) {
                String dbAttrName = dbAttr.getName();

                if (idMap.containsKey(dbAttrName)) {
                    continue;
                }

                // handle meaningful PK
                ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(dbAttr);
                if (objAttr != null) {

                    Object value = descriptor
                            .getProperty(objAttr.getName())
                            .readPropertyDirectly(object);

                    if (value != null) {
                        Class<?> javaClass = objAttr.getJavaClass();
                        if (javaClass.isPrimitive()
                                && value instanceof Number
                                && ((Number) value).intValue() == 0) {
                            // primitive 0 has to be treated as NULL, or otherwise we
                            // can't generate PK for POJO's
                        }
                        else {

                            idMap.put(dbAttrName, value);
                            continue;
                        }
                    }
                }

                // skip db-generated
                if (supportsGeneratedKeys && dbAttr.isGenerated()) {
                    continue;
                }

                // skip propagated
                if (isPropagated(dbAttr)) {
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

    // TODO, andrus 4/12/2006 - move to DbAttribute in 2.0+
    boolean isPropagated(DbAttribute attribute) {
        Iterator<?> it = attribute.getEntity().getRelationships().iterator();
        while (it.hasNext()) {

            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            for (DbJoin join : dbRel.getJoins()) {
                if (attribute.getName().equals(join.getSourceName())) {
                    return true;
                }
            }
        }

        return false;
    }
}
