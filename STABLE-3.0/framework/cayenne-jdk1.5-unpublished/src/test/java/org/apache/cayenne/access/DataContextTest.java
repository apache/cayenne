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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Exhibit;
import org.apache.art.Painting;
import org.apache.art.ROArtist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.Fault;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

public class DataContextTest extends DataContextCase {

    protected MockOperationObserver opObserver;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        opObserver = new MockOperationObserver();
    }

    public void testCurrentSnapshot1() throws Exception {
        Artist artist = fetchArtist("artist1", false);
        Map snapshot = context.currentSnapshot(artist);
        assertEquals(artist.getArtistName(), snapshot.get("ARTIST_NAME"));
        assertEquals(artist.getDateOfBirth(), snapshot.get("DATE_OF_BIRTH"));
    }

    public void testCurrentSnapshot2() throws Exception {
        // test null values
        Artist artist = fetchArtist("artist1", false);
        artist.setArtistName(null);
        artist.setDateOfBirth(null);

        Map snapshot = context.currentSnapshot(artist);
        assertTrue(snapshot.containsKey("ARTIST_NAME"));
        assertNull(snapshot.get("ARTIST_NAME"));

        assertTrue(snapshot.containsKey("DATE_OF_BIRTH"));
        assertNull(snapshot.get("DATE_OF_BIRTH"));
    }

    public void testCurrentSnapshot3() throws Exception {
        // test FK relationship snapshotting
        Artist a1 = fetchArtist("artist1", false);

        Painting p1 = new Painting();
        context.registerNewObject(p1);
        p1.setToArtist(a1);

        Map s1 = context.currentSnapshot(p1);
        Map idMap = a1.getObjectId().getIdSnapshot();
        assertEquals(idMap.get("ARTIST_ID"), s1.get("ARTIST_ID"));
    }

    /**
     * Testing snapshot with to-one fault. This was a bug CAY-96.
     */
    public void testCurrentSnapshotWithToOneFault() throws Exception {

        // Exhibit with Gallery as Fault must still include Gallery
        // Artist and Exhibit (Exhibit has unresolved to-one to gallery as in the
        // CAY-96 bug report)

        // first prepare test fixture
        createTestData("testGalleries");
        populateExhibits();

        ObjectId eId = new ObjectId("Exhibit", Exhibit.EXHIBIT_ID_PK_COLUMN, 2);
        Exhibit e = (Exhibit) context.performQuery(new ObjectIdQuery(eId)).get(0);

        assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY_PROPERTY) instanceof Fault);

        DataRow snapshot = context.currentSnapshot(e);

        // assert that after taking a snapshot, we have FK in, but the relationship
        // is still a Fault
        assertTrue(e.readPropertyDirectly(Exhibit.TO_GALLERY_PROPERTY) instanceof Fault);
        assertEquals(new Integer(33002), snapshot.get("GALLERY_ID"));
    }

    /**
     * Tests how CHAR field is handled during fetch. Some databases (Oracle...) would pad
     * a CHAR column with extra spaces, returned to the client. Cayenne should trim it.
     */
    public void testCharFetch() throws Exception {
        SelectQuery q = new SelectQuery("Artist");
        List artists = context.performQuery(q);
        Artist a = (Artist) artists.get(0);
        assertEquals(a.getArtistName().trim(), a.getArtistName());
    }

    /**
     * Tests how CHAR field is handled during fetch in the WHERE clause. Some databases
     * (Oracle...) would pad a CHAR column with extra spaces, returned to the client.
     * Cayenne should trim it.
     */
    public void testCharInQualifier() throws Exception {
        Expression e = ExpressionFactory.matchExp("artistName", "artist1");
        SelectQuery q = new SelectQuery("Artist", e);
        List artists = context.performQuery(q);
        assertEquals(1, artists.size());
    }

    /**
     * Test fetching query with multiple relationship paths between the same 2 entities
     * used in qualifier.
     */
    public void testMultiObjRelFetch() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist2"));
        q.orQualifier(ExpressionFactory.matchExp("toArtist.artistName", "artist4"));
        List results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    /**
     * Test fetching query with multiple relationship paths between the same 2 entities
     * used in qualifier.
     */
    public void testMultiDbRelFetch() throws Exception {
        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("Painting");
        q.andQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist2"));
        q.orQualifier(ExpressionFactory.matchDbExp("toArtist.ARTIST_NAME", "artist4"));
        List results = context.performQuery(q);

        assertEquals(2, results.size());
    }

    public void testSelectDate() throws Exception {
        createTestData("testGalleries");
        populateExhibits();

        List objects = context.performQuery(new SelectQuery(Exhibit.class));
        assertFalse(objects.isEmpty());

        Exhibit e1 = (Exhibit) objects.get(0);
        assertEquals(java.util.Date.class, e1.getClosingDate().getClass());
    }

    public void testCaseInsensitiveOrdering() throws Exception {
        if (!getAccessStackAdapter().supportsCaseInsensitiveOrder()) {
            return;
        }

        // case insensitive ordering appends extra columns
        // to the query when query is using DISTINCT...
        // verify that the result is not messaged up

        SelectQuery query = new SelectQuery(Artist.class);
        Ordering ordering = new Ordering("artistName", false);
        ordering.setCaseInsensitive(true);
        query.addOrdering(ordering);
        query.setDistinct(true);

        List objects = context.performQuery(query);
        assertEquals(artistCount, objects.size());

        Artist artist = (Artist) objects.get(0);
        Map snapshot = context.getObjectStore().getSnapshot(artist.getObjectId());
        assertEquals(3, snapshot.size());
    }

    public void testPerformSelectQuery1() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
        assertTrue(
                "Artist expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof Artist);
    }

    public void testPerformSelectQuery2() throws Exception {
        // do a query with complex qualifier
        List expressions = new ArrayList();
        expressions.add(ExpressionFactory.matchExp("artistName", "artist3"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist5"));
        expressions.add(ExpressionFactory.matchExp("artistName", "artist15"));

        SelectQuery query = new SelectQuery("Artist", ExpressionFactory.joinExp(
                Expression.OR,
                expressions));

        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(
                "Artist expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof Artist);
    }

    public void testPerformQuery() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
    }

    public void testPerformNonSelectingQuery() throws Exception {
        SelectQuery select = new SelectQuery(Painting.class, Expression
                .fromString("db:PAINTING_ID = 1"));

        assertEquals(0, context.performQuery(select).size());

        SQLTemplate query = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES (1, 'PX', 33002, 1)");
        context.performNonSelectingQuery(query);
        assertEquals(1, context.performQuery(select).size());
    }

    public void testPerformNonSelectingQueryCounts1() throws Exception {
        SQLTemplate query = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES ($pid, '$pt', $aid, $price)");

        Map map = new HashMap();
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
        SQLTemplate query = new SQLTemplate(
                Painting.class,
                "INSERT INTO PAINTING (PAINTING_ID, PAINTING_TITLE, ARTIST_ID, ESTIMATED_PRICE) "
                        + "VALUES ($pid, '$pt', $aid, #bind($price 'DECIMAL' 2))");

        Map[] maps = new Map[3];
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new HashMap();
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
        SelectQuery query = new SelectQuery("Artist");
        query.setPageSize(5);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList);
        assertTrue(((IncrementalFaultList<?>) objects).elements.get(0) instanceof Long);
        assertTrue(((IncrementalFaultList<?>) objects).elements.get(7) instanceof Long);
        
        assertTrue(objects.get(0) instanceof Artist);
    }
    
    public void testPerformPaginatedQuery1() throws Exception {
        EJBQLQuery query = new EJBQLQuery("select a FROM Artist a");
        query.setPageSize(5);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList);
        assertTrue(((IncrementalFaultList<?>) objects).elements.get(0) instanceof Long);
        assertTrue(((IncrementalFaultList<?>) objects).elements.get(7) instanceof Long);
        
        assertTrue(objects.get(0) instanceof Artist);
    }

    public void testPerformPaginatedQueryBigPage() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        query.setPageSize(DataContextTest.artistCount + 2);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList);

        blockQueries();
        try {
            assertEquals(DataContextTest.artistCount, objects.size());
        }
        finally {
            unblockQueries();
        }
    }

    public void testPerformDataRowQuery() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        query.setFetchingDataRows(true);
        List objects = context.performQuery(query);

        assertNotNull(objects);
        assertEquals(artistCount, objects.size());
        assertTrue(
                "Map expected, got " + objects.get(0).getClass(),
                objects.get(0) instanceof Map);
    }

    public void testCommitChangesRO1() throws Exception {
        ROArtist a1 = (ROArtist) context.newObject("ROArtist");
        a1.writePropertyDirectly("artistName", "abc");
        a1.setPersistenceState(PersistenceState.MODIFIED);

        try {
            context.commitChanges();
            fail("Inserting a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO2() throws Exception {
        ROArtist a1 = fetchROArtist("artist1");
        a1.writeProperty("artistName", "abc");

        try {
            context.commitChanges();
            fail("Updating a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO3() throws Exception {
        ROArtist a1 = fetchROArtist("artist1");
        context.deleteObject(a1);

        try {
            context.commitChanges();
            fail("Deleting a 'read-only' object must fail.");
        }
        catch (Exception ex) {
            // exception is expected,
            // must blow on saving new "read-only" object.
        }
    }

    public void testCommitChangesRO4() throws Exception {
        ROArtist a1 = fetchROArtist("artist1");
        Painting painting = (Painting) context.newObject("Painting");
        painting.setPaintingTitle("paint");
        a1.addToPaintingArray(painting);

        assertEquals(PersistenceState.MODIFIED, a1.getPersistenceState());
        try {
            context.commitChanges();
        }
        catch (Exception ex) {
            fail("Updating 'read-only' object's to-many must succeed, instead an exception was thrown: "
                    + ex);
        }

        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testPerformIteratedQuery1() throws Exception {
        SelectQuery q1 = new SelectQuery("Artist");
        ResultIterator it = context.performIteratedQuery(q1);

        try {
            int count = 0;
            while (it.hasNextRow()) {
                it.nextRow();
                count++;
            }

            assertEquals(DataContextTest.artistCount, count);
        }
        finally {
            it.close();
        }
    }

    public void testPerformIteratedQuery2() throws Exception {
        createTestData("testPaintings");

        ResultIterator it = context.performIteratedQuery(new SelectQuery(Artist.class));

        // just for this test increase pool size
        changeMaxConnections(1);

        try {
            while (it.hasNextRow()) {
                DataRow row = (DataRow) it.nextRow();

                // try instantiating an object and fetching its relationships
                Artist artist = context.objectFromDataRow(
                        Artist.class,
                        row,
                        false);
                List paintings = artist.getPaintingArray();
                assertNotNull(paintings);
                assertEquals("Expected one painting for artist: " + artist, 1, paintings
                        .size());
            }
        }
        finally {
            // change allowed connections back
            changeMaxConnections(-1);

            it.close();
        }
    }

    public void changeMaxConnections(int delta) {
        DataNode node = context
                .getParentDataDomain()
                .getDataNodes()
                .iterator()
                .next();

        // access DS directly as 'getDataSource' returns a wrapper.
        PoolManager manager = (PoolManager) node.dataSource;
        manager.setMaxConnections(manager.getMaxConnections() + delta);
    }

    /**
     * Tests that hasChanges performs correctly when an object is "modified" and the
     * property is simply set to the same value (an unreal modification)
     */
    public void testHasChangesPhantom() {
        String artistName = "ArtistName";
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        // Set again to *exactly* the same value
        artist.setArtistName(artistName);

        // note that since 1.2 the polciy is for hasChanges to return true for phantom
        // modifications, as there is no way to detect some more subtle modifications like
        // a change of the master related object, until we actually create the PKs
        assertTrue(context.hasChanges());
    }

    /**
     * Tests that hasChanges performs correctly when an object is "modified" and the
     * property is simply set to the same value (an unreal modification)
     */
    public void testHasChangesRealModify() {
        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("ArtistName");
        context.commitChanges();

        artist.setArtistName("Something different");
        assertTrue(context.hasChanges());
    }

    public void testInvalidateObjects() throws Exception {
        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = object.getObjectId();

        // insert object into the ObjectStore
        context.getObjectStore().registerNode(oid, object);

        assertSame(object, context.getObjectStore().getNode(oid));
        assertNotNull(context.getObjectStore().getCachedSnapshot(oid));

        context.invalidateObjects(Collections.singletonList(object));

        assertSame(oid, object.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertSame(object, context.getObjectStore().getNode(oid));
    }
    
    public void testBeforeHollowDeleteShouldChangeStateToCommited() throws Exception {
        ObjectId gid = new ObjectId("Artist","ARTIST_ID",33001);  
        final Artist inflated = new Artist();
        inflated.setPersistenceState(PersistenceState.COMMITTED);
        inflated.setObjectId(gid);
        inflated.setArtistName("artist1");
        
        Artist hollow = (Artist) context.localObject(gid, null);
        assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());

        // testing this...
        context.deleteObject(hollow);
        assertSame(hollow, context.getGraphManager().getNode(gid));
        assertEquals(inflated.getArtistName(), hollow.getArtistName());
          
        assertEquals(PersistenceState.DELETED, hollow.getPersistenceState());
    }

}
