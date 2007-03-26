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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLQueryTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
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

    public void testSelectFromWhereEqual() throws Exception {
        createTestData("prepare");

        String ejbql = "select a from Artist a where a.artistName = 'AA2'";
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
        assertEquals(new BigDecimal(3000), p.getEstimatedPrice());
    }

    public void testSelectFromWhereGreater() throws Exception {
        createTestData("prepare");

        String ejbql = "select P from Painting P WHERE p.estimatedPrice > 3000";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List ps = createDataContext().performQuery(query);
        assertEquals(1, ps.size());

        Painting p = (Painting) ps.get(0);
        assertEquals("P2", p.getPaintingTitle());
        assertEquals(new BigDecimal(5000), p.getEstimatedPrice());
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
        assertEquals(new BigDecimal(3000), p.getEstimatedPrice());
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
}
