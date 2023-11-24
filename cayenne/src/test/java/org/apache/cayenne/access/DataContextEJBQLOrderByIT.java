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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLOrderByIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
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
    }

    private void createThreePaintings() throws Exception {
        tPainting.insert(33001, null, "A", 3000);
        tPainting.insert(33002, null, "B", 2000);
        tPainting.insert(33003, null, "C", 1000);
    }

    private void createFourPaintings() throws Exception {
        tPainting.insert(33001, null, "A", 3000);
        tPainting.insert(33002, null, "B", 2000);
        tPainting.insert(33003, null, "C", 1000);
        tPainting.insert(33004, null, "C", 500);
    }

    private void createTwoArtistsTwoPaintings() throws Exception {
        tArtist.insert(33001, "A");
        tArtist.insert(33002, "B");
        tPainting.insert(33005, 33001, "C", 500);
        tPainting.insert(33006, 33002, "C", 500);
    }

    @Test
    public void testOrderByDefault() throws Exception {

        createThreePaintings();

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33001, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33003, Cayenne.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> results2 = context.performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33003, Cayenne.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results2.get(2)));
    }

    @Test
    public void testOrderByAsc() throws Exception {

        createThreePaintings();

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33001, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33003, Cayenne.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice ASC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> results2 = context.performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33003, Cayenne.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results2.get(2)));
    }

    @Test
    public void testOrderByDesc() throws Exception {
        createThreePaintings();

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle DESC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33003, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice DESC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> results2 = context.performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33001, Cayenne.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33003, Cayenne.intPKForObject((Persistent) results2.get(2)));
    }

    @Test
    public void testOrderByQualified() throws Exception {
        createThreePaintings();

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice > 1000 ORDER BY p.paintingTitle ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(2, results1.size());

        assertEquals(33001, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results1.get(1)));

        String ejbql2 = "SELECT p FROM Painting p WHERE p.estimatedPrice > 1000 ORDER BY p.estimatedPrice ASC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> results2 = context.performQuery(query2);
        assertEquals(2, results2.size());

        assertEquals(33002, Cayenne.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results2.get(1)));
    }

    @Test
    public void testOrderByMultiple() throws Exception {
        createFourPaintings();

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle DESC, p.estimatedPrice DESC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(4, results1.size());

        assertEquals(33003, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33004, Cayenne.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33002, Cayenne.intPKForObject((Persistent) results1.get(2)));
        assertEquals(33001, Cayenne.intPKForObject((Persistent) results1.get(3)));
    }

    @Test
    public void testOrderByPath() throws Exception {
        createTwoArtistsTwoPaintings();

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.toArtist.artistName ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List<?> results1 = context.performQuery(query1);
        assertEquals(2, results1.size());

        assertEquals(33005, Cayenne.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33006, Cayenne.intPKForObject((Persistent) results1.get(1)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.toArtist.artistName DESC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List<?> results2 = context.performQuery(query2);
        assertEquals(2, results2.size());

        assertEquals(33006, Cayenne.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33005, Cayenne.intPKForObject((Persistent) results2.get(1)));
    }
}
