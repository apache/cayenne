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

package org.apache.cayenne.access;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.ResultIteratorCallback;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.NullTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.ROArtist;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected UnitDbAdapter accessStackAdapter;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    @Inject
    protected ServerCaseDataSourceFactory dataSourceFactory;

    protected TableHelper tArtist;
    protected TableHelper tExhibit;
    protected TableHelper tGallery;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
        dbHelper.deleteAll("EXHIBIT");
        dbHelper.deleteAll("GALLERY");

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

    public void testCurrentSnapshot1() throws Exception {
        createSingleArtistDataSet();

        SelectQuery query = new SelectQuery(Artist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        Artist artist = (Artist) context.performQuery(query).get(0);

        DataRow snapshot = context.currentSnapshot(artist);
        assertEquals(artist.getArtistName(), snapshot.get("ARTIST_NAME"));
        assertEquals(artist.getDateOfBirth(), snapshot.get("DATE_OF_BIRTH"));
    }

    public void testCurrentSnapshot2() throws Exception {
        createSingleArtistDataSet();

        // test null values
        SelectQuery query = new SelectQuery(Artist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        Artist artist = (Artist) context.performQuery(query).get(0);

        artist.setArtistName(null);
        artist.setDateOfBirth(null);

        DataRow snapshot = context.currentSnapshot(artist);
        assertTrue(snapshot.containsKey("ARTIST_NAME"));
        assertNull(snapshot.get("ARTIST_NAME"));

        assertTrue(snapshot.containsKey("DATE_OF_BIRTH"));
        assertNull(snapshot.get("DATE_OF_BIRTH"));
    }

    public void testCurrentSnapshot3() throws Exception {
        createSingleArtistDataSet();

        // test null values
        SelectQuery query = new SelectQuery(Artist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        Artist artist = (Artist) context.performQuery(query).get(0);

        // test FK relationship snapshotting
        Painting p1 = new Painting();
        context.registerNewObject(p1);
        p1.setToArtist(artist);

        DataRow s1 = context.currentSnapshot(p1);
        Map<String, Object> idMap = artist.getObjectId().getIdSnapshot();
        assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
    }

    /**
     * Testing snapshot with to-one fault. This was a bug CAY-96.
     */
    public void testCurrentSnapshotWithToOneFault() throws Exception {

        createGalleriesAndExhibitsDataSet();

        // Exhibit with Gallery as Fault must still include Gallery
        // Artist and Exhibit (Exhibit has unresolved to-one to gallery as in
        // the
        // CAY-96 bug report)

        ObjectId eId = new ObjectId("Exhibit", Exhibit.EXHIBIT_ID_PK_COLUMN, 2);
        Exhibit e = (Exhibit) context.performQuery(new ObjectIdQuery(eId)).get(0);

        assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY_PROPERTY) instanceof Fault);

        DataRow snapshot = context.currentSnapshot(e);

        // assert that after taking a snapshot, we have FK in, but the
        // relationship
        // is still a Fault
        assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY_PROPERTY) instanceof Fault);
        assertEquals(new Integer(33002), snapshot.get("GALLERY_ID"));
    }

    /**
     * Tests how CHAR field is handled during fetch. Some databases (Oracle...)
     * would pad a CHAR column with extra spaces, returned to the client.
     * Cayenne should trim it.
     */
    public void testCharFetch() throws Exception {
        createSingleArtistDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        Artist a = (Artist) context.performQuery(query).get(0);
        assertEquals(a.getArtistName().trim(), a.getArtistName());
    }

    /**
     * Tests how CHAR field is handled during fetch in the WHERE clause. Some
     * databases (Oracle...) would pad a CHAR column with extra spaces, returned
     * to the client. Cayenne should trim it.
     */
    public void testCharInQualifier() throws Exception {
        createArtistsDataSet();

        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery q = new SelectQuery(Artist.class, e);
        List<Artist> artists = context.performQuery(q);
        assertEquals(1, artists.size());
    }

    /**
     * Test fetching query with multiple relationship paths between the same 2
     * entities used in qualifier.
     */
    public void testMultiObjRelFetch() throws Exception {
        createArtistsAndPaintingsDataSet();

        SelectQuery q = new SelectQuery(Painting.class);
        q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist2"));
        q.orQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist4"));
        List<Painting> results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    /**
     * Test fetching query with multiple relationship paths between the same 2
     * entities used in qualifier.
     */
    public void testMultiDbRelFetch() throws Exception {
        createArtistsAndPaintingsDataSet();

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist2"));
        q.orQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist4"));
        List<?> results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    public void testSelectDate() throws Exception {
        createGalleriesAndExhibitsDataSet();

        List<Exhibit> objects = context.performQuery(new SelectQuery(Exhibit.class));
        assertFalse(objects.isEmpty());

        Exhibit e1 = objects.get(0);
        assertEquals(java.util.Date.class, e1.getClosingDate().getClass());
    }

    public void testCaseInsensitiveOrdering() throws Exception {
        if (!accessStackAdapter.supportsCaseInsensitiveOrder()) {
            return;
        }

        createFiveArtistDataSet_MixedCaseName();

        // case insensitive ordering appends extra columns
        // to the query when query is using DISTINCT...
        // verify that the result is not messed up

        SelectQuery query = new SelectQuery(Artist.class);
        Ordering ordering = new Ordering(Artist.ARTIST_NAME_PROPERTY, SortOrder.ASCENDING_INSENSITIVE);
        query.addOrdering(ordering);
        query.setDistinct(true);

        List<Artist> objects = context.performQuery(query);
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

    public void testSelect_DataRows() throws Exception {
        createArtistsAndPaintingsDataSet();

        SelectQuery<DataRow> query = SelectQuery.dataRowQuery(Artist.class, null);
        List<DataRow> objects = context.select(query);

        assertNotNull(objects);
        assertEquals(7, objects.size());
        assertTrue("DataRow expected, got " + objects.get(0).getClass(), objects.get(0) instanceof DataRow);
    }

    public void testPerformSelectQuery1() throws Exception {
        createArtistsAndPaintingsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(7, objects.size());
        assertTrue("Artist expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Artist);
    }

    public void testPerformSelectQuery2() throws Exception {
        createArtistsAndPaintingsDataSet();

        // do a query with complex qualifier
        List<Expression> expressions = new ArrayList<Expression>();
        expressions.add(ExpressionFactory.matchExp("artistName", "artist3"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist5"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist21"));

        SelectQuery query = new SelectQuery(Artist.class, ExpressionFactory.joinExp(Expression.OR, expressions));

        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue("Artist expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Artist);
    }

    public void testPerformQuery_Routing() {
        Query query = mock(Query.class);
        QueryMetadata md = mock(QueryMetadata.class);
        when(query.getMetaData(any(EntityResolver.class))).thenReturn(md);
        context.performGenericQuery(query);
        verify(query).route(any(QueryRouter.class), eq(context.getEntityResolver()), (Query) isNull());
    }

    public void testPerformNonSelectingQuery() throws Exception {

        createSingleArtistDataSet();

        SelectQuery select = new SelectQuery(Painting.class, Expression.fromString("db:PAINTING_ID = 1"));

        assertEquals(0, context.performQuery(select).size());

        SQLTemplate query = new SQLTemplate(Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES (1, 'PX', 33001, 1)");
        context.performNonSelectingQuery(query);
        assertEquals(1, context.performQuery(select).size());
    }

    public void testPerformNonSelectingQueryCounts1() throws Exception {
        createArtistsDataSet();

        SQLTemplate query = new SQLTemplate(Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES ($pid, '$pt', $aid, $price)");

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("pid", new Integer(1));
        map.put("pt", "P1");
        map.put("aid", new Integer(33002));
        map.put("price", new Double(1.1));

        // single batch of parameters
        query.setParameters(map);

        int[] counts = context.performNonSelectingQuery(query);
        assertNotNull(counts);
        assertEquals(1, counts.length);
        assertEquals(1, counts[0]);
    }

    public void testPerformNonSelectingQueryCounts2() throws Exception {

        createArtistsDataSet();

        SQLTemplate query = new SQLTemplate(Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES ($pid, '$pt', $aid, #bind($price 'DECIMAL' 2))");

        Map<String, Object>[] maps = new Map[3];
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new HashMap<String, Object>();
            maps[i].put("pid", new Integer(1 + i));
            maps[i].put("pt", "P-" + i);
            maps[i].put("aid", new Integer(33002));
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

    public void testPerformPaginatedQuery() throws Exception {
        createArtistsDataSet();

        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setPageSize(5);
        List<Artist> objects = context.select(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList<?>);
        assertTrue(((IncrementalFaultList<Artist>) objects).elements.get(0) instanceof Long);
        assertTrue(((IncrementalFaultList<Artist>) objects).elements.get(6) instanceof Long);

        assertTrue(objects.get(0) instanceof Artist);
    }

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

    public void testPerformPaginatedQueryBigPage() throws Exception {
        createArtistsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        query.setPageSize(5);
        final List<?> objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList<?>);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(7, objects.size());
            }
        });
    }

    public void testPerformDataRowQuery() throws Exception {

        createArtistsDataSet();

        SelectQuery query = new SelectQuery(Artist.class);
        query.setFetchingDataRows(true);
        List<?> objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(7, objects.size());
        assertTrue("Map expected, got " + objects.get(0).getClass(), objects.get(0) instanceof Map<?, ?>);
    }

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

    public void testCommitChangesRO2() throws Exception {
        createArtistsDataSet();

        SelectQuery query = new SelectQuery(ROArtist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        ROArtist a1 = (ROArtist) context.performQuery(query).get(0);
        a1.writeProperty(ROArtist.ARTIST_NAME_PROPERTY, "abc");

        try {
            context.commitChanges();
            fail("Updating a 'read-only' object must fail.");
        } catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO3() throws Exception {

        createArtistsDataSet();

        SelectQuery query = new SelectQuery(ROArtist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        ROArtist a1 = (ROArtist) context.performQuery(query).get(0);
        context.deleteObjects(a1);

        try {
            context.commitChanges();
            fail("Deleting a 'read-only' object must fail.");
        } catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO4() throws Exception {
        createArtistsDataSet();

        SelectQuery query = new SelectQuery(ROArtist.class, ExpressionFactory.matchExp(Artist.ARTIST_NAME_PROPERTY,
                "artist1"));
        ROArtist a1 = (ROArtist) context.performQuery(query).get(0);

        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("paint");
        a1.addToPaintingArray(painting);

        assertEquals(PersistenceState.MODIFIED, a1.getPersistenceState());
        try {
            context.commitChanges();
        } catch (Exception ex) {
            fail("Updating 'read-only' object's to-many must succeed, instead an exception was thrown: " + ex);
        }

        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testIterate() throws Exception {

        createArtistsDataSet();

        SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class);

        context.iterate(q1, new ResultIteratorCallback<Artist>() {
            public void iterate(ResultIterator<Artist> it) {
                int count = 0;

                for (Artist a : it) {
                    assertNotNull(a.getArtistName());
                    count++;
                }

                assertEquals(7, count);
            }
        });
    }

    public void testIterateDataRows() throws Exception {

        createArtistsDataSet();

        SelectQuery<DataRow> q1 = SelectQuery.dataRowQuery(Artist.class, null);

        context.iterate(q1, new ResultIteratorCallback<DataRow>() {
            public void iterate(ResultIterator<DataRow> it) {
                int count = 0;

                for (DataRow a : it) {
                    assertNotNull(a.get("ARTIST_ID"));
                    count++;
                }

                assertEquals(7, count);
            }
        });
    }

    public void testIterator() throws Exception {

        createArtistsDataSet();

        SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class);

        ResultIterator<Artist> it = context.iterator(q1);
        try {
            int count = 0;

            for (Artist a : it) {
                count++;
            }

            assertEquals(7, count);
        } finally {
            it.close();
        }
    }

    public void testPerformIteratedQuery1() throws Exception {

        createArtistsDataSet();

        SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class);
        ResultIterator<?> it = context.performIteratedQuery(q1);

        try {
            int count = 0;
            while (it.hasNextRow()) {
                it.nextRow();
                count++;
            }

            assertEquals(7, count);
        } finally {
            it.close();
        }
    }

    public void testPerformIteratedQuery2() throws Exception {
        createArtistsAndPaintingsDataSet();

        ResultIterator it = context.performIteratedQuery(new SelectQuery(Artist.class));

        // just for this test increase pool size
        changeMaxConnections(1);

        try {
            while (it.hasNextRow()) {
                DataRow row = (DataRow) it.nextRow();

                // try instantiating an object and fetching its relationships
                Artist artist = context.objectFromDataRow(Artist.class, row);
                List<?> paintings = artist.getPaintingArray();
                assertNotNull(paintings);
                assertEquals("Expected one painting for artist: " + artist, 1, paintings.size());
            }
        } finally {
            // change allowed connections back
            changeMaxConnections(-1);
            it.close();
        }
    }

    /**
     * Tests that hasChanges performs correctly when an object is "modified" and
     * the property is simply set to the same value (an unreal modification)
     */
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
    public void testHasChangesRealModify() {
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("ArtistName");
        context.commitChanges();

        artist.setArtistName("Something different");
        assertTrue(context.hasChanges());
    }

    public void testInvalidateObjects_Vararg() throws Exception {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row);
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

    public void testInvalidateObjects() throws Exception {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row);
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

    public void testBeforeHollowDeleteShouldChangeStateToCommited() throws Exception {
        createSingleArtistDataSet();

        Artist hollow = Cayenne.objectForPK(context, Artist.class, 33001);
        context.invalidateObjects(hollow);
        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());

        // testing this...
        context.deleteObjects(hollow);
        assertSame(hollow, context.getGraphManager().getNode(new ObjectId("Artist", "ARTIST_ID", 33001)));
        assertEquals("artist1", hollow.getArtistName());

        assertEquals(PersistenceState.DELETED, hollow.getPersistenceState());
    }

    public void testCommitUnchangedInsert() throws Exception {

        // see CAY-1444 - reproducible on DB's that support auto incremented PK

        NullTestEntity newObject = context.newObject(NullTestEntity.class);

        assertTrue(context.hasChanges());
        context.commitChanges();
        assertFalse(context.hasChanges());

        assertEquals(PersistenceState.COMMITTED, newObject.getPersistenceState());
    }

    private void changeMaxConnections(int delta) {
        PoolManager manager = (PoolManager) dataSourceFactory.getSharedDataSource();
        manager.setMaxConnections(manager.getMaxConnections() + delta);
    }
}
