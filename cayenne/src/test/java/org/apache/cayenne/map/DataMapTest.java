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

package org.apache.cayenne.map;

import org.apache.cayenne.configuration.EmptyConfigurationNodeVisitor;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * DataMap unit tests.
 */
public class DataMapTest {

    @Test
    public void testSerializability() throws Exception {
        DataMap m1 = new DataMap("abc");
        DataMap d1 = (DataMap) Util.cloneViaSerialization(m1);
        assertEquals(m1.getName(), d1.getName());

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        DataMap d2 = (DataMap) Util.cloneViaSerialization(m1);
        assertNotNull(d2.getObjEntity(oe1.getName()));
    }

    @Test
    public void testDefaultSchema() {
        DataMap map = new DataMap();
        String tstSchema = "tst_schema";
        assertNull(map.getDefaultSchema());
        map.setDefaultSchema(tstSchema);
        assertEquals(tstSchema, map.getDefaultSchema());

        map.setDefaultSchema(null);
        assertNull(map.getDefaultSchema());
    }

    @Test
    public void testDefaultPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultPackage());
        map.setDefaultPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultPackage());

        map.setDefaultPackage(null);
        assertNull(map.getDefaultPackage());
    }

    @Test
    public void testDefaultSuperclass() {
        DataMap map = new DataMap();
        String tstSuperclass = "tst_superclass";
        assertNull(map.getDefaultSuperclass());
        map.setDefaultSuperclass(tstSuperclass);
        assertEquals(tstSuperclass, map.getDefaultSuperclass());

        map.setDefaultSuperclass(null);
        assertNull(map.getDefaultSuperclass());
    }

    @Test
    public void testDefaultLockType() {
        DataMap map = new DataMap();
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
        map.setDefaultLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, map.getDefaultLockType());

        map.setDefaultLockType(ObjEntity.LOCK_TYPE_NONE);
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
    }

    @Test
    public void testName() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        map.setName(tstName);
        assertEquals(tstName, map.getName());
    }

    @Test
    public void testLocation() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        assertNull(map.getLocation());
        map.setLocation(tstName);
        assertEquals(tstName, map.getLocation());
    }

    @Test
    public void testAddObjEntity() {
        DataMap map = new DataMap();
        ObjEntity e = new ObjEntity("b");
        e.setClassName("b");
        map.addObjEntity(e);
        assertSame(e, map.getObjEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    @Test
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
        } catch (Exception ignored) {
        }
    }

    @Test
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

    @Test
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

    @Test
    public void testMultipleNullClassNames() {
        // Now possible to have more than one objEntity with a null class name.
        // This test proves it

        ObjEntity e1 = new ObjEntity("g");
        ObjEntity e2 = new ObjEntity("h");

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);
    }

    @Test
    public void testRemoveThenAddRealClassName() {
        ObjEntity e = new ObjEntity("f");
        e.setClassName("f");

        DataMap map = new DataMap();
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    @Test
    public void testAddEmbeddable() {
        Embeddable e = new Embeddable("XYZ");

        DataMap map = new DataMap();
        assertEquals(0, map.getEmbeddables().size());
        map.addEmbeddable(e);
        assertEquals(1, map.getEmbeddables().size());
        assertTrue(map.getEmbeddables().contains(e));
    }

    @Test
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

    @Test
    public void testAddDbEntity() {
        DbEntity e = new DbEntity("b");

        DataMap map = new DataMap();
        map.addDbEntity(e);
        assertSame(e, map.getDbEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    @Test
    public void testAddQueryDescriptor() {
        QueryDescriptor q = QueryDescriptor.selectQueryDescriptor();
        q.setName("a");
        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        assertSame(q, map.getQueryDescriptor("a"));
    }

    @Test
    public void testRemoveQueryDescriptor() {
        QueryDescriptor q = QueryDescriptor.selectQueryDescriptor();
        q.setName("a");

        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        assertSame(q, map.getQueryDescriptor("a"));
        map.removeQueryDescriptor("a");
        assertNull(map.getQueryDescriptor("a"));
    }

    @Test
    public void testGetQueryMap() {
        QueryDescriptor q = QueryDescriptor.selectQueryDescriptor();
        q.setName("a");
        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        Map<String, QueryDescriptor> queries = map.getQueryDescriptorMap();
        assertEquals(1, queries.size());
        assertSame(q, queries.get("a"));
    }

    // make sure deleting a DbEntity & other entity's relationships to it
    // works & does not cause a ConcurrentModificationException
    @Test
    public void testRemoveDbEntity() {

        DataMap map = new DataMap();

        // create a twisty maze of intermingled relationships.
        DbEntity e1 = new DbEntity();
        e1.setName("e1");

        DbEntity e2 = new DbEntity();
        e2.setName("e2");

        DbRelationship r1 = new DbRelationship();
        r1.setName("r1");
        r1.setTargetEntityName(e2);

        DbRelationship r2 = new DbRelationship();
        r2.setName("r2");
        r2.setTargetEntityName(e1);

        DbRelationship r3 = new DbRelationship();
        r3.setName("r3");
        r3.setTargetEntityName(e2);

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

    @Test
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

        for (String expectedName : expectedNames) {
            Procedure proc = map.getProcedure(expectedName);
            assertNotNull(proc);
            assertEquals(expectedName, proc.getName());
        }
    }

    @Ignore("this test is broken for many reasons " +
            "(URL can't be mocked, encoder doesn't provide version, loader requires XMLReader proovider)")
    @Test
    public void testQuoteSqlIdentifiersEncodeAsXML() {
        DataMap map = new DataMap("aaa");
        map.setQuotingSQLIdentifiers(true);
        StringWriter w = new StringWriter();
        XMLEncoder encoder = new XMLEncoder(new PrintWriter(w));
        map.encodeAsXML(encoder, new EmptyConfigurationNodeVisitor());

        assertTrue(map.quotingSQLIdentifiers);

        XMLDataMapLoader loader = new XMLDataMapLoader();
        try {
            URL url = mock(URL.class);
            InputStream is = new ByteArrayInputStream(w.getBuffer().toString().getBytes("UTF-8"));
            when(url.openStream()).thenReturn(is);

            DataMap newMap = loader.load(new URLResource(url));
            assertTrue(newMap.quotingSQLIdentifiers);

        } catch (Exception e) {
            e.printStackTrace();
        }

        map.setQuotingSQLIdentifiers(false);
        StringWriter w2 = new StringWriter();
        XMLEncoder encoder2 = new XMLEncoder(new PrintWriter(w2));
        map.encodeAsXML(encoder2, new EmptyConfigurationNodeVisitor());

        assertFalse(map.quotingSQLIdentifiers);
        try {
            URL url = mock(URL.class);
            InputStream is = new ByteArrayInputStream(w.getBuffer().toString().getBytes("UTF-8"));
            when(url.openStream()).thenReturn(is);

            DataMap newMap = loader.load(new URLResource(url));
            assertFalse(newMap.quotingSQLIdentifiers);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
