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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.locking.SoftTest;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.LOCKING_PROJECT)
public class SoftDeleteBatchQueryBuilderTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    protected DbAdapter adapter;
    
    @Inject
    private DataNode dataNode;

    @Inject
    private UnitDbAdapter unitAdapter;

    @Inject
    private AdhocObjectFactory objectFactory;

    private DeleteBatchQueryBuilder createBuilder(DeleteBatchQuery query) {
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        return createBuilder(query, adapter);
    }

    private DeleteBatchQueryBuilder createBuilder(DeleteBatchQuery query, JdbcAdapter adapter) {
        return (DeleteBatchQueryBuilder) new SoftDeleteQueryBuilderFactory().createDeleteQueryBuilder(query, adapter);
    }

    public void testCreateSqlString() throws Exception {
        DbEntity entity = context.getEntityResolver().getObjEntity(SoftTest.class).getDbEntity();

        List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("SOFT_TEST_ID"));

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, Collections.<String> emptySet(), 1);
        DeleteBatchQueryBuilder builder = createBuilder(deleteQuery);
        String generatedSql = builder.createSqlString();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DELETED = ? WHERE SOFT_TEST_ID = ?", generatedSql);
    }

    public void testCreateSqlStringWithNulls() throws Exception {
        DbEntity entity = context.getEntityResolver().getObjEntity(SoftTest.class).getDbEntity();

        List<DbAttribute> idAttributes = Arrays
                .asList(entity.getAttribute("SOFT_TEST_ID"), entity.getAttribute("NAME"));

        Collection<String> nullAttributes = Collections.singleton("NAME");

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, nullAttributes, 1);
        DeleteBatchQueryBuilder builder = createBuilder(deleteQuery);
        String generatedSql = builder.createSqlString();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DELETED = ? WHERE SOFT_TEST_ID = ? AND NAME IS NULL",
                generatedSql);
    }

    public void testCreateSqlStringWithIdentifiersQuote() throws Exception {
        DbEntity entity = context.getEntityResolver().getObjEntity(SoftTest.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);

            List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("SOFT_TEST_ID"));

            DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, Collections.<String> emptySet(), 1);
            JdbcAdapter adapter = (JdbcAdapter) this.adapter;
            DeleteBatchQueryBuilder builder = createBuilder(deleteQuery, adapter);
            String generatedSql = builder.createSqlString();

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("UPDATE " + charStart + entity.getName() + charEnd + " SET " + charStart + "DELETED" + charEnd
                    + " = ? WHERE " + charStart + "SOFT_TEST_ID" + charEnd + " = ?", generatedSql);
        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }

    }

    public void testUpdate() throws Exception {

        final DbEntity entity = context.getEntityResolver().getObjEntity(SoftTest.class).getDbEntity();

        JdbcAdapter adapter = (JdbcAdapter) this.adapter;
        BatchQueryBuilderFactory oldFactory = dataNode.getBatchQueryBuilderFactory();
        try {
            dataNode.setBatchQueryBuilderFactory(new SoftDeleteQueryBuilderFactory());

            final SoftTest test = context.newObject(SoftTest.class);
            test.setName("SoftDeleteBatchQueryBuilderTest");
            context.commitChanges();

            final SelectQuery query = new SelectQuery(SoftTest.class);

            new ParallelTestContainer() {

                @Override
                protected void assertResult() throws Exception {
                    query.setQualifier(ExpressionFactory.matchExp("name", test.getName()));
                    assertEquals(1, context.performQuery(query).size());

                    query.andQualifier(ExpressionFactory.matchDbExp("DELETED", true));
                    assertEquals(0, context.performQuery(query).size());
                }
            }.runTest(200);

            context.deleteObjects(test);
            assertEquals(test.getPersistenceState(), PersistenceState.DELETED);
            context.commitChanges();

            new ParallelTestContainer() {

                @Override
                protected void assertResult() throws Exception {
                    query.setQualifier(ExpressionFactory.matchExp("name", test.getName()));
                    assertEquals(0, context.performQuery(query).size());

                    SQLTemplate template = new SQLTemplate(entity, "SELECT * FROM SOFT_TEST");
                    template.setFetchingDataRows(true);
                    assertEquals(1, context.performQuery(template).size());
                }
            }.runTest(200);
        } finally {
            context.performQuery(new SQLTemplate(entity, "DELETE FROM SOFT_TEST"));
            dataNode.setBatchQueryBuilderFactory(oldFactory);
        }
    }

}
