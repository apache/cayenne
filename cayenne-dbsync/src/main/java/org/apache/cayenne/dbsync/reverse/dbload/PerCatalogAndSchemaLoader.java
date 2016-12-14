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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.SchemaFilter;

public abstract class PerCatalogAndSchemaLoader extends AbstractLoader {

    PerCatalogAndSchemaLoader(DbAdapter adapter, DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(adapter, config, delegate);
    }

    public void load(DatabaseMetaData metaData, DbLoadDataStore map) throws SQLException {
        for (CatalogFilter catalog : config.getFiltersConfig().getCatalogs()) {
            for (SchemaFilter schema : catalog.schemas) {
                if(!shouldLoad(catalog, schema)) {
                    continue;
                }
                try (ResultSet rs = getResultSet(catalog.name, schema.name, metaData)) {
                    while (rs.next()) {
                        processResultSetRow(catalog, schema, map, rs);
                    }
                }
            }
        }
    }

    boolean shouldLoad(CatalogFilter catalog, SchemaFilter schema) {
        return true;
    }

    abstract ResultSet getResultSet(String catalogName, String schemaName, DatabaseMetaData metaData) throws SQLException;

    abstract void processResultSetRow(CatalogFilter catalog, SchemaFilter schema, DbLoadDataStore map, ResultSet rs) throws SQLException;
}
