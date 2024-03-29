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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.reverse.filters.CatalogFilter;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.SchemaFilter;
import org.apache.cayenne.map.DbEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AttributeLoader extends PerCatalogAndSchemaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbLoader.class);

    private final AttributeProcessor attributeProcessor;

    AttributeLoader(DbAdapter adapter, DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(adapter, config, delegate);
        attributeProcessor = new AttributeProcessor(adapter);
    }

    protected ResultSet getResultSet(String catalogName, String schemaName, DatabaseMetaData metaData) throws SQLException {
        return metaData.getColumns(catalogName, schemaName, WILDCARD, WILDCARD);
    }

    @Override
    boolean catchException(String catalogName, String schemaName, SQLException ex) {
        String message = "Unable to load columns for the "
                + catalogName + "/" + schemaName + ", falling back to per-entity loader.";
        LOGGER.warn(message, ex);
        return true;
    }

    @Override
    protected void processResultSetRow(CatalogFilter catalog, SchemaFilter schema, DbLoadDataStore map, ResultSet rs) throws SQLException {
        // for a reason not quiet apparent to me, Oracle sometimes
        // returns duplicate record sets for the same table, messing up
        // table names. E.g. for the system table "WK$_ATTR_MAPPING" columns
        // are returned twice - as "WK$_ATTR_MAPPING" and "WK$$_ATTR_MAPPING"...
        // Go figure
        String tableName = rs.getString("TABLE_NAME");
        DbEntity entity = map.getDbEntity(tableName);
        if(entity == null) {
            return;
        }

        PatternFilter columnFilter = schema.tables.getIncludeTableColumnFilter(tableName);

        attributeProcessor.processAttribute(rs, columnFilter, entity);
    }
}
