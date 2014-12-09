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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SelectQueryIT extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    protected void createArtistsDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH");

        long dateBase = System.currentTimeMillis();

        for (int i = 1; i <= 20; i++) {
            tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
        }
    }

    protected void createArtistsWildcardDataSet() throws Exception {
        TableHelper tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tArtist.insert(1, "_X");
        tArtist.insert(2, "Y_");
    }

    @Test
    public void testSetQualifier() {
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        assertNull(query.getQualifier());

        Expression qual = ExpressionFactory.expressionOfType(Expression.AND);
        query.setQualifier(qual);
        assertNotNull(query.getQualifier());
        assertSame(qual, query.getQualifier());
    }

    @Test
    public void testAndQualifier() {
    	SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        assertNull(query.getQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        query.andQualifier(e1);
        assertSame(e1, query.getQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        query.andQualifier(e2);
        assertEquals(Expression.AND, query.getQualifier().getType());
    }

    @Test
    public void testOrQualifier() {
    	SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        assertNull(query.getQualifier());

        Expression e1 = ExpressionFactory.expressionOfType(Expression.EQUAL_TO);
        query.orQualifier(e1);
        assertSame(e1, query.getQualifier());

        Expression e2 = ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO);
        query.orQualifier(e2);
        assertEquals(Expression.OR, query.getQualifier().getType());
    }

    @Test
    public void testFetchLimit() throws Exception {
        createArtistsDataSet();

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.setFetchLimit(7);

        List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertEquals(7, objects.size());
    }

    @Test
    public void testFetchOffset() throws Exception {

        createArtistsDataSet();

        int totalRows = context.select(new SelectQuery<Artist>(Artist.class)).size();

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.addOrdering("db:" + Artist.ARTIST_ID_PK_COLUMN, SortOrder.ASCENDING);
        query.setFetchOffset(5);
        List<Artist> results = context.select(query);

        assertEquals(totalRows - 5, results.size());
        assertEquals("artist6", results.get(0).getArtistName());
    }

    @Test
    public void testDbEntityRoot() throws Exception {

        createArtistsDataSet();
        DbEntity artistDbEntity = context.getEntityResolver().getDbEntity("ARTIST");

        SelectQuery query = new SelectQuery(artistDbEntity);
        List<?> results = context.performQuery(query);

        assertEquals(20, results.size());
        assertTrue(results.get(0) instanceof DataRow);
    }

    @Test
    public void testFetchLimitWithOffset() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.addOrdering("db:" + Artist.ARTIST_ID_PK_COLUMN, SortOrder.ASCENDING);
        query.setFetchOffset(15);
        query.setFetchLimit(4);
        List<Artist> results = context.select(query);

        assertEquals(4, results.size());
        assertEquals("artist16", results.get(0).getArtistName());
    }

    @Test
    public void testFetchOffsetWithQualifier() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.setQualifier(Expression.fromString("db:ARTIST_ID > 3"));
        query.setFetchOffset(5);

        List<?> objects = context.performQuery(query);
        int size = objects.size();

        SelectQuery<Artist> sizeQ = new SelectQuery<Artist>(Artist.class);
        sizeQ.setQualifier(Expression.fromString("db:ARTIST_ID > 3"));
        List<?> objects1 = context.performQuery(sizeQ);
        int sizeAll = objects1.size();
        assertEquals(size, sizeAll - 5);
    }

    @Test
    public void testFetchLimitWithQualifier() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.setQualifier(Expression.fromString("db:ARTIST_ID > 3"));
        query.setFetchLimit(7);
        List<?> objects = context.performQuery(query);
        assertEquals(7, objects.size());
    }

    @Test
    public void testSelectAllObjectsRootEntityName() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>("Artist");
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectAllObjectsRootClass() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectAllObjectsRootObjEntity() throws Exception {
        createArtistsDataSet();
        ObjEntity artistEntity = context.getEntityResolver().getObjEntity(Artist.class);
        SelectQuery<Artist> query = new SelectQuery<Artist>(artistEntity);

        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectLikeExactMatch() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }

    @Test
    public void testSelectNotLikeSingleWildcardMatch() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.notLikeExp("artistName", "artist11%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(19, objects.size());
    }

    @Test
    public void testSelectNotLikeIgnoreCaseSingleWildcardMatch() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.notLikeIgnoreCaseExp("artistName", "aRtIsT11%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(19, objects.size());
    }

    @Test
    public void testSelectLikeCaseSensitive() throws Exception {
        if (!accessStackAdapter.supportsCaseSensitiveLike()) {
            return;
        }

        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "aRtIsT%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }

    @Test
    public void testSelectLikeSingleWildcardMatch() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist11%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }

    @Test
    public void testSelectLikeSingleWildcardMatchAndEscape() throws Exception {

        createArtistsWildcardDataSet();

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        query.andQualifier(ExpressionFactory.likeExp("artistName", "=_%", '='));

        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }
    
	@Test
	public void testSelectLikeSingleWildcardMatchAndEscape_AndOtherCriteria() throws Exception {

		createArtistsWildcardDataSet();

		// CAY-1978 - combining LIKE..ESCAPE with another clause generated bad
		// syntax
		SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
		query.andQualifier(ExpressionFactory.likeExp("artistName", "=_%", '='));
		query.andQualifier(Artist.ARTIST_NAME.eq("_X"));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}
	
	@Test
	public void testSelectLikeSingleWildcardMatchIgnoreCaseAndEscape_AndOtherCriteria() throws Exception {

		createArtistsWildcardDataSet();

		// CAY-1978 - combining LIKE..ESCAPE with another clause generated bad SQL
		SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
		query.andQualifier(ExpressionFactory.likeIgnoreCaseExp("artistName", "=_%", '='));
		query.andQualifier(Artist.ARTIST_NAME.eq("_X"));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

    @Test
    public void testSelectLikeMultipleWildcardMatch() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "artist1%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(11, objects.size());
    }

    /**
     * Test how "like ignore case" works when using uppercase parameter.
     */
    @Test
    public void testSelectLikeIgnoreCaseObjects1() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "ARTIST%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    /** Test how "like ignore case" works when using lowercase parameter. */
    @Test
    public void testSelectLikeIgnoreCaseObjects2() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);
        Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "artist%");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = Expression.fromString("artistName in ('artist1', 'artist2')");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
    }

    @Test
    public void testSelectParameterizedIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = Expression.fromString("artistName in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap("list", new Object[] { "artist1", "artist2" }));
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());
    }

    @Test
    public void testSelectParameterizedEmptyIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = Expression.fromString("artistName in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap("list", new Object[] {}));
        List<?> objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }

    @Test
    public void testSelectParameterizedEmptyNotIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = Expression.fromString("artistName not in $list");
        query.setQualifier(qual);
        query = query.queryWithParameters(Collections.singletonMap("list", new Object[] {}));
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectEmptyIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.inExp("artistName");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }

    @Test
    public void testSelectEmptyNotIn() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.notInExp("artistName");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(20, objects.size());
    }

    @Test
    public void testSelectBooleanTrue() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.expTrue();
        qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }

    @Test
    public void testSelectBooleanNotTrueOr() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.expTrue();
        qual = qual.notExp();
        qual = qual.orExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }

    @Test
    public void testSelectBooleanFalse() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.expFalse();
        qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }

    @Test
    public void testSelectBooleanFalseOr() throws Exception {
        createArtistsDataSet();
        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.expFalse();
        qual = qual.orExp(ExpressionFactory.matchExp("artistName", "artist1"));
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
    }

    /**
     * Tests that all queries specified in prefetch are executed in a more
     * complex prefetch scenario.
     */
    @Test
    public void testRouteWithPrefetches() {
        EntityResolver resolver = context.getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();

        SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class, ExpressionFactory.matchExp("artistName", "a"));

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
     * Tests that all queries specified in prefetch are executed in a more
     * complex prefetch scenario with no reverse obj relationships.
     */
    @Test
    public void testRouteQueryWithPrefetchesNoReverse() {

        EntityResolver resolver = context.getEntityResolver();
        ObjEntity paintingEntity = resolver.getObjEntity(Painting.class);
        ObjEntity galleryEntity = resolver.getObjEntity(Gallery.class);
        ObjEntity artistExhibitEntity = resolver.getObjEntity(ArtistExhibit.class);
        ObjEntity exhibitEntity = resolver.getObjEntity(Exhibit.class);
        ObjRelationship paintingToArtistRel = paintingEntity.getRelationship("toArtist");
        paintingEntity.removeRelationship("toArtist");

        ObjRelationship galleryToPaintingRel = galleryEntity.getRelationship("paintingArray");
        galleryEntity.removeRelationship("paintingArray");

        ObjRelationship artistExhibitToArtistRel = artistExhibitEntity.getRelationship("toArtist");
        artistExhibitEntity.removeRelationship("toArtist");

        ObjRelationship exhibitToArtistExhibitRel = exhibitEntity.getRelationship("artistExhibitArray");
        exhibitEntity.removeRelationship("artistExhibitArray");

        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery<Artist> q = new SelectQuery<Artist>(Artist.class, e);
        q.addPrefetch("paintingArray");
        q.addPrefetch("paintingArray.toGallery");
        q.addPrefetch("artistExhibitArray.toExhibit");

        try {
            MockQueryRouter router = new MockQueryRouter();
            q.route(router, resolver, null);
            assertEquals(4, router.getQueryCount());
        } finally {
            paintingEntity.addRelationship(paintingToArtistRel);
            galleryEntity.addRelationship(galleryToPaintingRel);
            artistExhibitEntity.addRelationship(artistExhibitToArtistRel);
            exhibitEntity.addRelationship(exhibitToArtistExhibitRel);
        }
    }

    /**
     * Test prefetching with qualifier on the root query being the path to the
     * prefetch.
     */
    @Test
    public void testRouteQueryWithPrefetchesPrefetchExpressionPath() {

        // find the painting not matching the artist (this is the case where
        // such prefetch
        // at least makes sense)
        Expression exp = ExpressionFactory.noMatchExp("toArtist", new Object());

        SelectQuery q = new SelectQuery(Painting.class, exp);
        q.addPrefetch("toArtist");

        // test how prefetches are resolved in this case - this was a stumbling
        // block for
        // a while
        EntityResolver resolver = context.getEntityResolver();
        MockQueryRouter router = new MockQueryRouter();
        q.route(router, resolver, null);
        assertEquals(2, router.getQueryCount());
    }

    @Test
    public void testLeftJoinAndPrefetchToMany() throws Exception {
        createArtistsDataSet();
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class, ExpressionFactory.matchExp(
                "paintingArray+.toGallery", null));
        query.addPrefetch("artistExhibitArray");
        context.performQuery(query);
    }

    @Test
    public void testLeftJoinAndPrefetchToOne() throws Exception {
        createArtistsDataSet();
        SelectQuery<Painting> query = new SelectQuery<Painting>(Painting.class, ExpressionFactory.matchExp(
                "toArtist+.artistName", null));
        query.addPrefetch("toGallery");
        context.select(query);
    }

    @Test
    public void testSelect_MatchObject() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");
        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("a3");
        context.commitChanges();

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);

        query.setQualifier(ExpressionFactory.matchExp(a2));
        Object res = Cayenne.objectForQuery(context, query);// exception if >1
                                                            // result
        assertSame(res, a2);
        assertTrue(query.getQualifier().match(res));

        query.setQualifier(ExpressionFactory.matchAnyExp(a1, a3));
        query.addOrdering("artistName", SortOrder.ASCENDING);
        List<Artist> list = context.select(query);
        assertEquals(list.size(), 2);
        assertSame(list.get(0), a1);
        assertSame(list.get(1), a3);
        assertTrue(query.getQualifier().match(a1));
        assertTrue(query.getQualifier().match(a3));

        assertEquals(query.getQualifier(), ExpressionFactory.matchAnyExp(Arrays.asList(a1, a3)));
    }

    @Test
    public void testSelect_WithOrdering() {

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");
        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("a3");
        context.commitChanges();

        List<Ordering> orderings = Arrays.asList(new Ordering("artistName", SortOrder.ASCENDING));
        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class, null, orderings);

        List<Artist> list = context.select(query);
        assertEquals(list.size(), 3);
        assertSame(list.get(0), a1);
        assertSame(list.get(1), a2);
        assertSame(list.get(2), a3);
    }

    /**
     * Tests INs with more than 1000 elements
     */
    @Test
    public void testSelectLongIn() {
        // not all adapters strip INs, so we just make sure query with such
        // qualifier
        // fires OK
        Object[] numbers = new String[2009];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = "" + i;
        }

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class,
                ExpressionFactory.inExp("artistName", numbers));
        context.performQuery(query);
    }

    @Test
    public void testCacheOffsetAndLimit() throws Exception {
        createArtistsDataSet();

        SelectQuery<Artist> query1 = new SelectQuery<Artist>(Artist.class);
        query1.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query1.setFetchOffset(0);
        query1.setFetchLimit(10);
        context.performQuery(query1);

        SelectQuery<Artist> query2 = new SelectQuery<Artist>(Artist.class);
        query2.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query2.setFetchOffset(10);
        query2.setFetchLimit(10);
        context.performQuery(query2);

        SelectQuery<Artist> query3 = new SelectQuery<Artist>(Artist.class);
        query3.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query3.setFetchOffset(10);
        query3.setFetchLimit(10);
        context.performQuery(query3);

        assertFalse(query1.metaData.getCacheKey().equals(query2.metaData.cacheKey));
        assertEquals(query2.metaData.getCacheKey(), query3.metaData.getCacheKey());
    }
}
