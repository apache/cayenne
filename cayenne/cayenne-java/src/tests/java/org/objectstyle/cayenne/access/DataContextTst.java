/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.access;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.ArtistAssets;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.ROArtist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.query.ObjectIdQuery;

public class DataContextTst extends DataContextTestBase {

    protected MockOperationObserver opObserver;

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

    /**
     * Test fetching a derived entity.
     */
    public void testDerivedEntityFetch1() throws Exception {

        // some DBs don't support HAVING
        if (!getAccessStackAdapter().supportsHaving()) {
            return;
        }

        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("ArtistAssets");
        q
                .setQualifier(ExpressionFactory.matchExp(
                        "estimatedPrice",
                        new BigDecimal(1000)));

        ArtistAssets a1 = (ArtistAssets) context.performQuery(q).get(0);
        assertEquals(1, a1.getPaintingsCount().intValue());
    }

    /**
     * Test fetching a derived entity with complex qualifier including relationships.
     */
    public void testDerivedEntityFetch2() throws Exception {
        // some DBs don't support HAVING
        if (!getAccessStackAdapter().supportsHaving()) {
            return;
        }

        createTestData("testPaintings");

        SelectQuery q = new SelectQuery("ArtistAssets");
        q.setParentObjEntityName("Painting");
        q
                .andQualifier(ExpressionFactory.matchExp(
                        "estimatedPrice",
                        new BigDecimal(1000)));
        q
                .andParentQualifier(ExpressionFactory.matchExp(
                        "toArtist.artistName",
                        "artist1"));

        ArtistAssets a1 = (ArtistAssets) context.performQuery(q).get(0);
        assertEquals(1, a1.getPaintingsCount().intValue());
    }

    /**
     * @deprecated since 1.2 as 'performQueries' is deprecated.
     */
    public void testPerformQueries() throws Exception {
        createTestData("testGalleries");

        SelectQuery q1 = new SelectQuery();
        q1.setRoot(Artist.class);
        SelectQuery q2 = new SelectQuery();
        q2.setRoot(Gallery.class);

        List qs = new ArrayList();
        qs.add(q1);
        qs.add(q2);
        context.performQueries(qs, opObserver);

        // check query results
        List o1 = opObserver.rowsForQuery(q1);
        assertNotNull(o1);
        assertEquals(artistCount, o1.size());

        List o2 = opObserver.rowsForQuery(q2);
        assertNotNull(o2);
        assertEquals(galleryCount, o2.size());
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

        assertTrue(((IncrementalFaultList) objects).elements.get(0) instanceof Artist);
        assertTrue(((IncrementalFaultList) objects).elements.get(7) instanceof Map);
    }
    
    public void testPerformPaginatedQueryBigPage() throws Exception {
        SelectQuery query = new SelectQuery("Artist");
        query.setPageSize(DataContextTst.artistCount + 2);
        List objects = context.performQuery(query);
        assertNotNull(objects);
        assertTrue(objects instanceof IncrementalFaultList);

        blockQueries();
        try {
            assertEquals(DataContextTst.artistCount, objects.size());
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
        ROArtist a1 = (ROArtist) context.createAndRegisterNewObject("ROArtist");
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
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
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
                it.nextDataRow();
                count++;
            }

            assertEquals(DataContextTst.artistCount, count);
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
                DataRow row = (DataRow) it.nextDataRow();

                // try instantiating an object and fetching its relationships
                Artist artist = (Artist) context.objectFromDataRow(
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
        DataNode node = (DataNode) context
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
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
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
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("ArtistName");
        context.commitChanges();

        artist.setArtistName("Something different");
        assertTrue(context.hasChanges());
    }

}