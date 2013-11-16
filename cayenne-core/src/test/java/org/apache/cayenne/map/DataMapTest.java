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

package org.apache.cayenne.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.query.AbstractQuery;
import org.apache.cayenne.query.MockAbstractQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.NamedObjectFactory;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.xml.sax.InputSource;

/**
 * DataMap unit tests.
 */
public class DataMapTest extends TestCase {

    public void testSerializability() throws Exception {
        DataMap m1 = new DataMap("abc");
        DataMap d1 = (DataMap) Util.cloneViaSerialization(m1);
        assertEquals(m1.getName(), d1.getName());

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        DataMap d2 = (DataMap) Util.cloneViaSerialization(m1);
        assertNotNull(d2.getObjEntity(oe1.getName()));
    }

    public void testInitWithProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(DataMap.CLIENT_SUPPORTED_PROPERTY, "true");
        properties.put(DataMap.DEFAULT_CLIENT_PACKAGE_PROPERTY, "aaaaa");

        DataMap map = new DataMap();
        map.initWithProperties(properties);

        assertTrue(map.isClientSupported());
        assertEquals("aaaaa", map.getDefaultClientPackage());

        // TODO: test other defaults
    }

    public void testDefaultSchema() {
        DataMap map = new DataMap();
        String tstSchema = "tst_schema";
        assertNull(map.getDefaultSchema());
        map.setDefaultSchema(tstSchema);
        assertEquals(tstSchema, map.getDefaultSchema());

        map.setDefaultSchema(null);
        assertNull(map.getDefaultSchema());
    }

    public void testDefaultClientPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultClientPackage());
        map.setDefaultClientPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultClientPackage());

        map.setDefaultClientPackage(null);
        assertNull(map.getDefaultClientPackage());
    }

    public void testDefaultClientSuperclass() {
        DataMap map = new DataMap();
        String tstSuperclass = "tst_superclass";
        assertNull(map.getDefaultClientSuperclass());
        map.setDefaultClientSuperclass(tstSuperclass);
        assertEquals(tstSuperclass, map.getDefaultClientSuperclass());

        map.setDefaultClientSuperclass(null);
        assertNull(map.getDefaultClientSuperclass());
    }

    public void testDefaultPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultPackage());
        map.setDefaultPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultPackage());

        map.setDefaultPackage(null);
        assertNull(map.getDefaultPackage());
    }

    public void testDefaultSuperclass() {
        DataMap map = new DataMap();
        String tstSuperclass = "tst_superclass";
        assertNull(map.getDefaultSuperclass());
        map.setDefaultSuperclass(tstSuperclass);
        assertEquals(tstSuperclass, map.getDefaultSuperclass());

        map.setDefaultSuperclass(null);
        assertNull(map.getDefaultSuperclass());
    }

    public void testDefaultLockType() {
        DataMap map = new DataMap();
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
        map.setDefaultLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, map.getDefaultLockType());

        map.setDefaultLockType(ObjEntity.LOCK_TYPE_NONE);
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
    }

    public void testName() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        map.setName(tstName);
        assertEquals(tstName, map.getName());
    }

    public void testLocation() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        assertNull(map.getLocation());
        map.setLocation(tstName);
        assertEquals(tstName, map.getLocation());
    }

    public void testAddObjEntity() {
        DataMap map = new DataMap();
        ObjEntity e = new ObjEntity("b");
        e.setClassName("b");
        map.addObjEntity(e);
        assertSame(e, map.getObjEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    public void testAddEntityWithSameName() {
        DataMap map = new DataMap();

        // Give them different class-names... we are only testing for the same
        // entity name
        // being a problem
        ObjEntity e1 = new ObjEntity("c");
        e1.setClassName("c1");
        ObjEntity e2 = new ObjEntity("c");
        e2.setClassName("c2");
        map.addObjEntity(e1);
        try {
            map.addObjEntity(e2);
            fail("Should not be able to add more than one entity with the same name");
        } catch (Exception e) {
        }
    }

    public void testRemoveThenAddNullClassName() {
        DataMap map = new DataMap();
        // It should be possible to cleanly remove and then add the same entity
        // again.
        // Uncovered the need for this while testing modeller manually.

        ObjEntity e = new ObjEntity("f");
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    public void testRemoveObjEntity() {
        // make sure deleting an ObjEntity & other entity's relationships to it
        // works & does not cause a ConcurrentModificationException

        ObjEntity e1 = new ObjEntity("1");
        ObjEntity e2 = new ObjEntity("2");

        ObjRelationship r1 = new ObjRelationship("r1");
        r1.setTargetEntityName("2");

        ObjRelationship r2 = new ObjRelationship("r2");
        r2.setTargetEntityName("1");

        ObjRelationship r3 = new ObjRelationship("r3");
        r1.setTargetEntityName("2");

        ObjRelationship r4 = new ObjRelationship("r4");
        r4.setTargetEntityName("1");

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e2.addRelationship(r3);
        e2.addRelationship(r4);

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);

        map.removeObjEntity("1", true);
        assertNull(map.getObjEntity("1"));
        assertEquals(1, e2.getRelationships().size());

        map.removeObjEntity("2", true);
        assertNull(map.getObjEntity("2"));
    }

    public void testMultipleNullClassNames() {
        // Now possible to have more than one objEntity with a null class name.
        // This test proves it

        ObjEntity e1 = new ObjEntity("g");
        ObjEntity e2 = new ObjEntity("h");

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);
    }

    public void testRemoveThenAddRealClassName() {
        ObjEntity e = new ObjEntity("f");
        e.setClassName("f");

        DataMap map = new DataMap();
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    public void testAddEmbeddable() {
        Embeddable e = new Embeddable("XYZ");

        DataMap map = new DataMap();
        assertEquals(0, map.getEmbeddables().size());
        map.addEmbeddable(e);
        assertEquals(1, map.getEmbeddables().size());
        assertTrue(map.getEmbeddables().contains(e));
    }

    public void testRemoveEmbeddable() {
        Embeddable e = new Embeddable("XYZ");

        DataMap map = new DataMap();
        map.addEmbeddable(e);
        assertTrue(map.getEmbeddables().contains(e));

        map.removeEmbeddable("123");
        assertTrue(map.getEmbeddables().contains(e));
        map.removeEmbeddable("XYZ");
        assertFalse(map.getEmbeddables().contains(e));
    }

    public void testAddDbEntity() {
        DbEntity e = new DbEntity("b");

        DataMap map = new DataMap();
        map.addDbEntity(e);
        assertSame(e, map.getDbEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    public void testAddQuery() {
        AbstractQuery q = new MockAbstractQuery("a");
        DataMap map = new DataMap();
        map.addQuery(q);
        assertSame(q, map.getQuery("a"));
    }

    public void testRemoveQuery() {
        AbstractQuery q = new MockAbstractQuery("a");

        DataMap map = new DataMap();
        map.addQuery(q);
        assertSame(q, map.getQuery("a"));
        map.removeQuery("a");
        assertNull(map.getQuery("a"));
    }

    public void testGetQueryMap() {
        AbstractQuery q = new MockAbstractQuery("a");
        DataMap map = new DataMap();
        map.addQuery(q);
        Map<String, Query> queries = map.getQueryMap();
        assertEquals(1, queries.size());
        assertSame(q, queries.get("a"));
    }

    // make sure deleting a DbEntity & other entity's relationships to it
    // works & does not cause a ConcurrentModificationException
    public void testRemoveDbEntity() {

        DataMap map = new DataMap();

        // create a twisty maze of intermingled relationships.
        DbEntity e1 = NamedObjectFactory.createObject(DbEntity.class, map);
        e1.setName("e1");

        DbEntity e2 = NamedObjectFactory.createObject(DbEntity.class, map);
        e2.setName("e2");

        DbRelationship r1 = NamedObjectFactory.createObject(DbRelationship.class, e1);
        r1.setName("r1");
        r1.setTargetEntity(e2);

        DbRelationship r2 = NamedObjectFactory.createObject(DbRelationship.class, e2);
        r2.setName("r2");
        r2.setTargetEntity(e1);

        DbRelationship r3 = NamedObjectFactory.createObject(DbRelationship.class, e1);
        r3.setName("r3");
        r3.setTargetEntity(e2);

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e1.addRelationship(r3);

        e2.addRelationship(r1);
        e2.addRelationship(r2);
        e2.addRelationship(r3);

        map.addDbEntity(e1);
        map.addDbEntity(e2);

        // now actually test something
        map.removeDbEntity(e1.getName(), true);
        assertNull(map.getDbEntity(e1.getName()));
        map.removeDbEntity(e2.getName(), true);
        assertNull(map.getDbEntity(e2.getName()));
    }

    public void testChildProcedures() throws Exception {
        DataMap map = new DataMap();
        checkProcedures(map, new String[0]);

        map.addProcedure(new Procedure("proc1"));
        checkProcedures(map, new String[] { "proc1" });

        map.addProcedure(new Procedure("proc2"));
        checkProcedures(map, new String[] { "proc1", "proc2" });

        map.removeProcedure("proc2");
        checkProcedures(map, new String[] { "proc1" });
    }

    protected void checkProcedures(DataMap map, String[] expectedNames) throws Exception {
        int len = expectedNames.length;
        Map<String, Procedure> proceduresMap = map.getProcedureMap();
        Collection<Procedure> proceduresCollection = map.getProcedures();

        assertNotNull(proceduresMap);
        assertEquals(len, proceduresMap.size());
        assertNotNull(proceduresCollection);
        assertEquals(len, proceduresCollection.size());

        for (int i = 0; i < len; i++) {
            Procedure proc = map.getProcedure(expectedNames[i]);
            assertNotNull(proc);
            assertEquals(expectedNames[i], proc.getName());
        }
    }

    public void testQuoteSqlIdentifiersEncodeAsXML() {
        DataMap map = new DataMap("aaa");
        map.setQuotingSQLIdentifiers(true);
        StringWriter w = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(w));
        map.encodeAsXML(encoder);

        assertTrue(map.quotingSQLIdentifiers);

        MapLoader loader = new MapLoader();
        try {
            InputStream is = new ByteArrayInputStream(w.getBuffer().toString().getBytes("UTF-8"));
            DataMap newMap = loader.loadDataMap(new InputSource(is));
            assertTrue(newMap.quotingSQLIdentifiers);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        map.setQuotingSQLIdentifiers(false);
        StringWriter w2 = new StringWriter();
        XMLEncoder encoder2 = new XMLEncoder(new PrintWriter(w2));
        map.encodeAsXML(encoder2);

        assertFalse(map.quotingSQLIdentifiers);
        try {
            InputStream is = new ByteArrayInputStream(w2.getBuffer().toString().getBytes("UTF-8"));
            DataMap newMap = loader.loadDataMap(new InputSource(is));
            assertFalse(newMap.quotingSQLIdentifiers);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
}
