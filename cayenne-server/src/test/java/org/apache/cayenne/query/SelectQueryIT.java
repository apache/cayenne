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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
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
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SelectQueryIT extends ServerCase {

	@Inject
	private ObjectContext context;

	@Inject
	private DBHelper dbHelper;

	@Inject
	private UnitDbAdapter accessStackAdapter;

	private TableHelper tArtist;
	private TableHelper tPainting;

	@Before
	public void before() {
		this.tArtist = new TableHelper(dbHelper, "ARTIST").setColumns("ARTIST_ID", "ARTIST_NAME", "DATE_OF_BIRTH")
				.setColumnTypes(Types.BIGINT, Types.CHAR, Types.DATE);
		tPainting = new TableHelper(dbHelper, "PAINTING").setColumns("PAINTING_ID", "ARTIST_ID", "PAINTING_TITLE")
				.setColumnTypes(Types.INTEGER, Types.BIGINT, Types.VARCHAR);
	}

	protected void createArtistsDataSet() throws Exception {

		long dateBase = System.currentTimeMillis();

		for (int i = 1; i <= 20; i++) {
			tArtist.insert(i, "artist" + i, new java.sql.Date(dateBase + 10000 * i));
		}
	}

	protected void createArtistsWildcardDataSet() throws Exception {
		tArtist.insert(1, "_X", null);
		tArtist.insert(2, "Y_", null);
	}

	@Test
	public void testSelect_QualfierOnToMany() throws Exception {

		tArtist.insert(1, "A1", new java.sql.Date(System.currentTimeMillis()));
		tPainting.insert(4, 1, "P1");
		tPainting.insert(5, 1, "P2");
		tPainting.insert(6, null, "P3");

		List<Artist> objects = SelectQuery.query(Artist.class,
				Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("P%")).select(context);

		// make sure no duplicate objects are returned when matching on a
		// to-many relationship
		assertEquals(1, objects.size());
	}

	@Test
	public void testFetchLimit() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.setFetchLimit(7);

		List<?> objects = context.performQuery(query);
		assertNotNull(objects);
		assertEquals(7, objects.size());
	}

	@Test
	public void testFetchOffset() throws Exception {

		createArtistsDataSet();

		int totalRows = new SelectQuery<>(Artist.class).select(context).size();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
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

		SelectQuery<DataRow> query = new SelectQuery<>(artistDbEntity);
		List<DataRow> results = context.select(query);

		assertEquals(20, results.size());
		assertTrue(results.get(0) instanceof DataRow);
	}

	@Test
	public void testFetchLimitWithOffset() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
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
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.setQualifier(ExpressionFactory.exp("db:ARTIST_ID > 3"));
		query.setFetchOffset(5);

		List<Artist> objects = query.select(context);
		int size = objects.size();

		SelectQuery<Artist> sizeQ = new SelectQuery<>(Artist.class);
		sizeQ.setQualifier(ExpressionFactory.exp("db:ARTIST_ID > 3"));
		List<Artist> objects1 = sizeQ.select(context);
		int sizeAll = objects1.size();
		assertEquals(size, sizeAll - 5);
	}

	@Test
	public void testFetchLimitWithQualifier() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.setQualifier(ExpressionFactory.exp("db:ARTIST_ID > 3"));
		query.setFetchLimit(7);
		List<Artist> objects = query.select(context);
		assertEquals(7, objects.size());
	}

	@Test
	public void testSelectAllObjectsRootEntityName() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>("Artist");
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectAllObjectsRootClass() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectAllObjectsRootObjEntity() throws Exception {
		createArtistsDataSet();
		ObjEntity artistEntity = context.getEntityResolver().getObjEntity(Artist.class);
		SelectQuery<Artist> query = new SelectQuery<>(artistEntity);

		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectLikeExactMatch() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.likeExp("artistName", "artist1");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectNotLikeSingleWildcardMatch() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.notLikeExp("artistName", "artist11%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(19, objects.size());
	}

	@Test
	public void testSelectNotLikeIgnoreCaseSingleWildcardMatch() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.notLikeIgnoreCaseExp("artistName", "aRtIsT11%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(19, objects.size());
	}

	/**
	 * SQL Server failure:
	 * http://stackoverflow.com/questions/14962419/is-the-like-operator-case-sensitive-with-ms-sql-server
	 */
	@Test
	public void testSelectLikeCaseSensitive() throws Exception {
		if (!accessStackAdapter.supportsCaseSensitiveLike()) {
			return;
		}

		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.likeExp("artistName", "aRtIsT%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(0, objects.size());
	}

	@Test
	public void testSelectLikeSingle_WildcardMatch() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.likeExp("artistName", "artist11%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectLikeSingle_WildcardMatchAndEscape() throws Exception {

		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		createArtistsWildcardDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.andQualifier(ExpressionFactory.likeExp("artistName", "=_%", '='));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectLike_WildcardMatchAndEscape_AndOtherCriteria() throws Exception {

		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		createArtistsWildcardDataSet();

		// CAY-1978 - combining LIKE..ESCAPE with another clause generated bad
		// syntax
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.andQualifier(ExpressionFactory.likeExp("artistName", "=_%", '='));
		query.andQualifier(Artist.ARTIST_NAME.eq("_X"));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectLike_WildcardMatchIgnoreCaseAndEscape_AndOtherCriteria() throws Exception {

		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		createArtistsWildcardDataSet();

		// CAY-1978 - combining LIKE..ESCAPE with another clause generated bad
		// SQL
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.andQualifier(ExpressionFactory.likeIgnoreCaseExp("artistName", "=_%", '='));
		query.andQualifier(Artist.ARTIST_NAME.eq("_X"));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectLike_WildcardMatchAndEscapeMulti_AndOtherCriteria() throws Exception {

		if(!accessStackAdapter.supportsEscapeInLike()) {
			return;
		}

		tArtist.insert(1, "_X_", null);
		tArtist.insert(2, "_X", null);

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.andQualifier(ExpressionFactory.likeExp("artistName", "#_%#_", '#'));
		query.andQualifier(Artist.ARTIST_NAME.eq("_X_"));

		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectLikeMultiple_WildcardMatch() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
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
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "ARTIST%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	/** Test how "like ignore case" works when using lowercase parameter. */
	@Test
	public void testSelectLikeIgnoreCaseObjects2() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.likeIgnoreCaseExp("artistName", "artist%");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.exp("artistName in ('artist1', 'artist2')");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(2, objects.size());
	}

	@Test
	public void testSelectParameterizedIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.exp("artistName in $list");
		query.setQualifier(qual);
		query = query.queryWithParameters(Collections.singletonMap("list", new Object[] { "artist1", "artist2" }));
		List<?> objects = context.performQuery(query);
		assertEquals(2, objects.size());
	}

	@Test
	public void testSelectParameterizedEmptyIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.exp("artistName in $list");
		query.setQualifier(qual);
		query = query.queryWithParameters(Collections.singletonMap("list", new Object[] {}));
		List<?> objects = context.performQuery(query);
		assertEquals(0, objects.size());
	}

	@Test
	public void testSelectParameterizedEmptyNotIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.exp("artistName not in $list");
		query.setQualifier(qual);
		query = query.queryWithParameters(Collections.singletonMap("list", new Object[] {}));
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectEmptyIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.inExp("artistName");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(0, objects.size());
	}

	@Test
	public void testSelectEmptyNotIn() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.notInExp("artistName");
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectBooleanTrue() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.expTrue();
		qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelectBooleanNotTrueOr() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
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
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.expFalse();
		qual = qual.andExp(ExpressionFactory.matchExp("artistName", "artist1"));
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(0, objects.size());
	}

	@Test
	public void testSelectBooleanFalseOr() throws Exception {
		createArtistsDataSet();
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.expFalse();
		qual = qual.orExp(ExpressionFactory.matchExp("artistName", "artist1"));
		query.setQualifier(qual);
		List<?> objects = context.performQuery(query);
		assertEquals(1, objects.size());
	}

	@Test
	public void testSelect() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		List<?> objects = query.select(context);
		assertEquals(20, objects.size());
	}

	@Test
	public void testSelectOne() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		Expression qual = ExpressionFactory.matchExp("artistName", "artist1");
		query.setQualifier(qual);

		Artist artist = (Artist) query.selectOne(context);
		assertEquals("artist1", artist.getArtistName());
	}

	@Test
	public void testSelectFirst() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.addOrdering(new Ordering(Artist.ARTIST_NAME.getName()));
		Artist artist = query.selectFirst(context);

		assertNotNull(artist);
		assertEquals("artist1", artist.getArtistName());
	}

	@Test
	public void testSelectFirstByContext() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.addOrdering(new Ordering(Artist.ARTIST_NAME.getName()));
		Artist artist = context.selectFirst(query);

		assertNotNull(artist);
		assertEquals("artist1", artist.getArtistName());
	}

	@Test
	public void testIterate() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> q1 = new SelectQuery<>(Artist.class);
		final int[] count = new int[1];
		q1.iterate(context, new ResultIteratorCallback<Artist>() {

			@Override
			public void next(Artist object) {
				assertNotNull(object.getArtistName());
				count[0]++;
			}
		});

		assertEquals(20, count[0]);
	}

	@Test
	public void testIterator() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> q1 = new SelectQuery<>(Artist.class);

		try (ResultIterator<Artist> it = q1.iterator(context);) {
			int count = 0;

			for (@SuppressWarnings("unused")
			Artist a : it) {
				count++;
			}

			assertEquals(20, count);
		}
	}

	@Test
	public void testBatchIterator() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> q1 = new SelectQuery<>(Artist.class);

		try (ResultBatchIterator<Artist> it = q1.batchIterator(context, 5);) {
			int count = 0;

			for (List<Artist> artistList : it) {
				count++;
				assertEquals(5, artistList.size());
			}

			assertEquals(4, count);
		}
	}

	/**
	 * Tests that all queries specified in prefetch are executed in a more
	 * complex prefetch scenario.
	 */
	@Test
	public void testRouteWithPrefetches() {
		EntityResolver resolver = context.getEntityResolver();
		MockQueryRouter router = new MockQueryRouter();

		SelectQuery<Artist> q = new SelectQuery<>(Artist.class, ExpressionFactory.matchExp("artistName", "a"));

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
		SelectQuery<Artist> q = new SelectQuery<>(Artist.class, e);
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

		SelectQuery<Painting> q = new SelectQuery<>(Painting.class, exp);
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
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class, ExpressionFactory.matchExp(
				"paintingArray+.toGallery", null));
		query.addPrefetch("artistExhibitArray");
		context.select(query);

		// TODO: assertions?
	}

	@Test
	public void testLeftJoinAndPrefetchToOne() throws Exception {
		createArtistsDataSet();
		SelectQuery<Painting> query = new SelectQuery<>(Painting.class, ExpressionFactory.matchExp(
				"toArtist+.artistName", null));
		query.addPrefetch("toGallery");
		context.select(query);

		// TODO: assertions?
	}

	@Test
	public void testMatchObject() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("a2");
		Artist a3 = context.newObject(Artist.class);
		a3.setArtistName("a3");
		context.commitChanges();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.setQualifier(ExpressionFactory.matchExp(a2));
		Artist result = query.selectOne(context);

		assertSame(a2, result);
	}

	@Test
	public void testMatchObjects() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("a2");
		Artist a3 = context.newObject(Artist.class);
		a3.setArtistName("a3");
		context.commitChanges();

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class);
		query.setQualifier(ExpressionFactory.matchAnyExp(a1, a3));
		query.addOrdering(Artist.ARTIST_NAME.asc());
		List<Artist> list = query.select(context);

		assertEquals(list.size(), 2);
		assertSame(a1, list.get(0));
		assertSame(a3, list.get(1));
	}

	@Test
	public void testMatchByRelatedObject() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("a2");
		Painting p1 = context.newObject(Painting.class);
		p1.setPaintingTitle("p1");
		p1.setToArtist(a1);
		Painting p2 = context.newObject(Painting.class);
		p2.setPaintingTitle("p2");
		p2.setToArtist(a2);
		context.commitChanges();

		SelectQuery<Painting> query = new SelectQuery<>(Painting.class);
		query.setQualifier(ExpressionFactory.matchExp("toArtist", a1));
		assertSame(p1, query.selectOne(context));
	}

	@Test
	public void testMatchByRelatedObjectId() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("a2");
		Painting p1 = context.newObject(Painting.class);
		p1.setPaintingTitle("p1");
		p1.setToArtist(a1);
		Painting p2 = context.newObject(Painting.class);
		p2.setPaintingTitle("p2");
		p2.setToArtist(a2);
		context.commitChanges();

		SelectQuery<Painting> query = new SelectQuery<>(Painting.class);
		query.setQualifier(ExpressionFactory.matchExp("toArtist", a1.getObjectId()));
		assertSame(p1, query.selectOne(context));
	}

	@Test
	public void testMatchByRelatedObjectIdValue() {

		Artist a1 = context.newObject(Artist.class);
		a1.setArtistName("a1");
		Artist a2 = context.newObject(Artist.class);
		a2.setArtistName("a2");
		Painting p1 = context.newObject(Painting.class);
		p1.setPaintingTitle("p1");
		p1.setToArtist(a1);
		Painting p2 = context.newObject(Painting.class);
		p2.setPaintingTitle("p2");
		p2.setToArtist(a2);
		context.commitChanges();

		SelectQuery<Painting> query = new SelectQuery<>(Painting.class);
		query.setQualifier(ExpressionFactory.matchExp("toArtist", Cayenne.longPKForObject(a1)));
		assertSame(p1, query.selectOne(context));
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
		SelectQuery<Artist> query = new SelectQuery<>(Artist.class, null, orderings);

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

		SelectQuery<Artist> query = new SelectQuery<>(Artist.class,
				ExpressionFactory.inExp("artistName", numbers));
		context.performQuery(query);
	}

	@Test
	public void testCacheOffsetAndLimit() throws Exception {
		createArtistsDataSet();

		SelectQuery<Artist> query1 = new SelectQuery<>(Artist.class);
		query1.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		query1.setFetchOffset(0);
		query1.setFetchLimit(10);
		context.performQuery(query1);

		SelectQuery<Artist> query2 = new SelectQuery<>(Artist.class);
		query2.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		query2.setFetchOffset(10);
		query2.setFetchLimit(10);
		context.performQuery(query2);

		SelectQuery<Artist> query3 = new SelectQuery<>(Artist.class);
		query3.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
		query3.setFetchOffset(10);
		query3.setFetchLimit(10);
		context.performQuery(query3);

		assertFalse(query1.metaData.getCacheKey().equals(query2.metaData.cacheKey));
		assertEquals(query2.metaData.getCacheKey(), query3.metaData.getCacheKey());
	}
}
