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

import org.apache.cayenne.query.*;
import org.apache.cayenne.test.file.FileUtil;
import org.apache.cayenne.testdo.embeddable.Embeddable1;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.util.XMLEncoder;
import org.junit.Test;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MapLoaderLoadTest {

    private InputSource getMapXml(String mapName) {
        return new InputSource(getClass().getClassLoader().getResourceAsStream(mapName));
    }

    @Test
    public void testLoadEmbeddableMap() throws Exception {
        MapLoader mapLoader = new MapLoader();
        DataMap map = mapLoader.loadDataMap(getMapXml("embeddable.map.xml"));
        assertNotNull(map);

        assertEquals(1, map.getEmbeddables().size());
        Embeddable e = map.getEmbeddable(Embeddable1.class.getName());
        assertNotNull(e);
        assertEquals(Embeddable1.class.getName(), e.getClassName());

        assertEquals(2, e.getAttributes().size());
        EmbeddableAttribute ea1 = e.getAttribute("embedded10");
        assertNotNull(ea1);
        assertEquals("embedded10", ea1.getName());
        assertEquals("java.lang.String", ea1.getType());
        assertEquals("EMBEDDED10", ea1.getDbAttributeName());

        EmbeddableAttribute ea2 = e.getAttribute("embedded20");
        assertNotNull(ea2);
        assertEquals("embedded20", ea2.getName());
        assertEquals("java.lang.String", ea2.getType());
        assertEquals("EMBEDDED20", ea2.getDbAttributeName());

        ObjEntity oe = map.getObjEntity("EmbedEntity1");
        assertNotNull(oe);
        assertEquals(3, oe.getDeclaredAttributes().size());

        EmbeddedAttribute oea1 = (EmbeddedAttribute) oe.getAttribute("embedded1");
        assertNotNull(oea1);
        assertEquals(Embeddable1.class.getName(), oea1.getType());
        assertEquals(0, oea1.getAttributeOverrides().size());

        EmbeddedAttribute oea2 = (EmbeddedAttribute) oe.getAttribute("embedded2");
        assertNotNull(oea2);
        assertEquals(Embeddable1.class.getName(), oea2.getType());
        assertEquals(2, oea2.getAttributeOverrides().size());
    }

    @Test
    public void testLoadTestMap() throws Exception {
        MapLoader mapLoader = new MapLoader();
        DataMap map = mapLoader.loadDataMap(getMapXml("testmap.map.xml"));
        assertNotNull(map);

        // test procedures
        Procedure procedure = map.getProcedure("cayenne_tst_upd_proc");
        assertNotNull(procedure);
        List<ProcedureParameter> params = procedure.getCallParameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        ProcedureParameter param = params.get(0);
        assertNotNull(param);
        assertEquals("paintingPrice", param.getName());
        assertEquals(ProcedureParameter.IN_PARAMETER, param.getDirection());

        // test super class name
        // We expect the artist entity to have a super class name... test map should be
        // set up in that way.
        // No other assertions can be made (the actual super class may change)
        ObjEntity ent = map.getObjEntity("Painting");
        assertNotNull(ent.getSuperClassName());

        //text exclude... parameters
        ObjEntity artistCallbackTestEntity = map.getObjEntity("ArtistCallback");
        assertTrue(artistCallbackTestEntity.isExcludingDefaultListeners());
        assertTrue(artistCallbackTestEntity.isExcludingSuperclassListeners());

        checkLoadedQueries(map);
    }

    @Test
    public void testEncodeAsXML() throws FileNotFoundException {
        // load map
        MapLoader mapLoader = new MapLoader();
        DataMap map = mapLoader.loadDataMap(getMapXml("testmap.map.xml"));
        assertNotNull(map);

        // encode map
        File file = new File(FileUtil.baseTestDirectory(), "testmap_generated.map.xml");
        PrintWriter pw = new PrintWriter(new FileOutputStream(file));
        map.encodeAsXML(new XMLEncoder(pw));
        pw.close();
    }

    private void checkLoadedQueries(DataMap map) throws Exception {
        SelectQueryDescriptor queryWithQualifier = (SelectQueryDescriptor) map.getQueryDescriptor("QueryWithQualifier");
        assertNotNull(queryWithQualifier);
        assertTrue(queryWithQualifier.getRoot() instanceof ObjEntity);
        assertEquals("Artist", ((Entity) queryWithQualifier.getRoot()).getName());
        assertNotNull(queryWithQualifier.getQualifier());

        SelectQueryDescriptor queryWithOrdering = (SelectQueryDescriptor) map.getQueryDescriptor("QueryWithOrdering");
        assertNotNull(queryWithOrdering);
        assertTrue(queryWithOrdering.getRoot() instanceof ObjEntity);
        assertEquals("Artist", ((Entity) queryWithOrdering.getRoot()).getName());
        assertEquals(2, queryWithOrdering.getOrderings().size());

        Ordering artistNameOrdering = queryWithOrdering.getOrderings().get(0);
        assertEquals(Artist.ARTIST_NAME_PROPERTY, artistNameOrdering.getSortSpecString());
        assertFalse(artistNameOrdering.isAscending());
        assertTrue(artistNameOrdering.isCaseInsensitive());

        Ordering dobOrdering = queryWithOrdering.getOrderings().get(1);
        assertEquals(Artist.DATE_OF_BIRTH_PROPERTY, dobOrdering.getSortSpecString());
        assertTrue(dobOrdering.isAscending());
        assertFalse(dobOrdering.isCaseInsensitive());

        SelectQueryDescriptor queryWithPrefetch = (SelectQueryDescriptor) map.getQueryDescriptor("QueryWithPrefetch");
        assertNotNull(queryWithPrefetch);
        assertTrue(queryWithPrefetch.getRoot() instanceof ObjEntity);
        assertEquals("Gallery", ((Entity) queryWithPrefetch.getRoot()).getName());
        assertNotNull(queryWithPrefetch.getPrefetches());
        assertEquals(1, queryWithPrefetch.getPrefetches().size());
        assertTrue(queryWithPrefetch.getPrefetches().contains(Gallery.PAINTING_ARRAY.getName()));

        QueryDescriptor nonSelectingQuery = map.getQueryDescriptor("NonSelectingQuery");
        assertNotNull(nonSelectingQuery);
        assertEquals("NonSelectingQuery", nonSelectingQuery.getName());
        assertEquals("UPPER", nonSelectingQuery.getProperties().get("cayenne.SQLTemplate.columnNameCapitalization"));
        
        EJBQLQueryDescriptor ejbqlQueryTest = (EJBQLQueryDescriptor) map.getQueryDescriptor("EjbqlQueryTest");
        assertNotNull(ejbqlQueryTest);
        assertEquals("EjbqlQueryTest", ejbqlQueryTest.getName());
        assertNotNull(ejbqlQueryTest.getEjbql());
        assertEquals("SHARED_CACHE", ejbqlQueryTest.getProperties().get("cayenne.GenericSelectQuery.cacheStrategy"));
    }
}
