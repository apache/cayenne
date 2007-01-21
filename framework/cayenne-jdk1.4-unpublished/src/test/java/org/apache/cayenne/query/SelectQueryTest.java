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

package org.apache.cayenne.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.ArtistExhibit;
import org.apache.art.Exhibit;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

public class SelectQueryTest extends SelectQueryBase {

    private static final int _artistCount = 20;

    public void testFetchLimit() throws Exception {
        query.setRoot(Artist.class);
        query.setFetchLimit(7);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(7, objects.size());
    }
    
    public void testFetchLimitWithQualifier() throws Exception {
        query.setRoot(Artist.class);
        query.setQualifier(Expression.fromString("db:ARTIST_ID > 3"));
        query.setFetchLimit(7);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(7, objects.size());
    }

    public void testSelectAllObjectsRootEntityName() throws Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootClass() throws Exception {
        query.setRoot(Artist.class);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectAllObjectsRootObjEntity() throws Exception {
        query.setRoot(this.getDomain().getEntityResolver().lookupObjEntity(Artist.class));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectLikeExactMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(1, objects.size());
    }

    public void testSelectNotLikeSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.notLikeExp("artistName", "artist11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(_artistCount - 1, objects.size());
    }

    public void testSelectNotLikeIgnoreCaseSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.notLikeIgnoreCaseExp(
                "artistName",
                "aRtIsT11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(_artistCount - 1, objects.size());
    }

    public void testSelectLikeCaseSensitive() throws Exception {
        if (!getAccessStackAdapter().supportsCaseSensitiveLike()) {
            return;
        }

        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "aRtIsT%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(0, objects.size());
    }

    public void testSelectLikeSingleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist11%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(1, objects.size());
    }

