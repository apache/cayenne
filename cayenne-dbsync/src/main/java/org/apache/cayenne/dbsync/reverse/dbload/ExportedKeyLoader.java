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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

class ExportedKeyLoader extends PerEntityLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportedKeyLoader.class);

    ExportedKeyLoader(DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(null, config, delegate);
    }

    @Override
    boolean shouldLoad(DbEntity entity) {
        return delegate.dbRelationship(entity);
    }

    @Override
    boolean catchException(DbEntity entity, SQLException ex) {
        LOGGER.info("Error getting relationships for '"
                + entity.getCatalog() + "." + entity.getSchema()
                + "', ignoring. " + ex.getMessage(), ex);
        return true;
    }

    @Override
    ResultSet getResultSet(DbEntity dbEntity, DatabaseMetaData metaData) throws SQLException {
        return metaData.getExportedKeys(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName());
    }

    @Override
    void processResultSet(DbEntity dbEntity, DbLoadDataStore map, ResultSet rs) throws SQLException {
        ExportedKey key = new ExportedKey(rs);

        DbEntity pkEntity = map.getDbEntity(key.getPk().getTable());
        if (pkEntity == null) {
            LOGGER.info("Skip relation: '" + key + "' because table '" + key.getPk().getTable() + "' is not found");
            return;
        }

        DbEntity fkEntity = map.getDbEntity(key.getFk().getTable());
        if (fkEntity == null) {
            LOGGER.info("Skip relation: '" + key + "' because table '" + key.getFk().getTable() + "' is not found");
            return;
        }

        map.addExportedKey(key);
    }
}
