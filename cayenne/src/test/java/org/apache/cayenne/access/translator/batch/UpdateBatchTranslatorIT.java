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
package org.apache.cayenne.access.translator.batch;

import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.query.resolved.UpdateBatch;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UpdateBatchTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LOCKING_PROJECT);

    private CayenneRuntime runtime;
    private DbAdapter adapter;
    private TestDbAdapter unitAdapter;
    private AdhocObjectFactory objectFactory;

    @BeforeEach
    public void setUp() {
        runtime = env.runtime();
        adapter = env.dataNode().getAdapter();
        unitAdapter = env.testDbAdapter();
        objectFactory = env.adhocObjectFactory();
    }

    /**
     * Returns the compiled table of the test entity. The mapping is replaced by {@link #quoteIdentifiers(boolean)},
     * so the table must be looked up after it.
     */
    private CompiledTable lockingTable() {
        return runtime.getDataDomain().getMapping().entity(SimpleLockingTestEntity.class).table();
    }

    /**
     * Switches identifier quoting on the authored map and recompiles the mapping for the runtime to see the change.
     */
    private void quoteIdentifiers(boolean quote) {
        env.project().dataMap("locking").setQuotingSQLIdentifiers(quote);
        env.recompile();
    }

    @Test
    public void createSqlString() {
        CompiledTable table = lockingTable();

        List<CompiledColumn> idColumns = List.of(table.column("LOCKING_TEST_ID"));
        List<CompiledColumn> updatedColumns = List.of(table.column("DESCRIPTION"));

        UpdateBatch updateQuery = new UpdateBatch(table, idColumns, updatedColumns, Set.of(), false, List.of());

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + table.name() + " SET DESCRIPTION = ? WHERE LOCKING_TEST_ID = ?", generatedSql);
    }

    @Test
    public void createSqlStringWithNulls() {
        CompiledTable table = lockingTable();

        List<CompiledColumn> idColumns = List.of(table.column("LOCKING_TEST_ID"), table.column("NAME"));

        List<CompiledColumn> updatedColumns = List.of(table.column("DESCRIPTION"));

        Collection<String> nullAttributes = Set.of("NAME");

        UpdateBatch updateQuery = new UpdateBatch(table, idColumns, updatedColumns, nullAttributes, false,
                List.of());

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
        assertNotNull(generatedSql);

        assertEquals("UPDATE " + table.name() + " SET DESCRIPTION = ? WHERE LOCKING_TEST_ID = ? AND NAME IS NULL",
                generatedSql);
    }

    @Test
    public void createSqlStringWithIdentifiersQuote() {
        quoteIdentifiers(true);
        try {
            CompiledTable table = lockingTable();
            List<CompiledColumn> idColumns = List.of(table.column("LOCKING_TEST_ID"));
            List<CompiledColumn> updatedColumns = List.of(table.column("DESCRIPTION"));

            UpdateBatch updateQuery = new UpdateBatch(table, idColumns, updatedColumns, Set.of(), false,
                    List.of());

            String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("UPDATE " + charStart + table.name() + charEnd + " SET " + charStart + "DESCRIPTION"
                    + charEnd + " = ? WHERE " + charStart + "LOCKING_TEST_ID" + charEnd + " = ?", generatedSql);

        } finally {
            quoteIdentifiers(false);
        }
    }

    @Test
    public void createSqlStringWithNullsWithIdentifiersQuote() {
        quoteIdentifiers(true);
        try {
            CompiledTable table = lockingTable();
            List<CompiledColumn> idColumns = List.of(table.column("LOCKING_TEST_ID"), table.column("NAME"));

            List<CompiledColumn> updatedColumns = List.of(table.column("DESCRIPTION"));

            Collection<String> nullAttributes = Set.of("NAME");

            UpdateBatch updateQuery = new UpdateBatch(table, idColumns, updatedColumns, nullAttributes, false,
                    List.of());

            String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
            assertNotNull(generatedSql);

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();
            assertEquals("UPDATE " + charStart + table.name() + charEnd + " SET " + charStart + "DESCRIPTION"
                    + charEnd + " = ? WHERE " + charStart + "LOCKING_TEST_ID" + charEnd + " = ? AND " + charStart
                    + "NAME" + charEnd + " IS NULL", generatedSql);

        } finally {
            quoteIdentifiers(false);
        }
    }
}
