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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void createSqlString() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();

        List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("LOCKING_TEST_ID"));
        List<DbAttribute> updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes,
                Collections.emptySet(), 1);

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DESCRIPTION = ? WHERE LOCKING_TEST_ID = ?", generatedSql);
    }

    @Test
    public void createSqlStringWithNulls() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();

        List<DbAttribute> idAttributes = Arrays.asList(entity.getAttribute("LOCKING_TEST_ID"), entity.getAttribute("NAME"));

        List<DbAttribute> updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

        Collection<String> nullAttributes = Collections.singleton("NAME");

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes, nullAttributes, 1);

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
        assertNotNull(generatedSql);

        assertEquals("UPDATE " + entity.getName() + " SET DESCRIPTION = ? WHERE ( LOCKING_TEST_ID = ? ) AND ( NAME IS NULL )",
                generatedSql);
    }

    @Test
    public void createSqlStringWithIdentifiersQuote() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("LOCKING_TEST_ID"));
            List<DbAttribute> updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

            UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes, Collections.emptySet(), 1);

            String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("UPDATE " + charStart + entity.getName() + charEnd + " SET " + charStart + "DESCRIPTION"
                    + charEnd + " = ? WHERE " + charStart + "LOCKING_TEST_ID" + charEnd + " = ?", generatedSql);

        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }

    @Test
    public void createSqlStringWithNullsWithIdentifiersQuote() {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List<DbAttribute> idAttributes = Arrays.asList(entity.getAttribute("LOCKING_TEST_ID"), entity.getAttribute("NAME"));

            List<DbAttribute> updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

            Collection<String> nullAttributes = Collections.singleton("NAME");

            UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes, nullAttributes, 1);

            String generatedSql = new UpdateBatchTranslator().translate(updateQuery, adapter).sql();
            assertNotNull(generatedSql);

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();
            assertEquals("UPDATE " + charStart + entity.getName() + charEnd + " SET " + charStart + "DESCRIPTION"
                    + charEnd + " = ? WHERE ( " + charStart + "LOCKING_TEST_ID" + charEnd + " = ? ) AND ( " + charStart
                    + "NAME" + charEnd + " IS NULL )", generatedSql);

        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }

}
