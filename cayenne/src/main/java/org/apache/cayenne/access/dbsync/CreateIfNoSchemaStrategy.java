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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.map.compiled.CompiledSchema;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 3.0
 */
public class CreateIfNoSchemaStrategy implements SchemaUpdateStrategy {

    private final static Logger LOGGER = LoggerFactory.getLogger(CreateIfNoSchemaStrategy.class);

    protected final SQLLogger sqlLogger;

    public CreateIfNoSchemaStrategy(@Inject SQLLogger sqlLogger) {
        this.sqlLogger = sqlLogger;
    }

    @Override
    public void updateSchema(DbAdapter adapter, DataSource dataSource, CompiledSchema schema, String... dataMaps)
            throws SQLException {

        Set<String> tablesInDB = tablesInDB(adapter, dataSource);
        boolean generate = true;
        for (CompiledTable table : ThrowOnPartialSchemaStrategy.tablesOf(schema, dataMaps)) {
            if (isInDB(tablesInDB, table)) {
                generate = false;
                break;
            }
        }

        if (generate) {
            LOGGER.info("No schema detected, will create mapped tables");
            generate(adapter, dataSource, schema, dataMaps);
        } else {
            LOGGER.info("Full or partial schema detected, skipping tables creation");
        }
    }

    private void generate(DbAdapter adapter, DataSource dataSource, CompiledSchema schema, String... dataMaps) {
        for (String map : dataMaps) {
            DbGenerator gen = new DbGenerator(adapter, schema, map, List.of(), List.of(dataMaps), sqlLogger);
            gen.setShouldCreateTables(true);
            gen.setShouldDropTables(false);
            gen.setShouldCreateFKConstraints(true);
            gen.setShouldCreatePKSupport(true);
            gen.setShouldDropPKSupport(false);
            try {
                gen.runGenerator(dataSource);
            } catch (Exception e) {
                throw new CayenneRuntimeException(e);
            }
        }
    }

    // same table matching rule as SchemaAnalyzer: unquoted identifiers may be folded by the DB to upper or lower case
    // (e.g. PostgreSQL folds to lower case), quoted identifiers are stored as is
    private static boolean isInDB(Set<String> tablesInDB, CompiledTable table) {
        String name = table.name();
        if (tablesInDB.contains(name)) {
            return true;
        }

        return !table.map().quotingSQLIdentifiers()
                && (tablesInDB.contains(name.toLowerCase()) || tablesInDB.contains(name.toUpperCase()));
    }

    /**
     * Returns all the table names in database.
     */
    protected Set<String> tablesInDB(DbAdapter adapter, DataSource dataSource) throws SQLException {
        String tableLabel = adapter.tableTypeForTable();
        Set<String> tables = new HashSet<>();

        try (Connection con = dataSource.getConnection()) {
            try (ResultSet rs = con.getMetaData().getTables(null, null, "%", new String[]{tableLabel})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }

        return tables;
    }
}