    public void testSelectLikeMultipleWildcardMatch() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(11, objects.size());
    }

    /** Test how "like ignore case" works when using uppercase parameter. */
    public void testSelectLikeIgnoreCaseObjects1() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "ARTIST%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    /** Test how "like ignore case" works when using lowercase parameter. */
    public void testSelectLikeIgnoreCaseObjects2() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "artist%");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertNotNull(objects);
        assertEquals(_artistCount, objects.size());
    }

    public void testSelectIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName in ('artist1', 'artist2')");
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(2, objects.size());
    }

    public void testSelectParameterizedIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap("list", new Object[] {
                "artist1", "artist2"
        }));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(2, objects.size());
    }

    public void testSelectParameterizedEmptyIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap(
                "list",
                new Object[] {}));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(0, objects.size());
    }
    
    public void testSelectParameterizedEmptyNotIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = Expression.fromString("artistName not in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap(
                "list",
                new Object[] {}));
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(20, objects.size());
    }
    
    public void testSelectEmptyIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.inExp("artistName", new Object[]{});
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(0, objects.size());
    }
    
    public void testSelectEmptyNotIn() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.notInExp("artistName", new Object[]{});
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(20, objects.size());
    }
    
    public void testSelectCustAttributes() throws Exception {
        query.setRoot(Artist.class);
        query.addCustomDbAttribute("ARTIST_NAME");

        List results = createDataContext().performQuery(query);

        // check query results
        assertEquals(_artistCount, results.size());

        Map row = (Map) results.get(0);
        assertNotNull(row.get("ARTIST_NAME"));
        assertEquals(1, row.size());
    }
    
    public void testSelectBooleanTrue() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.expTrue();
        qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(1, objects.size());
    }

    public void testSelectBooleanNotTrueOr() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.expTrue();
        qual = qual.notExp();
        qual = qual.orExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(1, objects.size());
    }
    
    public void testSelectBooleanFalse() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.expFalse();
        qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(0, objects.size());
    }

    public void testSelectBooleanFalseOr() throws Exception {
        query.setRoot(Artist.class);
        Expression qual = ExpressionFactory.expFalse();
        qual = qual.orExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        performQuery();

        // check query results
        List objects = opObserver.rowsForQuery(query);
        assertEquals(1, objects.size());
    }
    
    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario.
     */
    public void testRouteWithPrefetches() {
        EntityResolver resolver = getDomain().getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();

        SelectQuery q = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                "artistName",
                "a"));

        q.route(router, resolver, null);
        assertEquals(1, router.getQueryCount());

        q.addPrefetch("paintingArray");
        router.reset();
        q.route(router, resolver, null);
        assertEquals(2, router.getQueryCount());

        q.addPrefetch("paintingArray.toGallery");
        router.reset();
        q.route(router, resolver, null);
        assertEquals(3, router.getQueryCount());

        q.addPrefetch("artistExhibitArray.toExhibit");
        router.reset();
        q.route(router, resolver, null);
        assertEquals(4, router.getQueryCount());

        q.removePrefetch("paintingArray");
        router.reset();
        q.route(router, resolver, null);
        assertEquals(3, router.getQueryCount());
    }

    /**
     * Tests that all queries specified in prefetch are executed in a more complex
     * prefetch scenario with no reverse obj relationships.
     */
    public void testRouteQueryWithPrefetchesNoReverse() {

        EntityResolver resolver = getDomain().getEntityResolver();
        ObjEntity paintingEntity = resolver.lookupObjEntity(Painting.class);
        ObjEntity galleryEntity = resolver.lookupObjEntity(Gallery.class);
        ObjEntity artistExhibitEntity = resolver.lookupObjEntity(ArtistExhibit.class);
        ObjEntity exhibitEntity = resolver.lookupObjEntity(Exhibit.class);
        ObjRelationship paintingToArtistRel = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        ObjRelationship galleryToPaintingRel = (ObjRelationship) galleryEntity
                .getRelationship("paintingArray");
        galleryEntity.removeRelationship("paintingArray");

        ObjRelationship artistExhibitToArtistRel = (ObjRelationship) artistExhibitEntity
                .getRelationship("toArtist");
        artistExhibitEntity.removeRelationship("toArtist");

        ObjRelationship exhibitToArtistExhibitRel = (ObjRelationship) exhibitEntity
                .getRelationship("artistExhibitArray");
        exhibitEntity.removeRelationship("artistExhibitArray");

        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery q = new SelectQuery("Artist", e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        try {
            MockQueryRouter router = new MockQueryRouter();
            q.route(router, resolver, null);
            assertEquals(4, router.getQueryCount());
        }
        finally {
            paintingEntity.addRelationship(paintingToArtistRel);
            galleryEntity.addRelationship(galleryToPaintingRel);
            artistExhibitEntity.addRelationship(artistExhibitToArtistRel);
            exhibitEntity.addRelationship(exhibitToArtistExhibitRel);
        }
    }

    /**
     * Test prefetching with qualifier on the root query being the path to the prefetch.
     */
    public void testRouteQueryWithPrefetchesPrefetchExpressionPath() {

        // find the painting not matching the artist (this is the case where such prefetch
        // at least makes sense)
        Expression exp = ExpressionFactory.noMatchExp("toArtist", new Object());

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        // test how prefetches are resolved in this case - this was a stumbling block for
        // a while
        EntityResolver resolver = getDomain().getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();
        q.route(router, resolver, null);
        assertEquals(2, router.getQueryCount());
    }

    protected void populateTables() throws java.lang.Exception {
        String insertArtist = "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (?,?,?)";
        Connection conn = getConnection();

        try {
            conn.setAutoCommit(false);

            PreparedStatement stmt = conn.prepareStatement(insertArtist);
            long dateBase = System.currentTimeMillis();

            for (int i = 1; i <= _artistCount; i++) {
                stmt.setInt(1, i);
                stmt.setString(2, "artist" + i);
                stmt.setDate(3, new java.sql.Date(dateBase + 1000 * 60 * 60 * 24 * i));
                stmt.executeUpdate();
            }

            stmt.close();
            conn.commit();
        }
        finally {
            conn.close();
        }
    }
}
