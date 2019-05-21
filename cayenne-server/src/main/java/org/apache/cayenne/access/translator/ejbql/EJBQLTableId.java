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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Iterator;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

/**
 * A helper class representing an "id" of a database table during EJBQL translation. EJBQL
 * "ids" point to ObjEntities, but during translation we need id's that represent both
 * "root" tables that map back to an ObjEntity, as well as joined tables for flattened
 * attributes and relationships. EJBQLTableId is intended to represent both types of
 * tables.
 * 
 * @since 3.0
 */
class EJBQLTableId {

    private static String appendPath(EJBQLTableId baseId, String dbPathSuffix) {
        if (baseId.getDbPath() == null) {
            return dbPathSuffix;
        }

        if (dbPathSuffix == null) {
            return baseId.getDbPath();
        }

        return baseId.getDbPath() + "." + dbPathSuffix;
    }

    private String entityId;
    private String dbPath;

    EJBQLTableId(String entityId) {
        this(entityId, null);
    }

    EJBQLTableId(EJBQLTableId baseId, String dbPathSuffix) {
        this(baseId.getEntityId(), appendPath(baseId, dbPathSuffix));
    }

    EJBQLTableId(String entityId, String dbPath) {

        if (entityId == null) {
            throw new NullPointerException("Null entityId");
        }

        this.entityId = entityId;
        this.dbPath = dbPath;
    }
    
    boolean isPrimaryTable() {
        return dbPath == null;
    }

    /**
     * Returns a DbEntity corresponding to the ID, that could be a root entity for the id,
     * or a joined entity.
     */
    DbEntity getDbEntity(EJBQLTranslationContext context) {
        ClassDescriptor descriptor = context.getEntityDescriptor(entityId);
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();

        if (dbPath == null) {
            return rootEntity;
        }

        DbRelationship r = null;
        Iterator<?> it = rootEntity.resolvePathComponents(dbPath);
        while (it.hasNext()) {
            r = (DbRelationship) it.next();
        }

        return (DbEntity) r.getTargetEntity();
    }

    String getEntityId() {
        return entityId;
    }

    String getDbPath() {
        return dbPath;
    }

    @Override
    public int hashCode() {
        int hash = entityId.hashCode();

        if (dbPath != null) {
            hash += dbPath.hashCode() * 7;
        }

        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof EJBQLTableId)) {
            return false;
        }

        EJBQLTableId id = (EJBQLTableId) object;

        if (!Util.nullSafeEquals(entityId, id.entityId)) {
            return false;
        }

        if (!Util.nullSafeEquals(dbPath, id.dbPath)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return dbPath != null ? entityId + "/" + dbPath : entityId;
    }
}
