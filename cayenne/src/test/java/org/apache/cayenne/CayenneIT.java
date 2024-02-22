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

package org.apache.cayenne;

import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayenneIT extends RuntimeCase {

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
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");

    }

    private void createOneArtist() throws Exception {
        tArtist.insert(33002, "artist2");
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
    }

    @Test
    public void testReadNestedProperty_ToMany() throws Exception {

        tArtist.insert(1, "a");
        tPainting.insert(1, 1, "a1");
        tPainting.insert(2, 1, "a2");

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        Collection<String> titles = (Collection<String>) Cayenne.readNestedProperty(
                a,
                "paintingArray.paintingTitle");

        assertEquals(2, titles.size());
        assertTrue(titles.contains("a1"));
        assertTrue(titles.contains("a2"));

        int size = (Integer) Cayenne.readNestedProperty(a, "paintingArray.@size");
        assertEquals(2, size);
    }

    @Test
    public void testScalarObjectForQuery() throws Exception {
        createTwoArtists();

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = context.getEntityResolver().getDataMap("testmap");
        SQLTemplate query = new SQLTemplate(map, sql, false);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) AS X FROM ARTIST");
        query.setTemplate(
                MySQLAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) AS X FROM ARTIST");
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);

        SQLResult rsMap = new SQLResult();
        rsMap.addColumnResult("X");
        query.setResult(rsMap);

        Object object = Cayenne.objectForQuery(context, query);
        assertNotNull(object);
        assertTrue(object instanceof Number);
        assertEquals(2, ((Number) object).intValue());
    }

    @Test
    public void testScalarObjectForQuery2() throws Exception {
        createTwoArtists();

        String ejbql = "SELECT count(a) from Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        Object object = Cayenne.objectForQuery(context, query);
        assertNotNull(object);
        assertTrue(
                "Object class: " + object.getClass().getName(),
                object instanceof Number);
        assertEquals(2, ((Number) object).intValue());
    }

    @Test
    public void testMakePath() {
        assertEquals("", Cayenne.makePath());
        assertEquals("a", Cayenne.makePath("a"));
        assertEquals("a.b", Cayenne.makePath("a", "b"));
    }

    @Test
    public void testObjectForQuery() throws Exception {
        createOneArtist();

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 33002);

        assertNull(context.getGraphManager().getNode(id));

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void testObjectForSelect() throws Exception {
        createOneArtist();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, ExpressionFactory.matchDbExp("ARTIST_NAME", "artist2"));

        Artist object = context.selectOne(query);

        assertNotNull(object);
        assertEquals("artist2", object.getArtistName());
    }

    @Test
    public void testObjectForQueryNoObject() throws Exception {

        ObjectId id = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 44001);

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));
        assertNull(object);
    }

    @Test
    public void testNoObjectForPK() throws Exception {
        createOneArtist();

        // use bogus non-existent PK
        Object object = Cayenne.objectForPK(context, Artist.class, 44001);
        assertNull(object);
    }

    @Test
    public void testObjectForPKTemporary() throws Exception {

        Persistent o1 = context.newObject(Artist.class);
        Persistent o2 = context.newObject(Artist.class);
        assertSame(o1, Cayenne.objectForPK(context, o1.getObjectId()));
        assertSame(o2, Cayenne.objectForPK(context, o2.getObjectId()));

        assertNull(Cayenne.objectForPK(context, ObjectId.of("Artist", new byte[] {1, 2, 3})));
    }

    @Test
    public void testObjectForPKObjectId() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33002));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void testObjectForPKClassInt() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, Artist.class, 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void testObjectForPKEntityInt() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, "Artist", 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void testObjectForPKClassMap() throws Exception {
        createOneArtist();

        Map<String, Integer> pk = Collections.singletonMap(
                Artist.ARTIST_ID_PK_COLUMN,
                33002);
        Object object = Cayenne.objectForPK(context, Artist.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    @Test
    public void testIntPKForObject() throws Exception {
        createOneArtist();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        assertEquals(1, objects.size());
        Persistent object = objects.get(0);

        assertEquals(33002, Cayenne.intPKForObject(object));
    }

    @Test
    public void testPKForObject() throws Exception {
        createOneArtist();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        assertEquals(1, objects.size());
        Persistent object = objects.get(0);

        assertEquals(33002L, Cayenne.pkForObject(object));
    }

    @Test
    public void testEjbql() throws Exception {
        createOneArtist();

        List<?> objects = context.performQuery(new EJBQLQuery("select a from Artist a"));
        assertEquals(1, objects.size());
        Artist object = (Artist) objects.get(0);

        assertEquals(33002L, Cayenne.pkForObject(object));
    }

}
