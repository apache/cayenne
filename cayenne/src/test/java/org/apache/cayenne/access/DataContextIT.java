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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.NullTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.ROArtist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextIT extends RuntimeCase {

	@Inject
	protected DataContext context;

	@Inject
	protected DBHelper dbHelper;

	@Inject
	protected UnitDbAdapter accessStackAdapter;

	@Inject
	protected DataChannelInterceptor queryInterceptor;

	@Inject
	protected RuntimeCaseDataSourceFactory dataSourceFactory;

	protected TableHelper tArtist;
	protected TableHelper tExhibit;
	protected TableHelper tGallery;
	protected TableHelper tPainting;

	@Before
	public void setUp() throws Exception {
		tArtist = new TableHelper(dbHelper, "ARTIST");
		tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

		tExhibit = new TableHelper(dbHelper, "EXHIBIT");
		tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");

		tGallery = new TableHelper(dbHelper, "GALLERY");
		tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");

		tPainting = new TableHelper(dbHelper, "PAINTING");
		tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE");
	}

	protected void createSingleArtistDataSet() throws Exception {
		tArtist.insert(33001, "artist1");
	}

	protected void createFiveArtistDataSet_MixedCaseName() throws Exception {
		tArtist.insert(33001, "artist1");
		tArtist.insert(33002, "Artist3");
		tArtist.insert(33003, "aRtist5");
		tArtist.insert(33004, "arTist2");
		tArtist.insert(33005, "artISt4");
	}

	protected void createGalleriesAndExhibitsDataSet() throws Exception {

		tGallery.insert(33001, "gallery1");
		tGallery.insert(33002, "gallery2");
		tGallery.insert(33003, "gallery3");
		tGallery.insert(33004, "gallery4");

		Timestamp now = new Timestamp(System.currentTimeMillis());

		tExhibit.insert(1, 33001, now, now);
		tExhibit.insert(2, 33002, now, now);
	}

	protected void createArtistsDataSet() throws Exception {
		tArtist.insert(33001, "artist1");
		tArtist.insert(33002, "artist2");
		tArtist.insert(33003, "artist3");
		tArtist.insert(33004, "artist4");
		tArtist.insert(33005, "artist5");
		tArtist.insert(33006, "artist11");
		tArtist.insert(33007, "artist21");
	}

	protected void createArtistsAndPaintingsDataSet() throws Exception {
		createArtistsDataSet();

		tPainting.insert(33001, "P_artist1", 33001, 1000);
		tPainting.insert(33002, "P_artist2", 33002, 2000);
		tPainting.insert(33003, "P_artist3", 33003, 3000);
		tPainting.insert(33004, "P_artist4", 33004, 4000);
		tPainting.insert(33005, "P_artist5", 33005, 5000);
		tPainting.insert(33006, "P_artist11", 33006, 11000);
		tPainting.insert(33007, "P_artist21", 33007, 21000);
	}

	@Test
	public void testCurrentSnapshot1() throws Exception {
		createSingleArtistDataSet();

		Artist artist = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1")).selectFirst(context);

		DataRow snapshot = context.currentSnapshot(artist);
		assertEquals(artist.getArtistName(), snapshot.get("ARTIST_NAME"));
		assertEquals(artist.getDateOfBirth(), snapshot.get("DATE_OF_BIRTH"));
		assertEquals("Artist", snapshot.getEntityName());
	}

	@Test
	public void testCurrentSnapshot2() throws Exception {
		createSingleArtistDataSet();

		// test null values
		Artist artist = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1")).selectFirst(context);

		artist.setArtistName(null);
		artist.setDateOfBirth(null);

		DataRow snapshot = context.currentSnapshot(artist);
		assertEquals("Artist", snapshot.getEntityName());

		assertTrue(snapshot.containsKey("ARTIST_NAME"));
		assertNull(snapshot.get("ARTIST_NAME"));

		assertTrue(snapshot.containsKey("DATE_OF_BIRTH"));
		assertNull(snapshot.get("DATE_OF_BIRTH"));
	}

	@Test
	public void testCurrentSnapshot3() throws Exception {
		createSingleArtistDataSet();

		// test null values
		Artist artist = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1")).selectFirst(context);

		// test FK relationship snapshotting
		Painting p1 = new Painting();
		context.registerNewObject(p1);
		p1.setToArtist(artist);

		DataRow s1 = context.currentSnapshot(p1);
		assertEquals("Painting", s1.getEntityName());
		Map<String, Object> idMap = artist.getObjectId().getIdSnapshot();
		assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
	}

	/**
	 * Testing snapshot with to-one fault. This was a bug CAY-96.
	 */
	@Test
	public void testCurrentSnapshotWithToOneFault() throws Exception {

		createGalleriesAndExhibitsDataSet();

		// Exhibit with Gallery as Fault must still include Gallery
		// Artist and Exhibit (Exhibit has unresolved to-one to gallery as in
		// the
		// CAY-96 bug report)

        ObjectId eId = ObjectId.of("Exhibit", Exhibit.EXHIBIT_ID_PK_COLUMN, 2);
		Exhibit e = (Exhibit) context.performQuery(new ObjectIdQuery(eId)).get(0);

		assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY.getName()) instanceof Fault);

		DataRow snapshot = context.currentSnapshot(e);

		// assert that after taking a snapshot, we have FK in, but the
		// relationship
		// is still a Fault
		assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY.getName()) instanceof Fault);
		assertEquals(33002, snapshot.get("GALLERY_ID"));
	}

	/**
	 * Tests how CHAR field is handled during fetch. Some databases (Oracle...)
	 * would pad a CHAR column with extra spaces, returned to the client.
	 * Cayenne should trim it.
	 */
	@Test
	public void testCharFetch() throws Exception {
		createSingleArtistDataSet();

		Artist artist = ObjectSelect.query(Artist.class).selectFirst(context);
		assertEquals(artist.getArtistName().trim(), artist.getArtistName());
	}

	/**
	 * Tests how CHAR field is handled during fetch in the WHERE clause. Some
	 * databases (Oracle...) would pad a CHAR column with extra spaces, returned
	 * to the client. Cayenne should trim it.
	 */
	@Test
	public void testCharInQualifier() throws Exception {
		createArtistsDataSet();

		List<Artist> artists = ObjectSelect.query(Artist.class, Artist.ARTIST_NAME.eq("artist1")).select(context);
		assertEquals(1, artists.size());
	}

	/**
	 * Test fetching query with multiple relationship paths between the same 2
	 * entities used in qualifier.
	 */
	@Test
	public void testMultiObjRelFetch() throws Exception {
		createArtistsAndPaintingsDataSet();

		List<Painting> results = ObjectSelect.query(Painting.class)
				.where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("artist2"))
				.or(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).eq("artist4"))
				.select(context);

		assertEquals(2, results.size());
	}

	/**
	 * Test fetching query with multiple relationship paths between the same 2
	 * entities used in qualifier.
	 */
	@Test
	public void testMultiDbRelFetch() throws Exception {
		createArtistsAndPaintingsDataSet();

		List<Painting> results = ObjectSelect.query(Painting.class)
				.where(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist2"))
				.or(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist4"))
				.select(context);

		assertEquals(2, results.size());
	}

	@Test
	public void testSelectDate() throws Exception {
		createGalleriesAndExhibitsDataSet();

		List<Exhibit> objects = ObjectSelect.query(Exhibit.class).select(context);
		assertFalse(objects.isEmpty());

		Exhibit e1 = objects.get(0);
		assertEquals(java.util.Date.class, e1.getClosingDate().getClass());
	}

	@Test
	public void testCaseInsensitiveOrdering() throws Exception {
		if (!accessStackAdapter.supportsCaseInsensitiveOrder()) {
			return;
		}

		createFiveArtistDataSet_MixedCaseName();

		// case insensitive ordering appends extra columns
		// to the query when query is using DISTINCT...
		// verify that the result is not messed up

		List<Artist> objects = ObjectSelect.query(Artist.class)
				.orderBy(Artist.ARTIST_NAME.ascInsensitive())
				.select(context);
		assertEquals(5, objects.size());

		Artist artist = objects.get(0);
		DataRow snapshot = context.getObjectStore().getSnapshot(artist.getObjectId());
		assertEquals(3, snapshot.size());

		// assert the ordering
		assertEquals("artist1", objects.get(0).getArtistName());
		assertEquals("arTist2", objects.get(1).getArtistName());
		assertEquals("Artist3", objects.get(2).getArtistName());
		assertEquals("artISt4", objects.get(3).getArtistName());
		assertEquals("aRtist5", objects.get(4).getArtistName());
	}

	@Test
	public void testSelect_DataRows() throws Exception {
		createArtistsAndPaintingsDataSet();

		List<DataRow> objects = ObjectSelect.dataRowQuery(Artist.class, null).select(context);

		assertNotNull(objects);
		assertEquals(7, objects.size());
		assertTrue("DataRow expected, got " + objects.get(0).getClass(), objects.get(0) instanceof DataRow);
	}

	@Test
	public void testPerformSelectQuery1() throws Exception {
		createArtistsAndPaintingsDataSet();

		List<Artist> objects = ObjectSelect.query(Artist.class).select(context);

		assertNotNull(objects);
		assertEquals(7, objects.size());
		assertTrue("Artist expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Artist);
	}

	@Test
	public void testPerformSelectQuery2() throws Exception {
		createArtistsAndPaintingsDataSet();

		// do a query with complex qualifier
		List<?> objects = ObjectSelect.query(Artist.class)
				.where(Artist.ARTIST_NAME.eq("artist3"))
				.or(Artist.ARTIST_NAME.eq("artist5"))
				.or(Artist.ARTIST_NAME.eq("artist21"))
				.select(context);

		assertNotNull(objects);
		assertEquals(3, objects.size());
		assertTrue("Artist expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Artist);
	}

	@Test
	public void testPerformQuery_Routing() {
		Query query = mock(Query.class);
		QueryMetadata md = mock(QueryMetadata.class);
		when(query.getMetaData(any(EntityResolver.class))).thenReturn(md);
		context.performGenericQuery(query);
		verify(query).route(any(QueryRouter.class), eq(context.getEntityResolver()), (Query) isNull());
	}

	@Test
	public void testPerformNonSelectingQuery() throws Exception {

		createSingleArtistDataSet();

		ObjectSelect<Painting> query = ObjectSelect.query(Painting.class, ExpressionFactory.exp("db:PAINTING_ID = 1"));

		assertEquals(0, query.select(context).size());

		SQLTemplate insert = new SQLTemplate(Painting.class,
				"INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
						+ "VALUES (1, 'PX', 33001, 1)");
		context.performNonSelectingQuery(insert);

		assertEquals(1, query.select(context).size());
	}

	@Test
	public void testPerformNonSelectingQueryCounts1() throws Exception {
		createArtistsDataSet();

		SQLTemplate query = new SQLTemplate(Painting.class,
				"INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
						+ "VALUES ($pid, '$pt', $aid, $price)");

		Map<String, Object> map = new HashMap<>();
		map.put("pid", 1);
		map.put("pt", "P1");
		map.put("aid", 33002);
		map.put("price", 1.1);

		// single batch of parameters
		query.setParameters(map);

		int[] counts = context.performNonSelectingQuery(query);
		assertNotNull(counts);
		assertEquals(1, counts.length);
		assertEquals(1, counts[0]);
	}

	@Test
	public void testPerformNonSelectingQueryCounts2() throws Exception {

		createArtistsDataSet();

		SQLTemplate query = new SQLTemplate(Painting.class,
				"INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
						+ "VALUES ($pid, '$pt', $aid, #bind($price 'DECIMAL' 2))");

		Map<String, Object>[] maps = new Map[3];
		for (int i = 0; i < maps.length; i++) {
			maps[i] = new HashMap<>();
			maps[i].put("pid", 1 + i);
			maps[i].put("pt", "P-" + i);
			maps[i].put("aid", 33002);
			maps[i].put("price", new BigDecimal("1." + i));
		}

		// single batch of parameters
		query.setParameters(maps);

		int[] counts = context.performNonSelectingQuery(query);
		assertNotNull(counts);
		assertEquals(maps.length, counts.length);
		for (int i = 0; i < maps.length; i++) {
			assertEquals(1, counts[i]);
		}

		SQLTemplate delete = new SQLTemplate(Painting.class, "delete from PAINTING");
		counts = context.performNonSelectingQuery(delete);
		assertNotNull(counts);
		assertEquals(1, counts.length);
		assertEquals(3, counts[0]);
	}

	@Test
	public void testPerformPaginatedQuery() throws Exception {
		createArtistsDataSet();

		List<Artist> objects = ObjectSelect.query(Artist.class).pageSize(5).select(context);
		assertNotNull(objects);
		assertTrue(objects instanceof IncrementalFaultList<?>);
		assertTrue(((IncrementalFaultList<Artist>) objects).elements.get(0) instanceof Long);
		assertTrue(((IncrementalFaultList<Artist>) objects).elements.get(6) instanceof Long);

		assertTrue(objects.get(0) instanceof Artist);
	}

	@Test
	public void testPerformPaginatedQuery1() throws Exception {
		createArtistsDataSet();

		EJBQLQuery query = new EJBQLQuery("select a FROM Artist a");
		query.setPageSize(5);
		List<?> objects = context.performQuery(query);
		assertNotNull(objects);
		assertTrue(objects instanceof IncrementalFaultList<?>);
		assertTrue(((IncrementalFaultList<?>) objects).elements.get(0) instanceof Long);
		assertTrue(((IncrementalFaultList<?>) objects).elements.get(6) instanceof Long);

		assertTrue(objects.get(0) instanceof Artist);
	}

	@Test
	public void testPerformPaginatedQueryBigPage() throws Exception {
		createArtistsDataSet();

		final List<?> objects = ObjectSelect.query(Artist.class).pageSize(5).select(context);
		assertNotNull(objects);
		assertTrue(objects instanceof IncrementalFaultList<?>);

		queryInterceptor.runWithQueriesBlocked(() -> assertEquals(7, objects.size()));
	}

	@Test
	public void testPerformDataRowQuery() throws Exception {

		createArtistsDataSet();

		List<?> objects = ObjectSelect.dataRowQuery(Artist.class).select(context);

		assertNotNull(objects);
		assertEquals(7, objects.size());
		assertTrue("Map expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Map<?, ?>);
	}

	@Test
	public void testCommitChangesRO1() throws Exception {

		ROArtist a1 = (ROArtist) context.newObject("ROArtist");
		a1.writePropertyDirectly("artistName", "abc");
		a1.setPersistenceState(PersistenceState.MODIFIED);

		try {
			context.commitChanges();
			fail("Inserting a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected,
			// must blow on saving new "read-only" object.
		}
	}

	@Test
	public void testCommitChangesRO2() throws Exception {
		createArtistsDataSet();

		ROArtist a1 = ObjectSelect.query(ROArtist.class, Artist.ARTIST_NAME.eq("artist1")).selectOne(context);
		a1.writeProperty(ROArtist.ARTIST_NAME.getName(), "abc");

		try {
			context.commitChanges();
			fail("Updating a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected,
			// must blow on saving new "read-only" object.
		}
	}

	@Test
	public void testCommitChangesRO3() throws Exception {

		createArtistsDataSet();

		ROArtist a1 = ObjectSelect.query(ROArtist.class, Artist.ARTIST_NAME.eq("artist1")).selectOne(context);
		context.deleteObjects(a1);

		try {
			context.commitChanges();
			fail("Deleting a 'read-only' object must fail.");
		} catch (Exception ex) {
			// exception is expected,
			// must blow on saving new "read-only" object.
		}
	}

	@Test
	public void testCommitChangesRO4() throws Exception {
		createArtistsDataSet();

		ROArtist a1 = ObjectSelect.query(ROArtist.class, Artist.ARTIST_NAME.eq("artist1")).selectOne(context);

		Painting painting = context.newObject(Painting.class);
		painting.setPaintingTitle("paint");
		a1.addToManyTarget("paintingArray", painting, true);

		assertEquals(PersistenceState.MODIFIED, a1.getPersistenceState());
		try {
			context.commitChanges();
		} catch (Exception ex) {
			fail("Updating 'read-only' object's to-many must succeed, instead an exception was thrown: " + ex);
		}

		assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
	}

	/**
	 * Tests that hasChanges performs correctly when an object is "modified" and
	 * the property is simply set to the same value (an unreal modification)
	 */
	@Test
	public void testHasChangesPhantom() {

		String artistName = "ArtistName";
		Artist artist = (Artist) context.newObject("Artist");
		artist.setArtistName(artistName);
		context.commitChanges();

		// Set again to *exactly* the same value
		artist.setArtistName(artistName);

		// note that since 1.2 the polciy is for hasChanges to return true for
		// phantom
		// modifications, as there is no way to detect some more subtle
		// modifications like
		// a change of the master related object, until we actually create the
		// PKs
		assertTrue(context.hasChanges());
	}

	/**
	 * Tests that hasChanges performs correctly when an object is "modified" and
	 * the property is simply set to the same value (an unreal modification)
	 */
	@Test
	public void testHasChangesRealModify() {
		Artist artist = (Artist) context.newObject("Artist");
		artist.setArtistName("ArtistName");
		context.commitChanges();

		artist.setArtistName("Something different");
		assertTrue(context.hasChanges());
	}

	@Test
	public void testInvalidateObjects_Vararg() throws Exception {

		DataRow row = new DataRow(10);
		row.put("ARTIST_ID", 1);
		row.put("ARTIST_NAME", "ArtistXYZ");
		row.put("DATE_OF_BIRTH", new Date());
		Persistent object = context.objectFromDataRow(Artist.class, row);
		ObjectId oid = object.getObjectId();

		// insert object into the ObjectStore
		context.getObjectStore().registerNode(oid, object);

		assertSame(object, context.getObjectStore().getNode(oid));
		assertNotNull(context.getObjectStore().getCachedSnapshot(oid));

		context.invalidateObjects(object);

		assertSame(oid, object.getObjectId());
		assertNull(context.getObjectStore().getCachedSnapshot(oid));
		assertSame(object, context.getObjectStore().getNode(oid));
	}

	@Test
	public void testInvalidateObjects() throws Exception {

		DataRow row = new DataRow(10);
		row.put("ARTIST_ID", 1);
		row.put("ARTIST_NAME", "ArtistXYZ");
		row.put("DATE_OF_BIRTH", new Date());
		Persistent object = context.objectFromDataRow(Artist.class, row);
		ObjectId oid = object.getObjectId();

		// insert object into the ObjectStore
		context.getObjectStore().registerNode(oid, object);

		assertSame(object, context.getObjectStore().getNode(oid));
		assertNotNull(context.getObjectStore().getCachedSnapshot(oid));

		context.invalidateObjects(Collections.singleton(object));

		assertSame(oid, object.getObjectId());
		assertNull(context.getObjectStore().getCachedSnapshot(oid));
		assertSame(object, context.getObjectStore().getNode(oid));
	}

	@Test
	public void testBeforeHollowDeleteShouldChangeStateToCommited() throws Exception {
		createSingleArtistDataSet();

		Artist hollow = Cayenne.objectForPK(context, Artist.class, 33001);
		context.invalidateObjects(hollow);
		assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());

		// testing this...
		context.deleteObjects(hollow);
		assertSame(hollow, context.getGraphManager().getNode(ObjectId.of("Artist", "ARTIST_ID", 33001)));
		assertEquals("artist1", hollow.getArtistName());

		assertEquals(PersistenceState.DELETED, hollow.getPersistenceState());
	}

	@Test
	public void testCommitUnchangedInsert() throws Exception {

		// see CAY-1444 - reproducible on DB's that support auto incremented PK

		NullTestEntity newObject = context.newObject(NullTestEntity.class);

		assertTrue(context.hasChanges());
		context.commitChanges();
		assertFalse(context.hasChanges());

		assertEquals(PersistenceState.COMMITTED, newObject.getPersistenceState());
	}
}
