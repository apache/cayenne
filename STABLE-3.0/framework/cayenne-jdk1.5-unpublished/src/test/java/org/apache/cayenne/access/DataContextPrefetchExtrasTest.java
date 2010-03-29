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

import org.apache.art.CharFkTestEntity;
import org.apache.art.CharPkTestEntity;
import org.apache.art.CompoundFkTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * Test prefetching of various obscure cases.
 * 
 */
public class DataContextPrefetchExtrasTest extends CayenneCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testPrefetchToManyOnCharKey() throws Exception {
        createTestData("testPrefetchToManyOnCharKey");

        SelectQuery q = new SelectQuery(CharPkTestEntity.class);
        q.addPrefetch("charFKs");
        q.addOrdering(CharPkTestEntity.OTHER_COL_PROPERTY, Ordering.ASC);

        List pks = context.performQuery(q);
        assertEquals(2, pks.size());

        CharPkTestEntity pk1 = (CharPkTestEntity) pks.get(0);
        assertEquals("n1", pk1.getOtherCol());
        List toMany = (List) pk1.readPropertyDirectly("charFKs");
        assertNotNull(toMany);
        assertFalse(((ValueHolder)toMany).isFault());
        assertEquals(3, toMany.size());

        CharFkTestEntity fk1 = (CharFkTestEntity) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());
        assertSame(pk1, fk1.getToCharPK());
    }

    /**
     * Tests to-one prefetching over relationships with compound keys.
     */
    public void testPrefetch10() throws Exception {
        createTestData("testCompound");

        Expression e = ExpressionFactory.matchExp("name", "CFK2");
        SelectQuery q = new SelectQuery(CompoundFkTestEntity.class, e);
        q.addPrefetch("toCompoundPk");

        List objects = context.performQuery(q);
        assertEquals(1, objects.size());
        CayenneDataObject fk1 = (CayenneDataObject) objects.get(0);

        blockQueries();
        try {

            Object toOnePrefetch = fk1.readNestedProperty("toCompoundPk");
            assertNotNull(toOnePrefetch);
            assertTrue(
                    "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
                    toOnePrefetch instanceof DataObject);

            DataObject pk1 = (DataObject) toOnePrefetch;
            assertEquals(PersistenceState.COMMITTED, pk1.getPersistenceState());
            assertEquals("CPK2", pk1.readPropertyDirectly("name"));
        }
        finally {
            unblockQueries();
        }
    }

    /**
     * Tests to-many prefetching over relationships with compound keys.
     */
    public void testPrefetch11() throws Exception {
        createTestData("testCompound");

        Expression e = ExpressionFactory.matchExp("name", "CPK2");
        SelectQuery q = new SelectQuery(CompoundPkTestEntity.class, e);
        q.addPrefetch("compoundFkArray");

        List pks = context.performQuery(q);
        assertEquals(1, pks.size());
        CayenneDataObject pk1 = (CayenneDataObject) pks.get(0);

        List toMany = (List) pk1.readPropertyDirectly("compoundFkArray");
        assertNotNull(toMany);
        assertFalse(((ValueHolder) toMany).isFault());
        assertEquals(2, toMany.size());

        CayenneDataObject fk1 = (CayenneDataObject) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());

        CayenneDataObject fk2 = (CayenneDataObject) toMany.get(1);
        assertEquals(PersistenceState.COMMITTED, fk2.getPersistenceState());
    }
}
