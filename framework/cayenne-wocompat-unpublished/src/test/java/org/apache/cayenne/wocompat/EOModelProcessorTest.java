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

package org.apache.cayenne.wocompat;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.XMLEncoder;

public class EOModelProcessorTest extends TestCase {

    protected EOModelProcessor processor;

    @Override
    public void setUp() throws Exception {
        processor = new EOModelProcessor();
    }

    public void testLoadModel() throws Exception {
        URL url = getClass().getClassLoader().getResource("wotests/art.eomodeld/");
        assertNotNull(url);

        
        DataMap map = processor.loadEOModel(url);
        assertLoaded("art", map);
        assertLoadedQueries(map);
        assertOneWayRelationships(map);
        assertLoadedCustomTypes(map);
    }

    public void testLoadModelWithDependencies() throws Exception {
        URL url = getClass().getClassLoader().getResource(
                "wotests/cross-model-relationships.eomodeld/");
        assertNotNull(url);

        DataMap map = processor.loadEOModel(url);

        ObjEntity entity = map.getObjEntity("CrossModelRelTest");
        assertNotNull(entity);

        ObjAttribute a1 = (ObjAttribute) entity.getAttribute("testAttribute");
        assertNotNull(a1);

        DbAttribute da1 = a1.getDbAttribute();
        assertNotNull(da1);
        assertSame(da1, entity.getDbEntity().getAttribute("TEST_ATTRIBUTE"));

        // for now cross model relationships are simply ignored
        // eventually we must handle those...
        assertEquals(0, entity.getRelationships().size());
        assertEquals(0, entity.getDbEntity().getRelationships().size());
    }
    
    public void testLoadFlattened() throws Exception {
        URL url = getClass().getClassLoader().getResource("wotests/flattened.eomodeld/");
        assertNotNull(url);

        // see CAY-1806
        DataMap map = processor.loadEOModel(url);
        ObjEntity artistE = map.getObjEntity("Artist");
        assertNotNull(artistE);
        assertEquals(2, artistE.getRelationships().size());
        assertNotNull(artistE.getRelationship("exhibitArray"));
        assertNotNull(artistE.getRelationship("artistExhibitArray"));
    }

    public void testLoadBrokenModel() throws Exception {
        URL url = getClass().getClassLoader().getResource("art-with-errors.eomodeld/");
        assertNotNull(url);

        DataMap map = processor.loadEOModel(url);
        assertLoaded("art-with-errors", map);
    }

    protected void assertOneWayRelationships(DataMap map) throws Exception {
        // assert that one way relationships are loaded properly
        // - Db loaded as two-way, obj - as one-way

        ObjEntity exhibitEntity = map.getObjEntity("Exhibit");
        ObjRelationship toTypeObject = (ObjRelationship) exhibitEntity
                .getRelationship("toExhibitType");
        DbRelationship toTypeDB = (DbRelationship) exhibitEntity
                .getDbEntity()
                .getRelationship("toExhibitType");
        assertNotNull(toTypeObject);
        assertNotNull(toTypeDB);
        assertNull(toTypeObject.getReverseRelationship());
        assertNotNull(toTypeDB.getReverseRelationship());
    }

    protected void assertLoadedQueries(DataMap map) throws Exception {

        // queries
        Query query = map.getQuery("ExhibitType_TestQuery");

        assertNotNull(query);
        assertTrue(query instanceof SelectQuery);
        assertTrue(query instanceof EOQuery);
        EOQuery eoQuery = (EOQuery) query;

        assertSame(map.getObjEntity("ExhibitType"), eoQuery.getRoot());

        Collection<?> bindings = eoQuery.getBindingNames();
        assertNotNull(bindings);
        assertEquals(3, bindings.size());
        assertEquals("java.lang.String", eoQuery.bindingClass("x"));
        assertEquals("java.lang.String", eoQuery.bindingClass("y"));
        assertEquals("java.lang.Object", eoQuery.bindingClass("z"));
    }

    protected void assertLoadedCustomTypes(DataMap map) throws Exception {

        // check obj entities
        ObjEntity customTypes = map.getObjEntity("CustomTypes");
        assertNotNull(customTypes);

        ObjAttribute pk = (ObjAttribute) customTypes.getAttribute("pk");
        assertNotNull(pk);
        assertEquals("CustomType1", pk.getType());

        ObjAttribute other = (ObjAttribute) customTypes.getAttribute("other");
        assertNotNull(other);
        assertEquals("CustomType2", other.getType());
    }

    protected void assertLoaded(String mapName, DataMap map) throws Exception {
        assertNotNull(map);
        assertEquals(mapName, map.getName());

        // check obj entities
        ObjEntity artistE = map.getObjEntity("Artist");
        assertNotNull(artistE);
        assertEquals("Artist", artistE.getName());

        // check Db entities
        DbEntity artistDE = map.getDbEntity("ARTIST");
        DbEntity artistDE1 = artistE.getDbEntity();
        assertSame(artistDE, artistDE1);

        // check attributes
        ObjAttribute a1 = (ObjAttribute) artistE.getAttribute("artistName");
        assertNotNull(a1);

        DbAttribute da1 = a1.getDbAttribute();
        assertNotNull(da1);
        assertSame(da1, artistDE.getAttribute("ARTIST_NAME"));

        // check ObjRelationships
        ObjRelationship rel = (ObjRelationship) artistE
                .getRelationship("artistExhibitArray");
        assertNotNull(rel);
        assertEquals(1, rel.getDbRelationships().size());

        // check DbRelationships
        DbRelationship drel = (DbRelationship) artistDE
                .getRelationship("artistExhibitArray");
        assertNotNull(drel);
        assertSame(drel, rel.getDbRelationships().get(0));

        // flattened relationships
        ObjRelationship frel = (ObjRelationship) artistE.getRelationship("exhibitArray");
        assertNotNull(frel);
        assertEquals(2, frel.getDbRelationships().size());

        // storing data map may uncover some inconsistencies
        PrintWriter mockupWriter = new NullPrintWriter();
        map.encodeAsXML(new XMLEncoder(mockupWriter));
    }

    class NullPrintWriter extends PrintWriter {

        public NullPrintWriter() {
            super(System.out);
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void write(char[] arg0, int arg1, int arg2) {
        }

        @Override
        public void write(char[] arg0) {
        }

        @Override
        public void write(int arg0) {
        }

        @Override
        public void write(String arg0, int arg1, int arg2) {
        }

        @Override
        public void write(String arg0) {
        }
    }
}
