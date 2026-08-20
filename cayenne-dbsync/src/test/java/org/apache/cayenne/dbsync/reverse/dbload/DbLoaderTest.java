/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DbLoaderTest {

    private static final String CATALOG = "test_catalog";
    private static final String SCHEMA = "test_schema";
    private static final String TABLE = "ARTIST";
    private static final String PROCEDURE = "TEST_PROCEDURE";

    @Test
    public void loadWithoutCatalogs() throws SQLException {
        DataMap map = load(false);

        assertEquals(1, map.getDbEntities().size());
        assertEquals(1, map.getProcedures().size());

        DbEntity entity = map.getDbEntity(TABLE);
        assertNotNull(entity);
        assertNull(entity.getCatalog());
        assertEquals(SCHEMA, entity.getSchema());

        Procedure procedure = map.getProcedure(PROCEDURE);
        assertNotNull(procedure);
        assertNull(procedure.getCatalog());
        assertEquals(SCHEMA, procedure.getSchema());
    }

    @Test
    public void loadWithCatalogs() throws SQLException {
        DataMap map = load(true);

        assertEquals(1, map.getDbEntities().size());
        assertEquals(1, map.getProcedures().size());

        DbEntity entity = map.getDbEntity(TABLE);
        assertNotNull(entity);
        assertEquals(CATALOG, entity.getCatalog());
        assertEquals(SCHEMA, entity.getSchema());

        Procedure procedure = map.getProcedure(PROCEDURE);
        assertNotNull(procedure);
        assertEquals(CATALOG, procedure.getCatalog());
        assertEquals(SCHEMA, procedure.getSchema());
    }

    private DataMap load(boolean supportsCatalogs) throws SQLException {
        DbAdapter adapter = mock(DbAdapter.class);

        when(adapter.tableTypeForTable()).thenReturn("TABLE");
        when(adapter.tableTypeForView()).thenReturn("VIEW");
        when(adapter.supportsCatalogsOnReverseEngineering()).thenReturn(supportsCatalogs);

        ResultSet tables = tablesResultSet();
        ResultSet procedures = proceduresResultSet();
        ResultSet columns = emptyResultSet();
        ResultSet primaryKeys = emptyResultSet();
        ResultSet exportedKeys = emptyResultSet();
        ResultSet procedureColumns = emptyResultSet();

        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(tables);
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(columns);
        when(metaData.getPrimaryKeys(any(), any(), any())).thenReturn(primaryKeys);
        when(metaData.getExportedKeys(any(), any(), any())).thenReturn(exportedKeys);
        when(metaData.getProcedures(any(), any(), any())).thenReturn(procedures);
        when(metaData.getProcedureColumns(any(), any(), any(), any())).thenReturn(procedureColumns);

        Connection connection = mock(Connection.class);
        when(connection.getMetaData()).thenReturn(metaData);

        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(FiltersConfig.create(null, null, TableFilter.everything(),
                new PatternFilter().include(".*")));

        return new DbLoader(adapter, connection, config, null, new DefaultObjectNameGenerator()).load();
    }

    private static ResultSet tablesResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("TABLE_NAME")).thenReturn(TABLE);
        when(rs.getString("TABLE_CAT")).thenReturn(CATALOG);
        when(rs.getString("TABLE_SCHEM")).thenReturn(SCHEMA);
        when(rs.getString("TABLE_TYPE")).thenReturn("TABLE");
        return rs;
    }

    private static ResultSet proceduresResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("PROCEDURE_NAME")).thenReturn(PROCEDURE);
        when(rs.getString("PROCEDURE_CAT")).thenReturn(CATALOG);
        when(rs.getString("PROCEDURE_SCHEM")).thenReturn(SCHEMA);
        when(rs.getShort("PROCEDURE_TYPE")).thenReturn((short) DatabaseMetaData.procedureNoResult);
        return rs;
    }

    private static ResultSet emptyResultSet() {
        return mock(ResultSet.class);
    }
}
