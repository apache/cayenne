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

package org.apache.cayenne.access.translator.batch.legacy;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.UpdateBatchQuery;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

@Deprecated
@UseServerRuntime(CayenneProjects.LOCKING_PROJECT)
public class UpdateBatchTranslatorIT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DbAdapter adapter;

    @Inject
    private UnitDbAdapter unitAdapter;

    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testConstructor() throws Exception {
        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        UpdateBatchTranslator builder = new UpdateBatchTranslator(mock(UpdateBatchQuery.class), adapter, null);
        assertSame(adapter, builder.adapter);
    }

    @Test
    public void testCreateSqlString() throws Exception {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();

        List idAttributes = Collections.singletonList(entity.getAttribute("LOCKING_TEST_ID"));
        List updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes,
                Collections.<String> emptySet(), 1);

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        UpdateBatchTranslator builder = new UpdateBatchTranslator(updateQuery, adapter, null);
        String generatedSql = builder.getSql();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DESCRIPTION = ? WHERE LOCKING_TEST_ID = ?", generatedSql);
    }

    @Test
    public void testCreateSqlStringWithNulls() throws Exception {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();

        List idAttributes = Arrays.asList(entity.getAttribute("LOCKING_TEST_ID"), entity.getAttribute("NAME"));

        List updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

        Collection nullAttributes = Collections.singleton("NAME");

        UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes, nullAttributes, 1);

        DbAdapter adapter = objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName());
        UpdateBatchTranslator builder = new UpdateBatchTranslator(updateQuery, adapter, null);
        String generatedSql = builder.getSql();
        assertNotNull(generatedSql);

        assertEquals("UPDATE " + entity.getName() + " SET DESCRIPTION = ? WHERE LOCKING_TEST_ID = ? AND NAME IS NULL",
                generatedSql);
    }

    @Test
    public void testCreateSqlStringWithIdentifiersQuote() throws Exception {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List idAttributes = Collections.singletonList(entity.getAttribute("LOCKING_TEST_ID"));
            List updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

            UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes,
                    Collections.<String> emptySet(), 1);
            JdbcAdapter adapter = (JdbcAdapter) this.adapter;

            UpdateBatchTranslator builder = new UpdateBatchTranslator(updateQuery, adapter, null);
            String generatedSql = builder.getSql();

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
    public void testCreateSqlStringWithNullsWithIdentifiersQuote() throws Exception {
        DbEntity entity = runtime.getDataDomain().getEntityResolver().getObjEntity(SimpleLockingTestEntity.class)
                .getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List idAttributes = Arrays.asList(entity.getAttribute("LOCKING_TEST_ID"), entity.getAttribute("NAME"));

            List updatedAttributes = Collections.singletonList(entity.getAttribute("DESCRIPTION"));

            Collection nullAttributes = Collections.singleton("NAME");

            UpdateBatchQuery updateQuery = new UpdateBatchQuery(entity, idAttributes, updatedAttributes,
                    nullAttributes, 1);
            JdbcAdapter adapter = (JdbcAdapter) this.adapter;

            UpdateBatchTranslator builder = new UpdateBatchTranslator(updateQuery, adapter, null);
            String generatedSql = builder.getSql();
            assertNotNull(generatedSql);

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();
            assertEquals("UPDATE " + charStart + entity.getName() + charEnd + " SET " + charStart + "DESCRIPTION"
                    + charEnd + " = ? WHERE " + charStart + "LOCKING_TEST_ID" + charEnd + " = ? AND " + charStart
                    + "NAME" + charEnd + " IS NULL", generatedSql);

        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }

}
