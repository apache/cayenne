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

package org.apache.cayenne.access.flush;

import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Visitor that fills replacement map of {@link ObjectId}s of inserted objects.
 *
 * @since 4.2
 */
class PermanentObjectIdVisitor implements DbRowOpVisitor<Void> {

    private final DataDomain dataDomain;
    private final EntityResolver resolver;

    private ClassDescriptor lastDescriptor;
    private ObjEntity lastObjEntity;
    private DbEntity lastDbEntity;
    private DataNode lastNode;
    private String lastEntityName;

    PermanentObjectIdVisitor(DataDomain dataDomain) {
        this.dataDomain = dataDomain;
        this.resolver = dataDomain.getEntityResolver();
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        ObjectId id = dbRow.getChangeId();
        if (id == null || !id.isTemporary()) {
            return null;
        }

        if((lastObjEntity == null && lastDbEntity == null) || !id.getEntityName().equals(lastEntityName)) {
            lastEntityName = id.getEntityName();
            if(lastEntityName.startsWith(ASTDbPath.DB_PREFIX)) {
                lastDbEntity = resolver.getDbEntity(lastEntityName.substring(ASTDbPath.DB_PREFIX.length()));
                lastObjEntity = null;
                lastDescriptor = null;
                lastNode = dataDomain.lookupDataNode(lastDbEntity.getDataMap());
            } else {
                lastObjEntity = resolver.getObjEntity(id.getEntityName());
                lastDbEntity = lastObjEntity.getDbEntity();
                lastDescriptor = resolver.getClassDescriptor(lastObjEntity.getName());
                lastNode = dataDomain.lookupDataNode(lastObjEntity.getDataMap());
            }
        }

        createPermanentId(dbRow);
        return null;
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        // flattened ids will be temporary for delete operation, replace it
        if(dbRow.getChangeId().isTemporary() && dbRow.getChangeId().isReplacementIdAttached()) {
            dbRow.setChangeId(dbRow.getChangeId().createReplacementId());
        }
        return null;
    }

    private void createPermanentId(InsertDbRowOp dbRow) {
        ObjectId id = dbRow.getChangeId();
        boolean supportsGeneratedKeys = lastNode.getAdapter().supportsGeneratedKeys();
        PkGenerator pkGenerator = lastNode.getAdapter().getPkGenerator();

        // modify replacement id directly...
        Map<String, Object> idMap = id.getReplacementIdMap();

        boolean autoPkDone = false;

        for (DbAttribute dbAttr : lastDbEntity.getPrimaryKeys()) {
            String dbAttrName = dbAttr.getName();

            if (idMap.containsKey(dbAttrName)) {
                continue;
            }

            // handle meaningful PK
            if(lastObjEntity != null) {
                ObjAttribute objAttr = lastObjEntity.getAttributeForDbAttribute(dbAttr);
                if (objAttr != null) {
                    Object value = lastDescriptor.getProperty(objAttr.getName()).readPropertyDirectly(dbRow.getObject());
                    if (value != null) {
                        // primitive 0 has to be treated as NULL, or otherwise we can't generate PK for POJO's
                        Class<?> javaClass = objAttr.getJavaClass();
                        if (!javaClass.isPrimitive() || !(value instanceof Number) || ((Number) value).intValue() != 0) {
                            idMap.put(dbAttrName, value);
                            continue;
                        }
                    }
                }
            }

            // skip db-generated
            if (supportsGeneratedKeys && dbAttr.isGenerated()) {
                // mark that this attribute should be generated at insert time
                idMap.put(dbAttrName, IdGenerationMarker.marker());
                continue;
            }

            // only a single key can be generated from DB... if this is done already in this loop, we must bail out.
            if (autoPkDone) {
                throw new CayenneRuntimeException("Primary Key autogeneration only works for a single attribute.");
            }

            // finally, use database generation mechanism
            try {
                Object pkValue = pkGenerator.generatePk(lastNode, dbAttr);
                idMap.put(dbAttrName, pkValue);
                autoPkDone = true;
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK: %s", ex,  ex.getMessage());
            }
        }
    }

}
