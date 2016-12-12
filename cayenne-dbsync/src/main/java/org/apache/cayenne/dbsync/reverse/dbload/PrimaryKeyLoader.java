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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class PrimaryKeyLoader extends PerEntityLoader {

    private static final Log LOGGER = LogFactory.getLog(DbLoader.class);

    PrimaryKeyLoader(DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(null, config, delegate);
    }

    @Override
    ResultSet getResultSet(DbEntity dbEntity, DatabaseMetaData metaData) throws SQLException {
        return metaData.getPrimaryKeys(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName());
    }

    @Override
    void processResultSet(DbEntity dbEntity, DbLoadDataStore map, ResultSet rs) throws SQLException {
        String columnName = rs.getString("COLUMN_NAME");
        DbAttribute attribute = dbEntity.getAttribute(columnName);
        if (attribute == null) {
            // why an attribute might be null is not quiet clear
            // but there is a bug report 731406 indicating that it is
            // possible so just print the warning, and ignore
            LOGGER.warn("Can't locate attribute for primary key: " + columnName);
            return;
        }

        attribute.setPrimaryKey(true);
        ((DetectedDbEntity) dbEntity).setPrimaryKeyName(rs.getString("PK_NAME"));
    }
}
