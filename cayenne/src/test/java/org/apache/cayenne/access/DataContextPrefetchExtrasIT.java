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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CharFkTestEntity;
import org.apache.cayenne.testdo.compound.CharPkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test prefetching of various obscure cases.
 */
@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class DataContextPrefetchExtrasIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tCharPkTest;
    protected TableHelper tCharFkTest;
    protected TableHelper tCompoundPkTest;
    protected TableHelper tCompoundFkTest;

    @Before
    public void setUp() throws Exception {
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

    @Test
    public void testPrefetchToManyOnCharKey() throws Exception {
        createPrefetchToManyOnCharKeyDataSet();

        ObjectSelect<CharPkTestEntity> q = ObjectSelect.query(CharPkTestEntity.class)
                .prefetch("charFKs", PrefetchTreeNode.UNDEFINED_SEMANTICS)
                .orderBy(CharPkTestEntity.OTHER_COL.asc());

        List<CharPkTestEntity> pks = q.select(context);
        assertEquals(2, pks.size());

        CharPkTestEntity pk1 = pks.get(0);
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
    @Test
    public void testPrefetch10() throws Exception {
        createCompoundDataSet();

        ObjectSelect<CompoundFkTestEntity> q = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.NAME.eq("CFK2"))
                .prefetch("toCompoundPk", PrefetchTreeNode.UNDEFINED_SEMANTICS);

        List<CompoundFkTestEntity> objects = q.select(context);
        assertEquals(1, objects.size());
        PersistentObject fk1 = objects.get(0);

        Object toOnePrefetch = fk1.readNestedProperty("toCompoundPk");
        assertNotNull(toOnePrefetch);
        assertTrue(
                "Expected Persistent, got: " + toOnePrefetch.getClass().getName(),
                toOnePrefetch instanceof Persistent);

        Persistent pk1 = (Persistent) toOnePrefetch;
        assertEquals(PersistenceState.COMMITTED, pk1.getPersistenceState());
        assertEquals("CPK2", pk1.readPropertyDirectly("name"));
    }

    /**
     * Tests to-many prefetching over relationships with compound keys.
     */
    @Test
    public void testPrefetch11() throws Exception {
        createCompoundDataSet();

        ObjectSelect<CompoundPkTestEntity> q = ObjectSelect.query(CompoundPkTestEntity.class)
                .where(CompoundPkTestEntity.NAME.eq("CPK2"))
                .prefetch("compoundFkArray", PrefetchTreeNode.UNDEFINED_SEMANTICS);

        List<CompoundPkTestEntity> pks = q.select(context);
        assertEquals(1, pks.size());
        PersistentObject pk1 = pks.get(0);

        List<?> toMany = (List<?>) pk1.readPropertyDirectly("compoundFkArray");
        assertNotNull(toMany);
        assertFalse(((ValueHolder) toMany).isFault());
        assertEquals(2, toMany.size());

        PersistentObject fk1 = (PersistentObject) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());

        PersistentObject fk2 = (PersistentObject) toMany.get(1);
        assertEquals(PersistenceState.COMMITTED, fk2.getPersistenceState());
    }
}
