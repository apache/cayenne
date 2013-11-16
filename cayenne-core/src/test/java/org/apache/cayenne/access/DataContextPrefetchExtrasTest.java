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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.CharFkTestEntity;
import org.apache.cayenne.testdo.testmap.CharPkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundFkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Test prefetching of various obscure cases.
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextPrefetchExtrasTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tCharPkTest;
    protected TableHelper tCharFkTest;
    protected TableHelper tCompoundPkTest;
    protected TableHelper tCompoundFkTest;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CHAR_FK_TEST");
        dbHelper.deleteAll("CHAR_PK_TEST");

        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");

        tCharPkTest = new TableHelper(dbHelper, "CHAR_PK_TEST");
        tCharPkTest.setColumns("PK_COL", "OTHER_COL");

        tCharFkTest = new TableHelper(dbHelper, "CHAR_FK_TEST");
        tCharFkTest.setColumns("PK", "FK_COL", "NAME");

        tCompoundPkTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPkTest.setColumns("KEY1", "KEY2", "NAME");

        tCompoundFkTest = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFkTest.setColumns("PKEY", "F_KEY1", "F_KEY2", "NAME");
    }

    protected void createPrefetchToManyOnCharKeyDataSet() throws Exception {
        tCharPkTest.insert("k1", "n1");
        tCharPkTest.insert("k2", "n2");

        tCharFkTest.insert(1, "k1", "fn1");
        tCharFkTest.insert(2, "k1", "fn2");
        tCharFkTest.insert(3, "k2", "fn3");
        tCharFkTest.insert(4, "k2", "fn4");
        tCharFkTest.insert(5, "k1", "fn5");
    }

    protected void createCompoundDataSet() throws Exception {
        tCompoundPkTest.insert("101", "201", "CPK1");
        tCompoundPkTest.insert("102", "202", "CPK2");
        tCompoundPkTest.insert("103", "203", "CPK3");

        tCompoundFkTest.insert(301, "102", "202", "CFK1");
        tCompoundFkTest.insert(302, "102", "202", "CFK2");
        tCompoundFkTest.insert(303, "101", "201", "CFK3");
    }

    public void testPrefetchToManyOnCharKey() throws Exception {
        createPrefetchToManyOnCharKeyDataSet();

        SelectQuery q = new SelectQuery(CharPkTestEntity.class);
        q.addPrefetch("charFKs");
        q.addOrdering(CharPkTestEntity.OTHER_COL_PROPERTY, SortOrder.ASCENDING);

        List<?> pks = context.performQuery(q);
        assertEquals(2, pks.size());

        CharPkTestEntity pk1 = (CharPkTestEntity) pks.get(0);
        assertEquals("n1", pk1.getOtherCol());
        List<?> toMany = (List<?>) pk1.readPropertyDirectly("charFKs");
        assertNotNull(toMany);
        assertFalse(((ValueHolder) toMany).isFault());
        assertEquals(3, toMany.size());

        CharFkTestEntity fk1 = (CharFkTestEntity) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());
        assertSame(pk1, fk1.getToCharPK());
    }

    /**
     * Tests to-one prefetching over relationships with compound keys.
     */
    public void testPrefetch10() throws Exception {
        createCompoundDataSet();

        Expression e = ExpressionFactory.matchExp("name", "CFK2");
        SelectQuery q = new SelectQuery(CompoundFkTestEntity.class, e);
        q.addPrefetch("toCompoundPk");

        List<?> objects = context.performQuery(q);
        assertEquals(1, objects.size());
        CayenneDataObject fk1 = (CayenneDataObject) objects.get(0);

        Object toOnePrefetch = fk1.readNestedProperty("toCompoundPk");
        assertNotNull(toOnePrefetch);
        assertTrue(
                "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                toOnePrefetch instanceof DataObject);

        DataObject pk1 = (DataObject) toOnePrefetch;
        assertEquals(PersistenceState.COMMITTED, pk1.getPersistenceState());
        assertEquals("CPK2", pk1.readPropertyDirectly("name"));
    }

    /**
     * Tests to-many prefetching over relationships with compound keys.
     */
    public void testPrefetch11() throws Exception {
        createCompoundDataSet();

        Expression e = ExpressionFactory.matchExp("name", "CPK2");
        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class, e);
        q.addPrefetch("compoundFkArray");

        List<?> pks = context.performQuery(q);
        assertEquals(1, pks.size());
        CayenneDataObject pk1 = (CayenneDataObject) pks.get(0);

        List<?> toMany = (List<?>) pk1.readPropertyDirectly("compoundFkArray");
        assertNotNull(toMany);
        assertFalse(((ValueHolder) toMany).isFault());
        assertEquals(2, toMany.size());

        CayenneDataObject fk1 = (CayenneDataObject) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());

        CayenneDataObject fk2 = (CayenneDataObject) toMany.get(1);
        assertEquals(PersistenceState.COMMITTED, fk2.getPersistenceState());
    }
}
