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

import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.testdo.locking.SimpleLockingTestEntity;
import org.apache.cayenne.unit.LockingCase;

public class InsertBatchQueryBuilderTest extends LockingCase {

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

        InsertBatchQuery deleteQuery = new InsertBatchQuery(entity, 1);
        InsertBatchQueryBuilder builder = new InsertBatchQueryBuilder(new JdbcAdapter());
        String generatedSql = builder.createSqlString(deleteQuery);
        assertNotNull(generatedSql);
        assertEquals("INSERT INTO "
                + entity.getName()
                + " (DESCRIPTION, LOCKING_TEST_ID, NAME) VALUES (?, ?, ?)", generatedSql);
    }

    public void testCreateSqlStringWithIdentifiersQuote() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SimpleLockingTestEntity.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List idAttributes = Collections.singletonList(entity
                    .getAttribute("LOCKING_TEST_ID"));
           
            JdbcAdapter adapter = (JdbcAdapter) getAccessStackAdapter().getAdapter();
            
            InsertBatchQuery deleteQuery = new InsertBatchQuery(entity, 1);
            InsertBatchQueryBuilder builder = new InsertBatchQueryBuilder(adapter);
            String generatedSql = builder.createSqlString(deleteQuery);
            String charStart = adapter.getIdentifiersStartQuote();
            String charEnd = adapter.getIdentifiersEndQuote();
            assertNotNull(generatedSql);
            assertEquals("INSERT INTO "
                    + charStart
                    + entity.getName()
                    + charEnd
                    + " ("
                    + charStart
                    + "DESCRIPTION"
                    + charEnd
                    + ", "
                    + charStart
                    + "LOCKING_TEST_ID"
                    + charEnd
                    + ", "
                    + charStart
                    + "NAME"
                    + charEnd
                    + ") VALUES (?, ?, ?)", generatedSql);
        }
        finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }
    }
}
