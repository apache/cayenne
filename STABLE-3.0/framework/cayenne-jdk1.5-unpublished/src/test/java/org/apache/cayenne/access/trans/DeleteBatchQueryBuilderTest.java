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

package org.apache.cayenne.access.trans;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.LockingCase;

/**
 */
public class DeleteBatchQueryBuilderTest extends LockingCase {

    public void testConstructor() throws Exception {
        DbAdapter adapter = new JdbcAdapter();

        DeleteBatchQueryBuilder builder = new DeleteBatchQueryBuilder(adapter);

        assertSame(adapter, builder.getAdapter());
    }

    public void testCreateSqlString() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SimpleLockingTestEntity.class).getDbEntity();

        List idAttributes = Collections.singletonList(entity
                .getAttribute("LOCKING_TEST_ID"));

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, null, 1);
        DeleteBatchQueryBuilder builder = new DeleteBatchQueryBuilder(new JdbcAdapter());
        String generatedSql = builder.createSqlString(deleteQuery);
        assertNotNull(generatedSql);
        assertEquals(
                "DELETE FROM " + entity.getName() + " WHERE LOCKING_TEST_ID = ?",
                generatedSql);
    }

    public void testCreateSqlStringWithNulls() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SimpleLockingTestEntity.class).getDbEntity();

        List idAttributes = Arrays.asList(entity.getAttribute("LOCKING_TEST_ID"), entity
                .getAttribute("NAME"));

        Collection nullAttributes = Collections.singleton("NAME");

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(
                entity,
                idAttributes,
                nullAttributes,
                1);
        DeleteBatchQueryBuilder builder = new DeleteBatchQueryBuilder(new JdbcAdapter());
        String generatedSql = builder.createSqlString(deleteQuery);
        assertNotNull(generatedSql);
        assertEquals("DELETE FROM "
                + entity.getName()
                + " WHERE LOCKING_TEST_ID = ? AND NAME IS NULL", generatedSql);
    }

    public void testCreateSqlStringWithIdentifiersQuote() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SimpleLockingTestEntity.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List idAttributes = Collections.singletonList(entity
                    .getAttribute("LOCKING_TEST_ID"));

            DeleteBatchQuery deleteQuery = new DeleteBatchQuery(
                    entity,
                    idAttributes,
                    null,
                    1);
            JdbcAdapter adapter = (JdbcAdapter) getAccessStackAdapter().getAdapter();
            DeleteBatchQueryBuilder builder = new DeleteBatchQueryBuilder(adapter);
            String generatedSql = builder.createSqlString(deleteQuery);
            
            String charStart = adapter.getIdentifiersStartQuote();
            String charEnd = adapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("DELETE FROM "
                    + charStart
                    + entity.getName()
                    + charEnd
                    + " WHERE "
                    + charStart
                    + "LOCKING_TEST_ID"
                    + charEnd
                    + " = ?", generatedSql);
        }
        finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }

    }

    public void testCreateSqlStringWithNullsWithIdentifiersQuote() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SimpleLockingTestEntity.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);

            List idAttributes = Arrays.asList(
                    entity.getAttribute("LOCKING_TEST_ID"),
                    entity.getAttribute("NAME"));

            Collection nullAttributes = Collections.singleton("NAME");

            DeleteBatchQuery deleteQuery = new DeleteBatchQuery(
                    entity,
                    idAttributes,
                    nullAttributes,
                    1);
            
            JdbcAdapter adapter = (JdbcAdapter) getAccessStackAdapter().getAdapter();
            
            DeleteBatchQueryBuilder builder = new DeleteBatchQueryBuilder(adapter);
            String generatedSql = builder.createSqlString(deleteQuery);

            String charStart = adapter.getIdentifiersStartQuote();
            String charEnd = adapter.getIdentifiersEndQuote();
            assertNotNull(generatedSql);

            assertEquals("DELETE FROM "
                    + charStart
                    + entity.getName()
                    + charEnd
                    + " WHERE "
                    + charStart
                    + "LOCKING_TEST_ID"
                    + charEnd
                    + " = ? AND "
                    + charStart
                    + "NAME"
                    + charEnd
                    + " IS NULL", generatedSql);
        }
        finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }
}
