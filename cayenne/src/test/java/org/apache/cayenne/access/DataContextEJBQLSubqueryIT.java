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
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextEJBQLSubqueryIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

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

    private void createTwoArtistsFourPaintings() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tPainting.insert(33001, 33001, "P1", 3000);
        tPainting.insert(33002, 33001, "P2", 4000);
        tPainting.insert(33003, null, "P1", 5000);
        tPainting.insert(33004, null, "P4", 6000);
    }

    @Test
    public void testSubqueryNoQualifier() throws Exception {
        if (!accessStackAdapter.supportsAllAnySome()) {
            return;
        }

        createTwoArtistsFourPaintings();

        String ejbql = "SELECT DISTINCT p FROM Painting p"
                + " WHERE p.estimatedPrice = ALL ("
                + " SELECT MAX(p1.estimatedPrice) FROM Painting p1"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33004));
    }

    @Test
    public void testDifferentEntity() throws Exception {
        createTwoArtistsFourPaintings();

        String ejbql = "SELECT a FROM Artist a"
                + " WHERE EXISTS ("
                + " SELECT DISTINCT p1.paintingTitle FROM Painting p1"
                + " WHERE p1.toArtist = a"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33001L));

        assertTrue("" + objects.get(0), objects.get(0) instanceof Artist);
    }

    @Test
    public void testExists() throws Exception {
        createTwoArtistsFourPaintings();

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE EXISTS ("
                + " SELECT DISTINCT p1.paintingTitle FROM Painting p1"
                + " WHERE p1.paintingTitle = p.paintingTitle"
                + " AND p.estimatedPrice <> p1.estimatedPrice"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33001));
        assertTrue(ids.contains(33003));
    }

    @Test
    public void testAll() throws Exception {
        if (!accessStackAdapter.supportsAllAnySome()) {
            return;
        }

        createTwoArtistsFourPaintings();

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE p.estimatedPrice > ALL ("
                + " SELECT p1.estimatedPrice FROM Painting p1"
                + " WHERE p1.paintingTitle = 'P2'"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33003));
        assertTrue(ids.contains(33004));
    }

    @Test
    public void testAny() throws Exception {
        if (!accessStackAdapter.supportsAllAnySome()) {
            return;
        }

        createTwoArtistsFourPaintings();

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE p.estimatedPrice > ANY ("
                + " SELECT p1.estimatedPrice FROM Painting p1"
                + " WHERE p1.paintingTitle = 'P1'"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(3, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33002));
        assertTrue(ids.contains(33003));
        assertTrue(ids.contains(33004));
    }

    @Test
    public void testSome() throws Exception {
        if (!accessStackAdapter.supportsAllAnySome()) {
            return;
        }

        createTwoArtistsFourPaintings();

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE p.estimatedPrice > SOME ("
                + " SELECT p1.estimatedPrice FROM Painting p1"
                + " WHERE p1.paintingTitle = 'P1'"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(3, objects.size());

        Set<Object> ids = new HashSet<>();
        for (Object object : objects) {
            Object id = Cayenne.pkForObject((Persistent) object);
            ids.add(id);
        }

        assertTrue(ids.contains(33002));
        assertTrue(ids.contains(33003));
        assertTrue(ids.contains(33004));
    }
}
