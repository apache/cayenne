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
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.test.junit.AssertExtras;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CompoundFkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEJBQLQueryTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    private TableHelper tArtist;
    private TableHelper tPainting;
    private TableHelper tCompoundPk;
    private TableHelper tCompoundFk;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);

        tCompoundPk = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPk.setColumns("KEY1", "KEY2");

        tCompoundFk = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFk.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    private void createFourArtistsTwoPaintings() throws Exception {
        tArtist.insert(33001, "AA1");
        tArtist.insert(33002, "AA2");
        tArtist.insert(33003, "BB1");
        tArtist.insert(33004, "BB2");
        tPainting.insert(33001, 33001, "P1", 3000);
        tPainting.insert(33002, 33002, "P2", 5000);
    }

    private void createTwoCompoundPKTwoFK() throws Exception {
        tCompoundPk.insert("a1", "a2");
        tCompoundPk.insert("b1", "b2");
        tCompoundFk.insert(33001, "a1", "a2");
        tCompoundFk.insert(33002, "b1", "b2");
    }

    /**
     * CAY-899: Checks that aggregate results do not cause callbacks execution.
     */
    public void testSelectAggregatePostLoadCallback() throws Exception {

        createFourArtistsTwoPaintings();

        LifecycleCallbackRegistry existingCallbacks = context
                .getEntityResolver()
                .getCallbackRegistry();
        LifecycleCallbackRegistry testCallbacks = new LifecycleCallbackRegistry(
                context.getEntityResolver());

        DataContextEJBQLQueryCallback listener = new DataContextEJBQLQueryCallback();
        testCallbacks.addDefaultListener(LifecycleEvent.POST_LOAD, listener, "postLoad");

        context.getEntityResolver().setCallbackRegistry(testCallbacks);

        try {
            String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
            EJBQLQuery query = new EJBQLQuery(ejbql);

            List<?> data = context.performQuery(query);

            assertFalse(listener.postLoad);

            assertEquals(1, data.size());
            assertTrue(data.get(0) instanceof Object[]);
        }
        finally {
            context.getEntityResolver().setCallbackRegistry(existingCallbacks);
        }
    }

    public void testSelectAggregate() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = context.performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(new Long(2), aggregates[0]);
        assertEquals(new Long(2), aggregates[1]);
        AssertExtras.assertEquals(new BigDecimal(5000d), aggregates[2], 0.01);
        AssertExtras.assertEquals(new BigDecimal(8000d), aggregates[3], 0.01);
    }

    public void testSelectAggregateNull() throws Exception {

        if (!accessStackAdapter.supportNullRowForAggregateFunctions()) {
            return;
        }

        String ejbql = "select count(p), max(p.estimatedPrice), sum(p.estimatedPrice) "
                + "from Painting p WHERE p.paintingTitle = 'X'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = context.performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(new Long(0), aggregates[0]);
        assertEquals(null, aggregates[1]);
        assertEquals(null, aggregates[2]);
    }

    public void testSelectEntityPathsScalarResult() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select p.paintingTitle"
                + " from Painting p order by p.paintingTitle DESC";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = context.performQuery(query);
        assertEquals(2, data.size());

        assertEquals("P2", data.get(0));
        assertEquals("P1", data.get(1));
    }

    public void testSelectEntityPathsArrayResult() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select p.estimatedPrice, p.toArtist.artistName "
                + "from Painting p order by p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = context.performQuery(query);
        assertEquals(2, data.size());

        assertTrue(data.get(0) instanceof Object[]);
        Object[] row0 = (Object[]) data.get(0);
        assertEquals(2, row0.length);
        AssertExtras.assertEquals(new BigDecimal(3000d), row0[0], 0.01);
        assertEquals("AA1", row0[1]);

        assertTrue(data.get(1) instanceof Object[]);
        Object[] row1 = (Object[]) data.get(1);
        assertEquals(2, row1.length);
        AssertExtras.assertEquals(new BigDecimal(5000d), row1[0], 0.01);
        assertEquals("AA2", row1[1]);
    }

    public void testSimpleSelect() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(4, artists.size());
        assertTrue(artists.get(0) instanceof Artist);
        assertTrue(((Artist) artists.get(0)).getPersistenceState() == PersistenceState.COMMITTED);
    }

    public void testFetchLimit() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setFetchLimit(2);

        List<?> artists = context.performQuery(query);
        assertEquals(2, artists.size());
    }

    public void testSelectFromWhereEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    public void testSelectFromWhereEqualReverseOrder() throws Exception {
        if (!accessStackAdapter.supportsReverseComparison()) {
            return;
        }

        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where 'AA2' = a.artistName";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    public void testSelectFromWhereNot() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where not a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(3, artists.size());
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    public void testSelectFromWhereNotEquals() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName <> 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(3, artists.size());
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    public void testSelectFromWhereOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName = 'AA2' or a.artistName = 'BB1'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = context.performQuery(query);
        assertEquals(2, artists.size());

        Set<String> names = new HashSet<String>();
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            names.add(((Artist) it.next()).getArtistName());
        }

        assertTrue(names.contains("AA2"));
        assertTrue(names.contains("BB1"));
    }

    public void testSelectFromWhereAndEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P where P.paintingTitle = 'P1' "
                + "AND p.estimatedPrice = 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        AssertExtras.assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereBetween() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        AssertExtras.assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereNotBetween() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice NOT BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        AssertExtras.assertEquals(new BigDecimal(5000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereGreater() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice > 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        AssertExtras.assertEquals(new BigDecimal(5000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereGreaterOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice >= 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereLess() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice < 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        AssertExtras.assertEquals(new BigDecimal(3000d), p.getEstimatedPrice(), 0.01);
    }

    public void testSelectFromWhereLessOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumber() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000.00";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumberPositional() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= ?1";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, new BigDecimal(5000.00));

        List<?> ps = context.performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereDecimalNumberNamed() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", new BigDecimal(5000.00));

        List<?> ps = context.performQuery(query);
        assertEquals(2, ps.size());
    }

    public void testSelectFromWhereMatchOnObject() throws Exception {
        createFourArtistsTwoPaintings();

        Artist a = Cayenne.objectForPK(context, Artist.class, 33002);

        String ejbql = "select P from Painting P WHERE p.toArtist = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(p));
    }

    public void testSelectFromWhereMatchRelationshipAndScalar() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.toArtist = 33002";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(p));
    }

    public void testSelectFromWhereMatchOnMultiColumnObject() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(o1));
    }

    public void testSelectFromWhereMatchOnMultiColumnObjectReverse() throws Exception {
        if (!accessStackAdapter.supportsReverseComparison()) {
            return;
        }

        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE :param = e.toCompoundPk";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(o1));
    }

    public void testSelectFromWhereNoMatchOnMultiColumnObject() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<String, String>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        String ejbql = "select e from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = context.performQuery(query);
        assertEquals(1, ps.size());

        CompoundFkTestEntity o1 = (CompoundFkTestEntity) ps.get(0);
        assertEquals(33001, Cayenne.intPKForObject(o1));
    }
}
