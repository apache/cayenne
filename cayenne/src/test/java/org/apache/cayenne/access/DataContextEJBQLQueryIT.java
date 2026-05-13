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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataContextEJBQLQueryIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private UnitDbAdapter accessStackAdapter;

    private TableHelper tArtist;
    private TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        accessStackAdapter = env.getInstance(UnitDbAdapter.class);
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING").setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);
    }

    private void createFourArtistsTwoPaintings() throws Exception {
        tArtist.insert(33001, "AA1");
        tArtist.insert(33002, "AA2");
        tArtist.insert(33003, "BB1");
        tArtist.insert(33004, "BB2");
        tPainting.insert(33001, 33001, "P1", 3000);
        tPainting.insert(33002, 33002, "P2", 5000);
    }

    /**
     * CAY-899: Checks that aggregate results do not cause callbacks execution.
     */
    @Test
    public void selectAggregatePostLoadCallback() throws Exception {

        createFourArtistsTwoPaintings();

        LifecycleCallbackRegistry existingCallbacks = env.context()
                .getEntityResolver()
                .getCallbackRegistry();
        LifecycleCallbackRegistry testCallbacks = new LifecycleCallbackRegistry(
                env.context().getEntityResolver());

        DataContextEJBQLQueryCallback listener = new DataContextEJBQLQueryCallback();
        testCallbacks.addDefaultListener(LifecycleEvent.POST_LOAD, listener, "postLoad");

        env.context().getEntityResolver().setCallbackRegistry(testCallbacks);

        try {
            String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
            EJBQLQuery query = new EJBQLQuery(ejbql);

            List<?> data = env.context().performQuery(query);

            assertFalse(listener.postLoad);

            assertEquals(1, data.size());
            assertTrue(data.get(0) instanceof Object[]);
        }
        finally {
            env.context().getEntityResolver().setCallbackRegistry(existingCallbacks);
        }
    }

    @Test
    public void selectAggregate() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select count(p), count(distinct p.estimatedPrice), max(p.estimatedPrice), sum(p.estimatedPrice) from Painting p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(2L, aggregates[0]);
        assertEquals(2L, aggregates[1]);
        assertEquals(5000d, ((BigDecimal) aggregates[2]).doubleValue(), 0.00001);
        assertEquals(8000d, ((BigDecimal) aggregates[3]).doubleValue(), 0.00001);
    }

    @Test
    public void selectAggregateNull() throws Exception {

        if (!accessStackAdapter.supportNullRowForAggregateFunctions()) {
            return;
        }

        String ejbql = "select count(p), max(p.estimatedPrice), sum(p.estimatedPrice) "
                + "from Painting p WHERE p.paintingTitle = 'X'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);
        Object[] aggregates = (Object[]) data.get(0);
        assertEquals(0L, aggregates[0]);
        assertEquals(null, aggregates[1]);
        assertEquals(null, aggregates[2]);
    }

    @Test
    public void selectEntityPathsScalarResult() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select p.paintingTitle"
                + " from Painting p order by p.paintingTitle DESC";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(2, data.size());

        assertEquals("P2", data.get(0));
        assertEquals("P1", data.get(1));
    }

    @Test
    public void selectEntityPathsArrayResult() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select p.estimatedPrice, p.toArtist.artistName "
                + "from Painting p order by p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(2, data.size());

        assertTrue(data.get(0) instanceof Object[]);
        Object[] row0 = (Object[]) data.get(0);
        assertEquals(2, row0.length);
        assertEquals(3000d, ((BigDecimal) row0[0]).doubleValue(), 0.00001);
        assertEquals("AA1", row0[1]);

        assertTrue(data.get(1) instanceof Object[]);
        Object[] row1 = (Object[]) data.get(1);
        assertEquals(2, row1.length);
        assertEquals(5000d, ((BigDecimal) row1[0]).doubleValue(), 0.00001);
        assertEquals("AA2", row1[1]);
    }

    @Test
    public void selectDbPath() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select db:p.ESTIMATED_PRICE "
                + "from Painting p order by p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(2, data.size());

        assertEquals(3000d, ((BigDecimal) data.get(0)).doubleValue(), 0.00001);
        assertEquals(5000d, ((BigDecimal) data.get(1)).doubleValue(), 0.00001);
    }

    @Test
    public void selectDbPath_Relationship() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select db:p.toArtist "
                + "from Painting p order by p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> data = env.context().performQuery(query);
        assertEquals(2, data.size());

        assertTrue(data.get(0) instanceof Artist);
        assertEquals(33001, Cayenne.intPKForObject((Artist) data.get(0)));

        assertTrue(data.get(1) instanceof Artist);
        assertEquals(33002, Cayenne.intPKForObject((Artist) data.get(1)));
    }

    @Test
    public void simpleSelect() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(4, artists.size());
        assertTrue(artists.get(0) instanceof Artist);
        assertTrue(((Artist) artists.get(0)).getPersistenceState() == PersistenceState.COMMITTED);
    }

    @Test
    public void fetchLimit() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a FROM Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setFetchLimit(2);

        List<?> artists = env.context().performQuery(query);
        assertEquals(2, artists.size());
    }

    @Test
    public void selectFromWhereEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    @Test
    public void selectFromWhereEqualReverseOrder() throws Exception {
        if (!accessStackAdapter.supportsReverseComparison()) {
            return;
        }

        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where 'AA2' = a.artistName";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(1, artists.size());
        assertEquals("AA2", ((Artist) artists.get(0)).getArtistName());
    }

    @Test
    public void selectFromWhereNot() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where not a.artistName = 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(3, artists.size());
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    @Test
    public void selectFromWhereNotEquals() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName <> 'AA2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(3, artists.size());
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            Artist a = (Artist) it.next();
            assertFalse("AA2".equals(a.getArtistName()));
        }
    }

    @Test
    public void selectFromWhereOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select a from Artist a where a.artistName = 'AA2' or a.artistName = 'BB1'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> artists = env.context().performQuery(query);
        assertEquals(2, artists.size());

        Set<String> names = new HashSet<String>();
        Iterator<?> it = artists.iterator();
        while (it.hasNext()) {
            names.add(((Artist) it.next()).getArtistName());
        }

        assertTrue(names.contains("AA2"));
        assertTrue(names.contains("BB1"));
    }

    @Test
    public void selectFromWhereAndEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P where P.paintingTitle = 'P1' "
                + "AND p.estimatedPrice = 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(3000d, p.getEstimatedPrice().doubleValue(), 0.00001);
    }

    @Test
    public void selectFromWhereBetween() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(3000d, p.getEstimatedPrice().doubleValue(), 0.00001);
    }

    @Test
    public void selectFromWhereNotBetween() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice NOT BETWEEN 2000 AND 3500";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        assertEquals(5000d, p.getEstimatedPrice().doubleValue(), 0.00001);
    }

    @Test
    public void selectFromWhereGreater() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice > 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        assertEquals(5000d, p.getEstimatedPrice().doubleValue(), 0.00001);
    }

    @Test
    public void selectFromWhereGreaterOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice >= 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(2, ps.size());
    }

    @Test
    public void selectFromWhereLess() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice < 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P1", p.getPaintingTitle());
        assertEquals(3000d, p.getEstimatedPrice().doubleValue(), 0.00001);
    }

    @Test
    public void selectFromWhereLessOrEqual() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(2, ps.size());
    }

    @Test
    public void selectFromWhereDecimalNumber() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= 5000.00";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(2, ps.size());
    }

    @Test
    public void selectFromWhereDecimalNumberPositional() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= ?1";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1, new BigDecimal(5000.00));

        List<?> ps = env.context().performQuery(query);
        assertEquals(2, ps.size());
    }

    @Test
    public void selectFromWhereDecimalNumberNamed() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.estimatedPrice <= :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", new BigDecimal(5000.00));

        List<?> ps = env.context().performQuery(query);
        assertEquals(2, ps.size());
    }

    @Test
    public void selectFromWhereMatchOnObject() throws Exception {
        createFourArtistsTwoPaintings();

        Artist a = Cayenne.objectForPK(env.context(), Artist.class, 33002);

        String ejbql = "select P from Painting P WHERE p.toArtist = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", a);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(p));
    }

    @Test
    public void selectFromWhereMatchRelationshipAndScalar() throws Exception {
        createFourArtistsTwoPaintings();

        String ejbql = "select P from Painting P WHERE p.toArtist = 33002";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List<?> ps = env.context().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals(33002, Cayenne.intPKForObject(p));
    }

}
