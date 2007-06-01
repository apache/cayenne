/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.util.List;

import junit.framework.TestCase;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.xml.sax.InputSource;

public class MapLoaderLoadTst extends TestCase {
    protected MapLoader mapLoader;
    private String testDataMap;

    public void setUp() throws Exception {
        super.setUp();

        mapLoader = new MapLoader();
        testDataMap =
            ClassLoader
                .getSystemResource("test-resources/testmap.map.xml")
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

        //test super class name
        //We expect the artist entity to have a super class name... test map should be set up in that way.
        //No other assertions can be made (the actual super class may change)
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
        assertEquals(1, queryWithPrefetch.getPrefetches().size());
        assertEquals(
            Gallery.PAINTING_ARRAY_PROPERTY,
            queryWithPrefetch.getPrefetches().iterator().next());
    }
}
