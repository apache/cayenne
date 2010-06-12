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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CharPkTestEntity;
import org.apache.cayenne.testdo.testmap.CompoundPkTestEntity;
import org.apache.cayenne.unit.CayenneCase;

public class CayenneTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testReadNestedProperty_ToMany() throws Exception {

        TableHelper artistHelper = new TableHelper(getDbHelper(), "ARTIST");
        artistHelper.setColumns("ARTIST_ID", "ARTIST_NAME");
        artistHelper.insert(1, "a");

        TableHelper paintingHelper = new TableHelper(getDbHelper(), "PAINTING");
        paintingHelper.setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE");
        paintingHelper.insert(1, 1, "a1");
        paintingHelper.insert(2, 1, "a2");

        Artist a = Cayenne.objectForPK(createDataContext(), Artist.class, 1);
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
        createTestData("testScalarObjectForQuery");
        DataContext context = createDataContext();

        String sql = "SELECT count(1) AS X FROM ARTIST";

        DataMap map = getDomain().getMap("testmap");
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
        createTestData("testScalarObjectForQuery");
        DataContext context = createDataContext();

        String ejbql = "SELECT count(a) from Artist a";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        Object object = Cayenne.objectForQuery(context, query);
        assertNotNull(object);
        assertTrue(
                "Object class: " + object.getClass().getName(),
                object instanceof Number);
        assertEquals(2, ((Number) object).intValue());
    }

    public void testObjectForQuery() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        ObjectId id = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, new Integer(
                33002));

        assertNull(context.getGraphManager().getNode(id));

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForQueryNoObject() throws Exception {
        DataContext context = createDataContext();

        ObjectId id = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, new Integer(
                44001));

        Object object = Cayenne.objectForQuery(context, new ObjectIdQuery(id));
        assertNull(object);
    }

    public void testNoObjectForPK() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        // use bogus non-existent PK
        Object object = Cayenne.objectForPK(context, Artist.class, 44001);
        assertNull(object);
    }

    public void testObjectForPKTemporary() throws Exception {

        DataContext context = createDataContext();

        Persistent o1 = context.newObject(Artist.class);
        Persistent o2 = context.newObject(Artist.class);
        assertSame(o1, Cayenne.objectForPK(context, o1.getObjectId()));
        assertSame(o2, Cayenne.objectForPK(context, o2.getObjectId()));

        try {
            assertNull(Cayenne.objectForPK(context, new ObjectId("Artist", new byte[] {
                    1, 2, 3
            })));

            fail("An attempt to fetch an object for "
                    + "the non-existent temp id should have failed...");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }

    public void testObjectForPKObjectId() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        Object object = Cayenne.objectForPK(context, new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                new Integer(33002)));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassInt() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        Object object = Cayenne.objectForPK(context, Artist.class, 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityInt() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        Object object = Cayenne.objectForPK(context, "Artist", 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassMap() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        Map pk = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, new Integer(33002));
        Object object = Cayenne.objectForPK(context, Artist.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityMapCompound() throws Exception {
        createTestData("testObjectForPKCompound");
        DataContext context = createDataContext();

        Map pk = new HashMap();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "PK1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "PK2");
        Object object = Cayenne.objectForPK(context, CompoundPkTestEntity.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof CompoundPkTestEntity);
        assertEquals("BBB", ((CompoundPkTestEntity) object).getName());
    }

    public void testCompoundPKForObject() throws Exception {
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        Map pk = Cayenne.compoundPKForObject(object);
        assertNotNull(pk);
        assertEquals(2, pk.size());
        assertEquals("PK1", pk.get(CompoundPkTestEntity.KEY1_PK_COLUMN));
        assertEquals("PK2", pk.get(CompoundPkTestEntity.KEY2_PK_COLUMN));
    }

    public void testIntPKForObjectFailureForCompound() throws Exception {
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            Cayenne.intPKForObject(object);
            fail("intPKForObject must fail for compound key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testIntPKForObjectFailureForNonNumeric() throws Exception {
        createTestData("testIntPKForObjectNonNumeric");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CharPkTestEntity.class));
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
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTestEntity.class));
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
        createTestData("testIntPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(33001, Cayenne.intPKForObject(object));
    }

    public void testPKForObject() throws Exception {
        createTestData("testIntPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(new Long(33001), Cayenne.pkForObject(object));
    }

    public void testIntPKForObjectNonNumeric() throws Exception {
        createTestData("testIntPKForObjectNonNumeric");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CharPkTestEntity.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals("CPK", Cayenne.pkForObject(object));
    }
}
