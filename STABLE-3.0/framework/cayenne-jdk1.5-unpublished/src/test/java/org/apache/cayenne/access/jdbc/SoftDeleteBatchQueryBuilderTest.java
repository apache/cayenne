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
package org.apache.cayenne.access.jdbc;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.locking.SoftTest;
import org.apache.cayenne.unit.LockingCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

public class SoftDeleteBatchQueryBuilderTest extends LockingCase {
    private DeleteBatchQueryBuilder createBuilder() {
        return createBuilder(new JdbcAdapter());
    }
    
    private DeleteBatchQueryBuilder createBuilder(JdbcAdapter adapter) {
        return (DeleteBatchQueryBuilder) new SoftDeleteQueryBuilderFactory().
            createDeleteQueryBuilder(adapter);
    }

    public void testCreateSqlString() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SoftTest.class).getDbEntity();

        List idAttributes = Collections.singletonList(entity
                .getAttribute("SOFT_TEST_ID"));

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, null, 1);
        DeleteBatchQueryBuilder builder = createBuilder();
        String generatedSql = builder.createSqlString(deleteQuery);
        assertNotNull(generatedSql);
        assertEquals(
                "UPDATE " + entity.getName() + " SET DELETED = ? WHERE SOFT_TEST_ID = ?",
                generatedSql);
    }

    public void testCreateSqlStringWithNulls() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SoftTest.class).getDbEntity();

        List idAttributes = Arrays.asList(entity.getAttribute("SOFT_TEST_ID"), entity
                .getAttribute("NAME"));

        Collection nullAttributes = Collections.singleton("NAME");

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(
                entity,
                idAttributes,
                nullAttributes,
                1);
        DeleteBatchQueryBuilder builder = createBuilder();
        String generatedSql = builder.createSqlString(deleteQuery);
        assertNotNull(generatedSql);
        assertEquals("UPDATE "
                + entity.getName()
                + " SET DELETED = ? WHERE SOFT_TEST_ID = ? AND NAME IS NULL", generatedSql);
    }

    public void testCreateSqlStringWithIdentifiersQuote() throws Exception {
        DbEntity entity = getDomain().getEntityResolver().lookupObjEntity(
                SoftTest.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);
            List idAttributes = Collections.singletonList(entity
                    .getAttribute("SOFT_TEST_ID"));

            DeleteBatchQuery deleteQuery = new DeleteBatchQuery(
                    entity,
                    idAttributes,
                    null,
                    1);
            JdbcAdapter adapter = (JdbcAdapter) getAccessStackAdapter().getAdapter();
            DeleteBatchQueryBuilder builder = createBuilder(adapter);
            String generatedSql = builder.createSqlString(deleteQuery);
            
            String charStart = adapter.getIdentifiersStartQuote();
            String charEnd = adapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("UPDATE "
                    + charStart
                    + entity.getName()
                    + charEnd
                    + " SET "
                    + charStart
                    + "DELETED"
                    + charEnd
                    + " = ? WHERE "
                    + charStart
                    + "SOFT_TEST_ID"
                    + charEnd
                    + " = ?", generatedSql);
        }
        finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }

    }
    
    public void testUpdate() throws Exception {
        final DataContext context = createDataContext();
        final DbEntity entity = context.getEntityResolver().lookupObjEntity(
                SoftTest.class).getDbEntity();
        
        try {
            context.getParentDataDomain().setQueryBuilderFactory(new SoftDeleteQueryBuilderFactory());
            
            final SoftTest test = context.newObject(SoftTest.class);
            test.setName("SoftDeleteBatchQueryBuilderTest");
            context.commitChanges();
            
            final SelectQuery query = new SelectQuery(SoftTest.class);
            
            new ThreadedTestHelper() {
                @Override
                protected void assertResult() throws Exception {
                    query.setQualifier(ExpressionFactory.matchExp("name", test.getName()));
                    assertEquals(1, context.performQuery(query).size());
                    
                    query.andQualifier(ExpressionFactory.matchDbExp("DELETED", true));
                    assertEquals(0, context.performQuery(query).size());
                }
            }.assertWithTimeout(200);
            
            context.deleteObject(test);
            assertEquals(test.getPersistenceState(), PersistenceState.DELETED);
            context.commitChanges();
            
            new ThreadedTestHelper() {
                @Override
                protected void assertResult() throws Exception {
                    query.setQualifier(ExpressionFactory.matchExp("name", test.getName()));
                    assertEquals(0, context.performQuery(query).size());
                    
                    SQLTemplate template = new SQLTemplate(entity, "SELECT * FROM SOFT_TEST");
                    template.setFetchingDataRows(true);
                    assertEquals(1, context.performQuery(template).size());
                }
            }.assertWithTimeout(200);
        }
        finally {
            context.performQuery(new SQLTemplate(entity, "DELETE FROM SOFT_TEST"));
            context.getParentDataDomain().setQueryBuilderFactory(null);
        }
    }

}
