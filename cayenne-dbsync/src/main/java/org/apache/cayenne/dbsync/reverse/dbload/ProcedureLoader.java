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
import org.apache.cayenne.map.Procedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcedureLoader extends PerCatalogAndSchemaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbLoader.class);

    ProcedureLoader(DbAdapter adapter, DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(adapter, config, delegate);
    }

    @Override
    protected ResultSet getResultSet(String catalogName, String schemaName, DatabaseMetaData metaData) throws SQLException {
        return metaData.getProcedures(catalogName, schemaName, WILDCARD);
    }

    @Override
    protected boolean shouldLoad(CatalogFilter catalog, SchemaFilter schema) {
        PatternFilter filter = config.getFiltersConfig().proceduresFilter(catalog.name, schema.name);
        return !filter.isEmpty();
    }

    @Override
    protected void processResultSetRow(CatalogFilter catalog, SchemaFilter schema, DbLoadDataStore map, ResultSet rs) throws SQLException {
        PatternFilter filter = config.getFiltersConfig().proceduresFilter(catalog.name, schema.name);
        String name = rs.getString("PROCEDURE_NAME");
        if (!filter.isIncluded(name)) {
            LOGGER.info("skipping Cayenne PK procedure: " + name);
            return;
        }

        Procedure procedure = new Procedure(name);
        procedure.setCatalog(rs.getString("PROCEDURE_CAT"));
        procedure.setSchema(rs.getString("PROCEDURE_SCHEM"));

        switch (rs.getShort("PROCEDURE_TYPE")) {
            case DatabaseMetaData.procedureNoResult:
            case DatabaseMetaData.procedureResultUnknown:
                procedure.setReturningValue(false);
                break;
            case DatabaseMetaData.procedureReturnsResult:
                procedure.setReturningValue(true);
                break;
        }
        map.addProcedureSafe(procedure);
    }
}
