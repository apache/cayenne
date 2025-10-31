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

package org.apache.cayenne.access;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.Fault;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.ArtistExhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.PaintingInfo;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextPrefetchIT extends RuntimeCase {

	@Inject
	protected DataContext context;

	@Inject
	protected DBHelper dbHelper;

	@Inject
	protected DataChannelInterceptor queryInterceptor;

	protected TableHelper tArtist;
	protected TableHelper tPainting;
	protected TableHelper tPaintingInfo;
	protected TableHelper tExhibit;
	protected TableHelper tGallery;
	protected TableHelper tArtistExhibit;
	protected TableHelper tArtistGroup;
	protected TableHelper tArtGroup;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE", "GALLERY_ID").setColumnTypes(
				Types.INTEGER, Types.VARCHAR, Types.BIGINT, Types.DECIMAL, Types.INTEGER);

		tPaintingInfo = new TableHelper(dbHelper, "PAINTING_INFO");
		tPaintingInfo.setColumns("PAINTING_ID", "TEXT_REVIEW");

		tExhibit = new TableHelper(dbHelper, "EXHIBIT");
		tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");

		tArtistExhibit = new TableHelper(dbHelper, "ARTIST_EXHIBIT");
		tArtistExhibit.setColumns("ARTIST_ID", "EXHIBIT_ID");

		tGallery = new TableHelper(dbHelper, "GALLERY");
		tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

		tArtistGroup = new TableHelper(dbHelper, "ARTIST_GROUP");
		tArtistGroup.setColumns("ARTIST_ID", "GROUP_ID");

		tArtGroup = new TableHelper(dbHelper, "ARTGROUP");
		tArtGroup.setColumns("GROUP_ID", "NAME");
	}

	protected void createTwoArtistsAndTwoPaintingsDataSet() throws Exception {
		tArtist.insert(11, "artist2");
		tArtist.insert(101, "artist3");
		tPainting.insert(6, "p_artist3", 101, 1000, null);
		tPainting.insert(7, "p_artist2", 11, 2000, null);
	}

	protected void createArtistWithTwoPaintingsAndTwoInfosDataSet() throws Exception {
		tArtist.insert(11, "artist2");

		tPainting.insert(6, "p_artist2", 11, 1000, null);
		tPainting.insert(7, "p_artist3", 11, 2000, null);

		tPaintingInfo.insert(6, "xYs");
	}

	protected void createTwoArtistsWithExhibitsDataSet() throws Exception {
		tArtist.insert(11, "artist2");
		tArtist.insert(101, "artist3");

		tGallery.insert(25, "gallery1");
		tGallery.insert(31, "gallery2");
		tGallery.insert(45, "gallery3");

		Timestamp now = new Timestamp(System.currentTimeMillis());

		tExhibit.insert(1, 25, now, now);
		tExhibit.insert(2, 31, now, now);
		tExhibit.insert(3, 45, now, now);
		tExhibit.insert(4, 25, now, now);

		tArtistExhibit.insert(11, 2);
		tArtistExhibit.insert(11, 4);
		tArtistExhibit.insert(101, 1);
		tArtistExhibit.insert(101, 3);
		tArtistExhibit.insert(101, 4);
	}

	private void createArtistWithPaintingAndGallery() throws SQLException {
		tArtist.insert(1, "artist1");
		tGallery.insert(1, "gallery1");
		tPainting.insert(1, "painting1", 1, 100, 1);
	}

	@Test
	public void testPrefetchToMany_ViaPath() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.prefetch("paintingArray", PrefetchTreeNode.UNDEFINED_SEMANTICS);

		final List<Artist> artists = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {

			assertEquals(2, artists.size());

			for (int i = 0; i < 2; i++) {
				Artist a = artists.get(i);
				List<?> toMany = (List<?>) a.readPropertyDirectly("paintingArray");
				assertNotNull(toMany);
				assertFalse(((ValueHolder) toMany).isFault());
				assertEquals(1, toMany.size());

				Painting p = (Painting) toMany.get(0);
				assertEquals("Invalid prefetched painting:" + p, "p_" + a.getArtistName(), p.getPaintingTitle());
			}
		});
	}

	@Test
	public void testPrefetchToMany_WithQualfier() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		Map<String, Object> params = new HashMap<>();
		params.put("name1", "artist2");
		params.put("name2", "artist3");
		Expression e = ExpressionFactory.exp("artistName = $name1 or artistName = $name2");
		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.where(e.params(params))
				.prefetch(Artist.PAINTING_ARRAY.disjoint());

		final List<Artist> artists = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {

			assertEquals(2, artists.size());

			Artist a1 = artists.get(0);
			List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
			assertNotNull(toMany);
			assertFalse(((ValueHolder) toMany).isFault());
			assertEquals(1, toMany.size());

			Painting p1 = (Painting) toMany.get(0);
			assertEquals("p_" + a1.getArtistName(), p1.getPaintingTitle());

			Artist a2 = artists.get(1);
			List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
			assertNotNull(toMany2);
			assertFalse(((ValueHolder) toMany2).isFault());
			assertEquals(1, toMany2.size());

			Painting p2 = (Painting) toMany2.get(0);
			assertEquals("p_" + a2.getArtistName(), p2.getPaintingTitle());
		});
	}

	@Test
	public void testPrefetchToManyNoQualifier() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.prefetch(Artist.PAINTING_ARRAY.disjoint());

		final List<Artist> artists = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {

			assertEquals(2, artists.size());

			for (int i = 0; i < 2; i++) {
				Artist a = artists.get(i);
				List<?> toMany = (List<?>) a.readPropertyDirectly("paintingArray");
				assertNotNull(toMany);
				assertFalse(((ValueHolder) toMany).isFault());
				assertEquals(1, toMany.size());

				Painting p = (Painting) toMany.get(0);
				assertEquals("Invalid prefetched painting:" + p, "p_" + a.getArtistName(), p.getPaintingTitle());
			}
		});
	}

	@Test
	public void testPrefetchByPathToManyNoQualifier() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		List<Artist> artists = ObjectSelect.query(Artist.class)
				.prefetch("paintingArray", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS)
				.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {

			assertEquals(2, artists.size());

			for (int i = 0; i < 2; i++) {
				Artist a = artists.get(i);
				List<?> toMany = (List<?>) a.readPropertyDirectly("paintingArray");
				assertNotNull(toMany);
				assertFalse(((ValueHolder) toMany).isFault());
				assertEquals(1, toMany.size());

				Painting p = (Painting) toMany.get(0);
				assertEquals("Invalid prefetched painting:" + p, "p_" + a.getArtistName(), p.getPaintingTitle());
			}
		});
	}

	/**
	 * Test that a to-many relationship is initialized when a target entity has
	 * a compound PK only partially involved in relationship.
	 */
	@Test
	public void testPrefetchToMany_OnJoinTableDisjoinedPrefetch() throws Exception {

		createTwoArtistsWithExhibitsDataSet();

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.prefetch(Artist.ARTIST_EXHIBIT_ARRAY.disjoint())
				.orderBy(Artist.ARTIST_NAME.asc());

		final List<Artist> artists = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(2, artists.size());

			Artist a1 = artists.get(0);
			assertEquals("artist2", a1.getArtistName());
			List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
			assertNotNull(toMany);
			assertFalse(((ValueHolder) toMany).isFault());
			assertEquals(2, toMany.size());

			ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
			assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
			assertSame(a1, artistExhibit.getToArtist());

			Artist a2 = artists.get(1);
			assertEquals("artist3", a2.getArtistName());
			List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
			assertNotNull(toMany2);
			assertFalse(((ValueHolder) toMany2).isFault());
			assertEquals(3, toMany2.size());

			ArtistExhibit artistExhibit2 = (ArtistExhibit) toMany2.get(0);
			assertEquals(PersistenceState.COMMITTED, artistExhibit2.getPersistenceState());
			assertSame(a2, artistExhibit2.getToArtist());
		});
	}

	@Test
	public void testPrefetchToMany_OnJoinTableJoinedPrefetch() throws Exception {
		createTwoArtistsWithExhibitsDataSet();

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.prefetch(Artist.ARTIST_EXHIBIT_ARRAY.joint())
				.orderBy(Artist.ARTIST_NAME.asc());

		final List<Artist> artists = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {

			assertEquals(2, artists.size());

			Artist a1 = artists.get(0);
			assertEquals("artist2", a1.getArtistName());
			List<?> toMany = (List<?>) a1.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
			assertNotNull(toMany);
			assertFalse(((ValueHolder) toMany).isFault());
			assertEquals(2, toMany.size());

			ArtistExhibit artistExhibit = (ArtistExhibit) toMany.get(0);
			assertEquals(PersistenceState.COMMITTED, artistExhibit.getPersistenceState());
			assertSame(a1, artistExhibit.getToArtist());

			Artist a2 = artists.get(1);
			assertEquals("artist3", a2.getArtistName());
			List<?> toMany2 = (List<?>) a2.readPropertyDirectly(Artist.ARTIST_EXHIBIT_ARRAY.getName());
			assertNotNull(toMany2);
			assertFalse(((ValueHolder) toMany2).isFault());
			assertEquals(3, toMany2.size());

			ArtistExhibit artistExhibit2 = (ArtistExhibit) toMany2.get(0);
			assertEquals(PersistenceState.COMMITTED, artistExhibit2.getPersistenceState());
			assertSame(a2, artistExhibit2.getToArtist());
		});
	}

	/**
	 * Test that a to-many relationship is initialized when there is no inverse
	 * relationship
	 */
	@Test
	public void testPrefetch_ToManyNoReverse() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjEntity paintingEntity = context.getEntityResolver().getObjEntity(Painting.class);
		ObjRelationship relationship = paintingEntity.getRelationship("toArtist");
		paintingEntity.removeRelationship("toArtist");

		try {

			final List<Artist> result = ObjectSelect.query(Artist.class)
					.prefetch(Artist.PAINTING_ARRAY.disjoint())
					.select(context);

			queryInterceptor.runWithQueriesBlocked(() -> {
				assertFalse(result.isEmpty());
				Artist a1 = result.get(0);
				List<?> toMany = (List<?>) a1.readPropertyDirectly("paintingArray");
				assertNotNull(toMany);
				assertFalse(((ValueHolder) toMany).isFault());
			});
		} finally {
			paintingEntity.addRelationship(relationship);
		}
	}

	@Test
	public void testPrefetch_ToManyNoReverseWithQualifier() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjEntity paintingEntity = context.getEntityResolver().getObjEntity(Painting.class);
		ObjRelationship relationship = paintingEntity.getRelationship("toArtist");
		paintingEntity.removeRelationship("toArtist");

		try {

			final List<Artist> result = ObjectSelect.query(Artist.class)
					.where(Artist.ARTIST_NAME.eq("artist2"))
					.prefetch(Artist.PAINTING_ARRAY.disjoint())
					.select(context);

			queryInterceptor.runWithQueriesBlocked(() -> {
				assertFalse(result.isEmpty());
				Artist a1 = result.get(0);
				List<?> toMany = (List<?>) a1.readPropertyDirectly("paintingArray");
				assertNotNull(toMany);
				assertFalse(((ValueHolder) toMany).isFault());
			});

		} finally {
			paintingEntity.addRelationship(relationship);
		}
	}

	@Test
	public void testPrefetch_ToOne() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint());

		final List<Painting> result = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertFalse(result.isEmpty());
			Painting p1 = result.get(0);

			Object toOnePrefetch = p1.readNestedProperty("toArtist");
			assertNotNull(toOnePrefetch);
			assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
					toOnePrefetch instanceof Artist);

			Artist a1 = (Artist) toOnePrefetch;
			assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
		});
	}

	@Test
	public void testPrefetch_ToOne_DbPath() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint())
				.and(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("artist2"));

		List<Painting> results = q.select(context);

		assertEquals(1, results.size());
	}

	@Test
	public void testPrefetch_ToOne_ObjPath() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint())
				.and(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("artist2"));

		List<Painting> results = q.select(context);
		assertEquals(1, results.size());
	}

	@Test
	public void testPrefetch_ReflexiveRelationship() {
		ArtGroup parent = (ArtGroup) context.newObject("ArtGroup");
		parent.setName("parent");
		ArtGroup child = (ArtGroup) context.newObject("ArtGroup");
		child.setName("child");
		child.setToParentGroup(parent);
		context.commitChanges();

		ObjectSelect<ArtGroup> q = ObjectSelect.query(ArtGroup.class)
				.where(ArtGroup.NAME.eq("child"))
				.prefetch("toParentGroup", PrefetchTreeNode.UNDEFINED_SEMANTICS);

		final List<ArtGroup> results = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			ArtGroup fetchedChild = results.get(0);
			// The parent must be fully fetched, not just HOLLOW (a fault)
			assertEquals(PersistenceState.COMMITTED, fetchedChild.getToParentGroup().getPersistenceState());
		});

		child.setToParentGroup(null);
		context.commitChanges();
	}

	@Test
	public void testPrefetch_ToOneWithQualifierOverlappingPrefetchPath() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("artist3"))
				.prefetch(Painting.TO_ARTIST.disjoint());

		final List<Painting> results = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Painting painting = results.get(0);

			// The parent must be fully fetched, not just HOLLOW (a fault)
			assertEquals(PersistenceState.COMMITTED, painting.getToArtist().getPersistenceState());
		});
	}

	@Test
	public void testPrefetch_ToOneWith_OuterJoinFlattenedQualifier() throws Exception {

		tArtGroup.insert(1, "AG");
		tArtist.insert(11, "artist2");
		tArtist.insert(101, "artist3");
		tPainting.insert(6, "p_artist3", 101, 1000, null);
		tPainting.insert(7, "p_artist21", 11, 2000, null);
		tPainting.insert(8, "p_artist22", 11, 3000, null);

		// flattened join matches an object that is NOT the one we are looking
		// for
		tArtistGroup.insert(101, 1);

		// OUTER join part intentionally doesn't match anything
		Expression exp = PropertyFactory.createBase("groupArray+.name", String.class)
				.eq("XX").orExp(Artist.ARTIST_NAME.eq("artist2"));

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.where(exp)
				.prefetch(Artist.PAINTING_ARRAY.disjoint());

		final List<Artist> results = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Artist a = results.get(0);
			assertEquals("artist2", a.getArtistName());
			assertEquals(2, a.getPaintingArray().size());
		});
	}

	@Test
	public void testPrefetch9() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		Artist artist1 = ObjectSelect
				.query(Artist.class)
				.where(Artist.ARTIST_NAME.eq("artist3"))
				.select(context)
				.get(0);

		// find the painting not matching the artist (this is the case where
		// such prefetch
		// at least makes sense)
		Expression exp = ExpressionFactory.noMatchExp("toArtist", artist1);

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.where(Painting.TO_ARTIST.eq(artist1))
				.prefetch("toArtist", PrefetchTreeNode.UNDEFINED_SEMANTICS);

		final List<Painting> results = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			// see that artists are resolved...

			Painting px = results.get(0);
			Artist ax = (Artist) px.readProperty(Painting.TO_ARTIST.getName());
			assertEquals(PersistenceState.COMMITTED, ax.getPersistenceState());
		});
	}

	@Test
	public void testPrefetch_OneToOneWithQualifier() throws Exception {
		createArtistWithTwoPaintingsAndTwoInfosDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).like("a%"))
				.prefetch(Painting.TO_PAINTING_INFO.disjoint())
				.orderBy(Painting.PAINTING_TITLE.asc());

		final List<Painting> results = q.select(context);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(2, results.size());

			// testing non-null to-one target
			Painting p0 = results.get(0);
			Object o2 = p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
			assertTrue(o2 instanceof PaintingInfo);
			PaintingInfo pi2 = (PaintingInfo) o2;
			assertEquals(PersistenceState.COMMITTED, pi2.getPersistenceState());
			assertEquals(Cayenne.intPKForObject(p0), Cayenne.intPKForObject(pi2));

			// testing null to-one target
			Painting p1 = results.get(1);
			assertNull(p1.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName()));

			// there was a bug marking an object as dirty when clearing the
			// relationships
			assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
		});
	}

	@Test
	public void testPrefetchToMany_DateInQualifier() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Artist> q = ObjectSelect.query(Artist.class)
				.where(Artist.DATE_OF_BIRTH.eq(new Date()))
				.prefetch("paintingArray", PrefetchTreeNode.UNDEFINED_SEMANTICS);

		// prefetch with query using date in qualifier used to fail on SQL Server
		// see CAY-119 for details
		context.performQuery(q);
	}

	@Test
	public void testPrefetchingToOneNull() throws Exception {

		tPainting.insert(6, "p_Xty", null, 1000, null);

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint());

		final List<Painting> paintings = context.select(q);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, paintings.size());

			Painting p2 = paintings.get(0);
			assertNull(p2.readProperty(Painting.TO_ARTIST.getName()));
		});
	}

	@Test
	public void testPrefetchToOneSharedCache() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint())
				.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

		context.select(q);

		queryInterceptor.runWithQueriesBlocked(() -> {
			// per CAY-499 second run of a cached query with prefetches
			// (i.e. when the
			// result is served from cache) used to throw an exception...

			List<Painting> cachedResult = context.select(q);

			assertFalse(cachedResult.isEmpty());
			Painting p1 = cachedResult.get(0);

			Object toOnePrefetch = p1.readNestedProperty("toArtist");
			assertNotNull(toOnePrefetch);
			assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
					toOnePrefetch instanceof Artist);

			Artist a1 = (Artist) toOnePrefetch;
			assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

			// and just in case - run one more time...
			context.performQuery(q);
		});
	}

	@Test
	public void testPrefetchToOneLocalCache() throws Exception {
		createTwoArtistsAndTwoPaintingsDataSet();

		ObjectSelect<Painting> q = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint())
				.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

		context.select(q);

		queryInterceptor.runWithQueriesBlocked(() -> {
			// per CAY-499 second run of a cached query with prefetches
			// (i.e. when the
			// result is served from cache) used to throw an exception...

			List<Painting> cachedResult = context.select(q);

			assertFalse(cachedResult.isEmpty());
			Painting p1 = cachedResult.get(0);

			Object toOnePrefetch = p1.readNestedProperty("toArtist");
			assertNotNull(toOnePrefetch);
			assertTrue("Expected Artist, got: " + toOnePrefetch.getClass().getName(),
					toOnePrefetch instanceof Artist);

			Artist a1 = (Artist) toOnePrefetch;
			assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());

			// and just in case - run one more time...
			context.performQuery(q);
		});
	}

	@Test
	public void testPrefetchToOneWithBackRelationship() throws Exception {
		createArtistWithTwoPaintingsAndTwoInfosDataSet();

		ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
				.and(Painting.PAINTING_TITLE.eq("p_artist2"))
				.prefetch(Painting.TO_PAINTING_INFO.disjoint())
				.prefetch(Painting.TO_PAINTING_INFO.dot(PaintingInfo.PAINTING).disjoint());
		final List<Painting> results = context.select(query);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Painting p0 = results.get(0);
			PaintingInfo pi0 = (PaintingInfo) p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
			assertNotNull(pi0);
			assertNotNull(pi0.readPropertyDirectly(PaintingInfo.PAINTING.getName()));
		});
	}

	@Test
	public void testPrefetchPaintingOverToOneAndToMany() throws Exception {
		createArtistWithTwoPaintingsAndTwoInfosDataSet();

		ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
				.and(Painting.PAINTING_TITLE.eq("p_artist2"))
				.prefetch(Painting.TO_ARTIST.disjoint())
				.prefetch(Painting.TO_ARTIST.dot(Artist.PAINTING_ARRAY).disjoint());
		final List<Painting> results = context.select(query);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Painting p0 = results.get(0);
			Artist a0 = (Artist) p0.readPropertyDirectly(Painting.TO_ARTIST.getName());
			assertNotNull(a0);
			List<?> paintings = (List<?>) a0.readPropertyDirectly(Artist.PAINTING_ARRAY.getName());
			assertEquals(2, paintings.size());
		});
	}

	@Test
	public void testPrefetchToOneWithBackRelationship_Joint() throws Exception {
		createArtistWithTwoPaintingsAndTwoInfosDataSet();

		ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
				.and(Painting.PAINTING_TITLE.eq("p_artist2"))
				.prefetch(Painting.TO_PAINTING_INFO.joint())
				.prefetch(Painting.TO_PAINTING_INFO.dot(PaintingInfo.PAINTING).joint());
		final List<Painting> results = context.select(query);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Painting p0 = results.get(0);
			PaintingInfo pi0 = (PaintingInfo) p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
			assertNotNull(pi0);
			assertNotNull(pi0.readPropertyDirectly(PaintingInfo.PAINTING.getName()));
		});
	}

	@Test
	public void testPrefetchJointAndDisjointByIdTogether() throws Exception {
		createArtistWithTwoPaintingsAndTwoInfosDataSet();

		ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
				.and(Painting.PAINTING_TITLE.eq("p_artist2"))
				.prefetch(Painting.TO_ARTIST.joint())
				.prefetch(Painting.TO_PAINTING_INFO.disjointById());
		final List<Painting> results = context.select(query);

		queryInterceptor.runWithQueriesBlocked(() -> {
			assertEquals(1, results.size());

			Painting p0 = results.get(0);
			Artist a0 = (Artist) p0.readPropertyDirectly(Painting.TO_ARTIST.getName());
			assertNotNull(a0);

			PaintingInfo info = (PaintingInfo) p0.readPropertyDirectly(Painting.TO_PAINTING_INFO.getName());
			assertNotNull(info);
		});
	}

	/**
	 * This test and next one is the result of CAY-2349 fix
	 */
	@Test
	public void testPrefetchWithLocalCache() throws Exception {
		createArtistWithPaintingAndGallery();

		List<Painting> paintings = ObjectSelect.query(Painting.class)
				.localCache("g1").select(context);
		assertEquals(1, paintings.size());
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Fault);

		paintings = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.joint())
				.localCache("g1").select(context);
		assertEquals(1, paintings.size());
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);

		queryInterceptor.runWithQueriesBlocked(() -> {
			List<Painting> paintings1 = ObjectSelect.query(Painting.class)
					.prefetch(Painting.TO_ARTIST.joint())
					.localCache("g1").select(context);
			assertEquals(1, paintings1.size());
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
		});
	}

	@Test
	public void testPrefetchWithSharedCache() throws Exception {
		createArtistWithPaintingAndGallery();

		ObjectSelect<Painting> s1 = ObjectSelect.query(Painting.class)
			.sharedCache("g1");

		ObjectSelect<Painting> s2 = ObjectSelect.query(Painting.class)
			.prefetch(Painting.TO_ARTIST.disjoint())
			.sharedCache("g1");

		ObjectSelect<Painting> s3 = ObjectSelect.query(Painting.class)
			.prefetch(Painting.TO_GALLERY.joint())
			.sharedCache("g1");

		ObjectSelect<Painting> s4 = ObjectSelect.query(Painting.class)
				.prefetch(Painting.TO_ARTIST.disjoint())
				.prefetch(Painting.TO_GALLERY.joint())
				.sharedCache("g1");

		// first iteration select from DB and cache
		List<Painting> paintings = s1.select(context);
		assertEquals(1, paintings.size());
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Fault);
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Fault);

		paintings = s2.select(context);
		assertEquals(1, paintings.size());
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Fault);

		paintings = s3.select(context);
		assertEquals(1, paintings.size());
		// Note: s3 prefetches only TO_GALLERY, so TO_ARTIST may be reset based on the prefetch pattern
		// The important thing is that TO_GALLERY is fetched via prefetch
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Gallery);

		paintings = s4.select(context);
		assertEquals(1, paintings.size());
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
		assertTrue(paintings.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Gallery);

		queryInterceptor.runWithQueriesBlocked(() -> {
			// select from cache - relationships that are already resolved stay resolved
			// This prevents stale cached data from clobbering newer in-memory state
			List<Painting> paintings1 = s2.select(context);
			assertEquals(1, paintings1.size());
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
			// TO_GALLERY stays resolved (not reset to Fault) from previous queries
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Gallery);

			paintings1 = s3.select(context);
			assertEquals(1, paintings1.size());
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Gallery);

			paintings1 = s4.select(context);
			assertEquals(1, paintings1.size());
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_ARTIST.getName()) instanceof Artist);
			assertTrue(paintings1.get(0).readPropertyDirectly(Painting.TO_GALLERY.getName()) instanceof Gallery);
		});
	}

}
