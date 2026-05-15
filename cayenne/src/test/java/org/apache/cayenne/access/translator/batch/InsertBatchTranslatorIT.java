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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

public class InsertBatchTranslatorIT {

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

    @Test
    public void constructor() {
        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());

        InsertBatchQuery query = mock(InsertBatchQuery.class);
        InsertBatchTranslator builder = new InsertBatchTranslator(query, adapter);

        assertSame(adapter, builder.context.getAdapter());
        assertSame(query, builder.context.getQuery());
    }

    @Test
    public void createSqlString() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver()
                .getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        InsertBatchQuery insertQuery = new InsertBatchQuery(entity, 1);
        InsertBatchTranslator builder = new InsertBatchTranslator(insertQuery, adapter);
        String generatedSql = builder.getSql();
        assertNotNull(generatedSql);
        assertEquals("INSERT INTO " + entity.getName() + "( DESCRIPTION, INT_COLUMN_NOTNULL, INT_COLUMN_NULL, LOCKING_TEST_ID, NAME) " +
                        "VALUES( ?, ?, ?, ?, ?)",
                generatedSql);
    }

    @Test
    public void createSqlStringWithIdentifiersQuote() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);

            InsertBatchQuery insertQuery = new InsertBatchQuery(entity, 1);
            InsertBatchTranslator builder = new InsertBatchTranslator(insertQuery, adapter);
            String generatedSql = builder.getSql();
            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();
            assertNotNull(generatedSql);
            assertEquals("INSERT INTO " + charStart + entity.getName() + charEnd
                    + "( " + charStart + "DESCRIPTION" + charEnd + ", "
                    + charStart + "INT_COLUMN_NOTNULL" + charEnd + ", "
                    + charStart + "INT_COLUMN_NULL" + charEnd + ", "
                    + charStart + "LOCKING_TEST_ID" + charEnd + ", "
                    + charStart + "NAME" + charEnd + ") VALUES( ?, ?, ?, ?, ?)", generatedSql);
        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }
}
