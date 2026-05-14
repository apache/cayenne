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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.soft_delete.SoftDelete;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SoftDeleteBatchTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.SOFT_DELETE_PROJECT);

    private DataNode node;
    private UnitDbAdapter unitAdapter;
    private AdhocObjectFactory objectFactory;

    private DeleteBatchTranslator createTranslator(DeleteBatchQuery query) {
        DbAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        return createTranslator(query, adapter);
    }

    private DeleteBatchTranslator createTranslator(DeleteBatchQuery query, DbAdapter adapter) {
        return (DeleteBatchTranslator) new SoftDeleteTranslatorFactory().translator(query, adapter, null);
    }

    @BeforeEach
    public void setUp() {
        node = env.dataNode();
        unitAdapter = env.unitDbAdapter();
        objectFactory = env.adhocObjectFactory();
    }

    @Test
    public void createSqlString() {
        DbEntity entity = env.context().getEntityResolver().getObjEntity(SoftDelete.class).getDbEntity();

        List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("ID"));

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, Collections.emptySet(), 1);
        DeleteBatchTranslator builder = createTranslator(deleteQuery);
        String generatedSql = builder.getSql();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DELETED = ? WHERE ID = ?", generatedSql);
    }

    @Test
    public void createSqlStringWithNulls() {
        DbEntity entity = env.context().getEntityResolver().getObjEntity(SoftDelete.class).getDbEntity();

        List<DbAttribute> idAttributes = Arrays.asList(entity.getAttribute("ID"), entity.getAttribute("NAME"));

        Collection<String> nullAttributes = Collections.singleton("NAME");

        DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, nullAttributes, 1);
        DeleteBatchTranslator builder = createTranslator(deleteQuery);
        String generatedSql = builder.getSql();
        assertNotNull(generatedSql);
        assertEquals("UPDATE " + entity.getName() + " SET DELETED = ? WHERE ( ID = ? ) AND ( NAME IS NULL )", generatedSql);
    }

    @Test
    public void createSqlStringWithIdentifiersQuote() {
        DbEntity entity = env.context().getEntityResolver().getObjEntity(SoftDelete.class).getDbEntity();
        try {

            entity.getDataMap().setQuotingSQLIdentifiers(true);

            List<DbAttribute> idAttributes = Collections.singletonList(entity.getAttribute("ID"));

            DeleteBatchQuery deleteQuery = new DeleteBatchQuery(entity, idAttributes, Collections.emptySet(), 1);
            DbAdapter adapter = node.getAdapter();
            DeleteBatchTranslator builder = createTranslator(deleteQuery, adapter);
            String generatedSql = builder.getSql();

            String charStart = unitAdapter.getIdentifiersStartQuote();
            String charEnd = unitAdapter.getIdentifiersEndQuote();

            assertNotNull(generatedSql);
            assertEquals("UPDATE " + charStart + entity.getName() + charEnd + " SET " + charStart + "DELETED" + charEnd
                    + " = ? WHERE " + charStart + "ID" + charEnd + " = ?", generatedSql);
        } finally {
            entity.getDataMap().setQuotingSQLIdentifiers(false);
        }

    }

    @Test
    public void update() throws Exception {

        DbEntity entity = env.context().getEntityResolver().getObjEntity(SoftDelete.class).getDbEntity();

        BatchTranslatorFactory oldFactory = node.getBatchTranslatorFactory();
        try {
            node.setBatchTranslatorFactory(new SoftDeleteTranslatorFactory());

            final SoftDelete test = env.context().newObject(SoftDelete.class);
            test.setName("SoftDeleteBatchQueryBuilderTest");
            env.context().commitChanges();

            new ParallelTestContainer() {

                @Override
                protected void assertResult() {
                    Expression exp = ExpressionFactory.matchExp("name", test.getName());
                    assertEquals(1, ObjectSelect.query(SoftDelete.class, exp).selectCount(env.context()));

                    exp = ExpressionFactory.matchDbExp("DELETED", true);
                    assertEquals(0, ObjectSelect.query(SoftDelete.class, exp).selectCount(env.context()));
                }
            }.runTest(200);

            env.context().deleteObjects(test);
            assertEquals(test.getPersistenceState(), PersistenceState.DELETED);
            env.context().commitChanges();

            new ParallelTestContainer() {

                @Override
                protected void assertResult() {
                    Expression exp = ExpressionFactory.matchExp("name", test.getName());
                    assertEquals(0, ObjectSelect.query(SoftDelete.class, exp).selectCount(env.context()));

                    SQLTemplate template = new SQLTemplate(entity, "SELECT * FROM SOFT_DELETE");
                    template.setFetchingDataRows(true);
                    assertEquals(1, env.context().performQuery(template).size());
                }
            }.runTest(200);
        } finally {
            env.context().performQuery(new SQLTemplate(entity, "DELETE FROM SOFT_DELETE"));
            node.setBatchTranslatorFactory(oldFactory);
        }
    }

}
