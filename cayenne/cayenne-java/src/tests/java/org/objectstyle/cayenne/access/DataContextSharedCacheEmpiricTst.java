package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.util.ThreadedTestHelper;

/**
 * @author Andrei Adamchik
 */
public class DataContextSharedCacheEmpiricTst extends CayenneTestCase {

    private static final String NEW_NAME = "versionX";

    protected DataContext c1;
    protected DataContext c2;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();

        DataRowStore cache = new DataRowStore("cacheTest");

        c1 = new DataContext((DataChannel) getDomain(), new ObjectStore(cache));
        c2 = new DataContext((DataChannel) getDomain(), new ObjectStore(cache));

        // prepare a single artist record
        SQLTemplate insert = new SQLTemplate(
                Artist.class,
                "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'version1')");
        c1.performNonSelectingQuery(insert);
    }

    public void testSelectSelectCommitRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(true);

        // select both, a2 should go second...
        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectSelectCommitNoRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(false);

        // select both, a2 should go second...
        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        // Update Artist
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        assertEquals("version1", a1.getArtistName());
        a1.setArtistName(NEW_NAME);
        assertEquals(NEW_NAME, a1.getArtistName());
        assertEquals(PersistenceState.MODIFIED, a1.getPersistenceState());
        assertSame(c1, a1.getDataContext());
        assertTrue(c1.hasChanges());
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectSelectCommitRefreshReverse() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(true);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        assertFalse(a2 == a1);

        // Update Artist
        a1.setArtistName(NEW_NAME);
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectSelectCommitNoRefreshReverse() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(false);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        assertFalse(a2 == a1);

        // Update Artist
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
        assertEquals("version1", a1.getArtistName());
        a1.setArtistName(NEW_NAME);
        assertEquals(NEW_NAME, a1.getArtistName());
        assertEquals(PersistenceState.MODIFIED, a1.getPersistenceState());
        assertSame(c1, a1.getDataContext());
        assertTrue(c1.hasChanges());
        c1.commitChanges();

        assertOnCommit(a2);
    }

    public void testSelectUpdateSelectCommitRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(true);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        // Update Artist
        a1.setArtistName(NEW_NAME);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        c1.commitChanges();
        assertOnCommit(a2);
    }

    public void testSelectUpdateSelectCommitNoRefresh() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setRefreshingObjects(false);

        List artists = c1.performQuery(query);
        Artist a1 = (Artist) artists.get(0);

        // Update Artist
        a1.setArtistName(NEW_NAME);

        List altArtists = c2.performQuery(query);
        final Artist a2 = (Artist) altArtists.get(0);
        assertNotNull(a2);
        assertFalse(a2 == a1);

        c1.commitChanges();
        assertOnCommit(a2);
    }

    private void assertOnCommit(final Artist a2) throws Exception {
        // check underlying cache
        final DataRow freshSnapshot = c2
                .getObjectStore()
                .getDataRowCache()
                .getCachedSnapshot(a2.getObjectId());
        assertNotNull("No snapshot for artist", freshSnapshot);
        assertEquals(NEW_NAME, freshSnapshot.get("ARTIST_NAME"));

        // check peer artist
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            protected void assertResult() throws Exception {
                assertEquals(
                        "Snapshot change is not propagated: " + freshSnapshot,
                        NEW_NAME,
                        a2.getArtistName());
            }
        };
        helper.assertWithTimeout(3000);
    }
}