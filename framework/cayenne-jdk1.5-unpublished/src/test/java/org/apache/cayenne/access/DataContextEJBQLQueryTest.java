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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.art.Artist;
import org.apache.art.CompoundFkTestEntity;
import org.apache.art.CompoundPkTestEntity;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLQueryTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    /**
     * CAY-899: Checks that aggregate results do not cause callbacks execution.
     */
    public void testSelectAggregatePostLoadCallback() throws Exception {

        createTestData("prepare");
        DataContext context = createDataContext();

        LifecycleCallbackRegistry existingCallbacks = context
                .getEntityResolver()
                .getCallbackRegistry();
        LifecycleCallbackRegistry testCallbacks = new LifecycleCallbackRegistry(context
                .getEntityResolver());

        DataContextEJBQLQueryCallback listener = new DataContextEJBQLQueryCallback();
        testCallbacks.addDefaultListener(
                LifecycleEvent.POST_LOAD,
                listener,
                "postLoad");

        context.getEntityResolver().setCallbackRegistry(testCallbacks);

        try {
            String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
            EJBQLQuery query = new EJBQLQuery(ejbql);

            List data = createDataContext().performQuery(query);
            
            assertFalse(listener.postLoad);
            
            assertEquals(1, data.size());
            assertTrue(data.get(0) instanceof Object[]);
        }
        finally {
            context.getEntityResolver().setCallbackRegistry(existingCallbacks);
        }
    }

    public void testSelectAggregate() throws Exception {
        createTestData("prepare");

        String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(new Long(2), aggregates[0]);
        assertEquals(new Long(2), aggregates[1]);
        assertEquals(new BigDecimal(5000d), aggregates[2], 0.01);
        assertEquals(new BigDecimal(8000d), aggregates[3], 0.01);
    }

    public void testSelectAggregateNull() throws Exception {

        if (!getAccessStackAdapter().supportNullRowForAggregateFunctions()) {
            return;
        }

        String ejbql = "select count(p), max(p.estimatedPrice), sum(p.estimatedPrice) "
                + "from Painting p WHERE p.paintingTitle = 'X'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(new Long(0), aggregates[0]);
        assertEquals(null, aggregates[1]);
        assertEquals(null, aggregates[2]);
    }

    public void testSelectEntityPathsScalarResult() throws Exception {
        createTestData("prepare");

        String ejbql = "select p.paintingTitle"
                + " from Painting p order by p.paintingTitle DESC";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(2, data.size());

        assertEquals("P2", data.get(0));
        assertEquals("P1", data.get(1));
    }

    public void testSelectEntityPathsArrayResult() throws Exception {
        createTestData("prepare");

        String ejbql = "select p.estimatedPrice, p.toArtist.artistName "
                + "from Painting p order by p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(2, data.size());

        assertTrue(data.get(0) instanceof Object[]);
        Object[] row0 = (Object[]) data.get(0);
        assertEquals(2, row0.length);
        assertEquals(new BigDecimal(3000d), row0[0], 0.01);
        assertEquals("AA1", row0[1]);

        assertTrue(data.get(1) instanceof Object[]);
        Object[] row1 = (Object[]) data.get(1);
        assertEquals(2, row1.length);
        assertEquals(new BigDecimal(5000d), row1[0], 0.01);
        assertEquals("AA2", row1[1]);
    }

    public void testSimpleSelect() throws Exception {
        createTestData("prepare");

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(4, artists.size());
        assertTrue(artists.get(0) instanceof Artist);
        assertTrue(((Artist) artists.get(0)).getPersistenceState() == PersistenceState.COMMITTED);
    }

    public void testFetchLimit() throws Exception {
        createTestData("prepare");

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setFetchLimit(2);

        List artists = createDataContext().performQuery(query);
        assertEquals(2, artists.size());
    }

    public void testSelectFromWhereEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select a from Artist a where a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    public void testSelectFromWhereEqualReverseOrder() throws Exception {
        if (!getAccessStackAdapter().supportsReverseComparison()) {
            return;
        }

        createTestData("prepare");

        String ejbql = "select a from Artist a where 'AA2' = a.artistName";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    public void testSelectFromWhereNot() throws Exception {
        createTestData("prepare");

        String ejbql = "select a from Artist a where not a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(3, artists.size());
        Iterator it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    public void testSelectFromWhereNotEquals() throws Exception {
        createTestData("prepare");

        String ejbql = "select a from Artist a where a.artistName <> 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(3, artists.size());
        Iterator it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    public void testSelectFromWhereOrEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select a from Artist a where a.artistName = 'AA2' or a.artistName = 'BB1'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List artists = createDataContext().performQuery(query);
        assertEquals(2, artists.size());

        Set names = new HashSet();
        Iterator it = artists.iterator();
        while (it.hasNext()) {
            names.add(((Artist) it.next()).getArtistName());
        }

        assertTrue(names.contains("AA2"));
        assertTrue(names.contains("BB1"));
    }

    public void testSelectFromWhereAndEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P where P.paintingTitle = 'P1' "
                + "AND p.estimatedPrice = 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereBetween() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereNotBetween() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice NOT BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        assertEquals(new BigDecimal(5000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereGreater() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice > 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        assertEquals(new BigDecimal(5000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereGreaterOrEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice >= 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereLess() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice < 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereLessOrEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumber() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000.00";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumberPositional() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= ?1";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, new BigDecimal(5000.00));

        List ps = createDataContext().performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumberNamed() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", new BigDecimal(5000.00));

        List ps = createDataContext().performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereMatchOnObject() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        String ejbql = "select P from Painting P WHERE p.toArtist = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testSelectFromWhereMatchRelationshipAndScalar() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        String ejbql = "select P from Painting P WHERE p.toArtist = 33002";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(p));
    }

    public void testSelectFromWhereMatchOnMultiColumnObject() throws Exception {
        createTestData("prepareCompound");

        ObjectContext context = createDataContext();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(o1));
    }

    public void testSelectFromWhereMatchOnMultiColumnObjectReverse() throws Exception {
        if (!getAccessStackAdapter().supportsReverseComparison()) {
            return;
        }

        createTestData("prepareCompound");

        ObjectContext context = createDataContext();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE :param = e.toCompoundPk";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, DataObjectUtils.intPKForObject(o1));
    }

    public void testSelectFromWhereNoMatchOnMultiColumnObject() throws Exception {
        createTestData("prepareCompound");

        ObjectContext context = createDataContext();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = DataObjectUtils.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33001, DataObjectUtils.intPKForObject(o1));
    }
}
