/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.wocompat;

import java.io.PrintWriter;
import java.util.Collection;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.BasicTestCase;

public class EOModelProcessorTst extends BasicTestCase {

    protected EOModelProcessor processor;

    public void setUp() throws Exception {
        processor = new EOModelProcessor();
    }

    public void testLoadModel() throws Exception {
        DataMap map = processor.loadEOModel("wotests/art.eomodeld");
        assertLoaded("art", map);
        assertLoadedQueries(map);
        assertOneWayRelationships(map);
        assertLoadedCustomTypes(map);
    }

    public void testLoadModelWithDependencies() throws Exception {
        DataMap map = processor.loadEOModel("wotests/cross-model-relationships.eomodeld");

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

    public void testLoadBrokenModel() throws Exception {
        DataMap map = processor.loadEOModel("art-with-errors.eomodeld");
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

        Collection bindings = eoQuery.getBindingNames();
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
        map.encodeAsXML(mockupWriter);
    }

    class NullPrintWriter extends PrintWriter {

        public NullPrintWriter() {
            super(System.out);
        }

        public void close() {
        }

        public void flush() {
        }

        public void write(char[] arg0, int arg1, int arg2) {
        }

        public void write(char[] arg0) {
        }

        public void write(int arg0) {
        }

        public void write(String arg0, int arg1, int arg2) {
        }

        public void write(String arg0) {
        }
    }
}