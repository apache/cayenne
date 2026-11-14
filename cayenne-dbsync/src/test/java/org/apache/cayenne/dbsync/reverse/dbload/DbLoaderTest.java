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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DefaultRuntimeProperties;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.unit.TestAdapters;
import org.apache.cayenne.unit.jdbc.TestConnection;
import org.apache.cayenne.unit.jdbc.TestResultSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        DbAdapter adapter = supportsCatalogs ? TestAdapters.jdbc() : new JdbcAdapter(
                new DefaultRuntimeProperties(Map.of()),
                List.of(),
                List.of(),
                List.of(),
                new DefaultValueObjectTypeRegistry(List.of())) {

            @Override
            public boolean supportsCatalogsOnReverseEngineering() {
                return false;
            }
        };

        // all other metadata results are empty
        Connection connection = new TestConnection()
                .metaDataResult("getTables", tablesResultSet())
                .metaDataResult("getProcedures", proceduresResultSet());

        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(FiltersConfig.create(null, null, TableFilter.everything(),
                new PatternFilter().include(".*")));

        return new DbLoader(adapter, connection, config, null, new DefaultObjectNameGenerator()).load();
    }

    private static ResultSet tablesResultSet() {
        TestResultSet rs = new TestResultSet("tables");
        rs.addColumn("TABLE_NAME", new Object[]{TABLE});
        rs.addColumn("TABLE_CAT", new Object[]{CATALOG});
        rs.addColumn("TABLE_SCHEM", new Object[]{SCHEMA});
        rs.addColumn("TABLE_TYPE", new Object[]{"TABLE"});
        return rs;
    }

    private static ResultSet proceduresResultSet() {
        TestResultSet rs = new TestResultSet("procedures");
        rs.addColumn("PROCEDURE_NAME", new Object[]{PROCEDURE});
        rs.addColumn("PROCEDURE_CAT", new Object[]{CATALOG});
        rs.addColumn("PROCEDURE_SCHEM", new Object[]{SCHEMA});
        rs.addColumn("PROCEDURE_TYPE", new Object[]{(short) DatabaseMetaData.procedureNoResult});
        return rs;
    }
}
