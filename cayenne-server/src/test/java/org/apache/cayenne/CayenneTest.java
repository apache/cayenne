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

package org.apache.cayenne;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CharPkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class CayenneTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;
    protected TableHelper tCompoundPKTest;
    protected TableHelper tCharPKTest;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("COMPOUND_FK_TEST");
        dbHelper.deleteAll("COMPOUND_PK_TEST");
        dbHelper.deleteAll("CHAR_PK_TEST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");

        tCompoundPKTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPKTest.setColumns("KEY1", "KEY2", "NAME");

        tCharPKTest = new TableHelper(dbHelper, "CHAR_PK_TEST");
        tCharPKTest.setColumns("PK_COL", "OTHER_COL");
    }

    private void createOneCompoundPK() throws Exception {
        tCompoundPKTest.insert("PK1", "PK2", "BBB");
    }

    private void createOneCharPK() throws Exception {
        tCharPKTest.insert("CPK", "AAAA");
    }

    private void createOneArtist() throws Exception {
        tArtist.insert(33002, "artist2");
    }

    private void createTwoArtists() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
    }

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

    public void testScalarObjectForQuery() throws Exception {
        createTwoArtists();

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = context.getEntityResolver().getDataMap("tstmap");
        SQLTemplate query = new SQLTemplate(map, sql, false);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "SELECT COUNT(ARTIST_ID) AS X FROM ARTIST");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
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
    
    public void testMakePath() {
        assertEquals("", Cayenne.makePath());
        assertEquals("a", Cayenne.makePath("a"));
        assertEquals("a.b", Cayenne.makePath("a", "b"));
    }

    public void testObjectForQuery() throws Exception {
        createOneArtist();

        ObjectId id = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, new Integer(
                33002));

        assertNull(context.getGraphManager().getNode(id));

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForSelect() throws Exception {
        createOneArtist();

        SelectQuery<Artist> query = SelectQuery.query(Artist.class, ExpressionFactory.matchDbExp("ARTIST_NAME", "artist2"));

        Artist object = context.selectOne(query);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }
    
    public void testObjectForQueryNoObject() throws Exception {

        ObjectId id = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, new Integer(
                44001));

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));
        assertNull(object);
    }

    public void testNoObjectForPK() throws Exception {
        createOneArtist();

        // use bogus non-existent PK
        Object object = Cayenne.objectForPK(context, Artist.class, 44001);
        assertNull(object);
    }

    public void testObjectForPKTemporary() throws Exception {

        Persistent o1 = context.newObject(Artist.class);
        Persistent o2 = context.newObject(Artist.class);
        assertSame(o1, Cayenne.objectForPK(context, o1.getObjectId()));
        assertSame(o2, Cayenne.objectForPK(context, o2.getObjectId()));

        assertNull(Cayenne.objectForPK(context, new ObjectId("Artist", new byte[] {
                1, 2, 3
        })));
    }

    public void testObjectForPKObjectId() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                33002));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassInt() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, Artist.class, 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityInt() throws Exception {
        createOneArtist();

        Object object = Cayenne.objectForPK(context, "Artist", 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassMap() throws Exception {
        createOneArtist();

        Map<String, Integer> pk = Collections.singletonMap(
                Artist.ARTIST_ID_PK_COLUMN,
                new Integer(33002));
        Object object = Cayenne.objectForPK(context, Artist.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityMapCompound() throws Exception {
        createOneCompoundPK();

        Map<String, Object> pk = new HashMap<String, Object>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "PK1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "PK2");
        Object object = Cayenne.objectForPK(context, CompoundPkTestEntity.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof CompoundPkTestEntity);
        assertEquals("BBB", ((CompoundPkTestEntity) object).getName());
    }

    public void testCompoundPKForObject() throws Exception {
        createOneCompoundPK();

        List<?> objects = context
                .performQuery(new SelectQuery(CompoundPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        Map<String, Object> pk = Cayenne.compoundPKForObject(object);
        assertNotNull(pk);
        assertEquals(2, pk.size());
        assertEquals("PK1", pk.get(CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals("PK2", pk.get(CompoundPkTestEntity.KEY2_PK_COLUMN));
    }

    public void testIntPKForObjectFailureForCompound() throws Exception {
        createOneCompoundPK();

        List<?> objects = context
                .performQuery(new SelectQuery(CompoundPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            Cayenne.intPKForObject(object);
            fail("intPKForObject must fail for compound key");
        }
        catch (CayenneRuntimeException ex) {
            // expected
        }
    }

    public void testIntPKForObjectFailureForNonNumeric() throws Exception {
        createOneCharPK();

        List<?> objects = context.performQuery(new SelectQuery(CharPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            Cayenne.intPKForObject(object);
            fail("intPKForObject must fail for non-numeric key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testPKForObjectFailureForCompound() throws Exception {
        createOneCompoundPK();

        List<?> objects = context
                .performQuery(new SelectQuery(CompoundPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            Cayenne.pkForObject(object);
            fail("pkForObject must fail for compound key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testIntPKForObject() throws Exception {
        createOneArtist();

        List<?> objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(33002, Cayenne.intPKForObject(object));
    }

    public void testPKForObject() throws Exception {
        createOneArtist();

        List<?> objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(new Long(33002), Cayenne.pkForObject(object));
    }

    public void testIntPKForObjectNonNumeric() throws Exception {
        createOneCharPK();

        List<?> objects = context.performQuery(new SelectQuery(CharPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals("CPK", Cayenne.pkForObject(object));
    }
}
