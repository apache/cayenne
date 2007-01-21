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

import java.util.List;

import junit.framework.TestCase;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.xml.sax.InputSource;

public class MapLoaderLoadTest extends TestCase {

    protected MapLoader mapLoader;
    private String testDataMap;

    public void setUp() throws Exception {
        super.setUp();

        mapLoader = new MapLoader();
        testDataMap = getClass()
                .getClassLoader()
                .getResource("testmap.map.xml")
                .toExternalForm();
    }

    public void testLoadDataMap() throws Exception {
        InputSource in = new InputSource(testDataMap);
        DataMap map = mapLoader.loadDataMap(in);
        assertNotNull(map);

        // test procedures
        Procedure procedure = map.getProcedure("cayenne_tst_upd_proc");
        assertNotNull(procedure);
        List params = procedure.getCallParameters();
        assertNotNull(params);
        assertEquals(1, params.size());
        ProcedureParameter param = (ProcedureParameter) params.get(0);
        assertNotNull(param);
        assertEquals("paintingPrice", param.getName());
        assertEquals(ProcedureParameter.IN_PARAMETER, param.getDirection());

        // test derived entities
        DerivedDbEntity d1 = (DerivedDbEntity) map.getDbEntity("ARTIST_ASSETS");
        assertNotNull(d1);
        assertNotNull(d1.getParentEntity());
        assertEquals(1, d1.getGroupByAttributes().size());

        DerivedDbAttribute a1 = (DerivedDbAttribute) d1.getAttribute("ESTIMATED_PRICE");
        assertNotNull(a1);
        assertNotNull(a1.getExpressionSpec());
        assertNotNull(a1.getParams());
        assertEquals(1, a1.getParams().size());

        // test super class name
        // We expect the artist entity to have a super class name... test map should be
        // set up in that way.
        // No other assertions can be made (the actual super class may change)
        ObjEntity ent = map.getObjEntity("Painting");
        assertNotNull(ent.getSuperClassName());

        checkLoadedQueries(map);
    }

    private void checkLoadedQueries(DataMap map) throws Exception {
        SelectQuery queryWithQualifier = (SelectQuery) map.getQuery("QueryWithQualifier");
        assertNotNull(queryWithQualifier);
        assertTrue(queryWithQualifier.getRoot() instanceof ObjEntity);
        assertEquals("Artist", ((Entity) queryWithQualifier.getRoot()).getName());
        assertNotNull(queryWithQualifier.getQualifier());

        SelectQuery queryWithOrdering = (SelectQuery) map.getQuery("QueryWithOrdering");
        assertNotNull(queryWithOrdering);
        assertTrue(queryWithOrdering.getRoot() instanceof ObjEntity);
        assertEquals("Artist", ((Entity) queryWithOrdering.getRoot()).getName());
        assertEquals(2, queryWithOrdering.getOrderings().size());

        Ordering artistNameOrdering = (Ordering) queryWithOrdering.getOrderings().get(0);
        assertEquals(Artist.ARTIST_NAME_PROPERTY, artistNameOrdering.getSortSpecString());
        assertFalse(artistNameOrdering.isAscending());
        assertTrue(artistNameOrdering.isCaseInsensitive());

        Ordering dobOrdering = (Ordering) queryWithOrdering.getOrderings().get(1);
        assertEquals(Artist.DATE_OF_BIRTH_PROPERTY, dobOrdering.getSortSpecString());
        assertTrue(dobOrdering.isAscending());
        assertFalse(dobOrdering.isCaseInsensitive());

        SelectQuery queryWithPrefetch = (SelectQuery) map.getQuery("QueryWithPrefetch");
        assertNotNull(queryWithPrefetch);
        assertTrue(queryWithPrefetch.getRoot() instanceof ObjEntity);
        assertEquals("Gallery", ((Entity) queryWithPrefetch.getRoot()).getName());
        assertNotNull(queryWithPrefetch.getPrefetchTree());
        assertEquals(1, queryWithPrefetch.getPrefetchTree().nonPhantomNodes().size());
        assertNotNull(queryWithPrefetch.getPrefetchTree().getNode(
                Gallery.PAINTING_ARRAY_PROPERTY));

        SQLTemplate nonSelectingQuery = (SQLTemplate) map.getQuery("NonSelectingQuery");
        assertNotNull(nonSelectingQuery);
        assertEquals("NonSelectingQuery", nonSelectingQuery.getName());
    }
}
