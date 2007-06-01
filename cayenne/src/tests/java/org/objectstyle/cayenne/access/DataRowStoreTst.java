package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataRowStoreTst extends CayenneTestCase {

    public void testDefaultConstructor() {
        DataRowStore cache = new DataRowStore("cacheXYZ");
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(
            cache.getSnapshotEventSubject().getSubjectName().indexOf("cacheXYZ") >= 0);

        assertEquals(
            DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testConstructorWithProperties() {
        Map props = new HashMap();
        props.put(
            DataRowStore.REMOTE_NOTIFICATION_PROPERTY,
            String.valueOf(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT));

        DataRowStore cache = new DataRowStore("cacheXYZ", props);
        assertEquals("cacheXYZ", cache.getName());
        assertEquals(
            !DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    public void testNotifyingRemoteListeners() {
        DataRowStore cache = new DataRowStore("cacheXYZ");

        assertEquals(
            DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());

        cache.setNotifyingRemoteListeners(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT);
        assertEquals(
            !DataRowStore.REMOTE_NOTIFICATION_DEFAULT,
            cache.isNotifyingRemoteListeners());
    }

    /**
     * Tests LRU cache behavior.
     */
    public void testMaxSize() throws Exception {
        Map props = new HashMap();
        props.put(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, String.valueOf(2));

        DataRowStore cache = new DataRowStore("cacheXYZ", props);
        assertEquals(2, cache.maximumSize());
        assertEquals(0, cache.size());

        ObjectId key1 = new ObjectId(Artist.class, Artist.ARTIST_ID_PK_COLUMN, 1);
        Map diff1 = new HashMap();
        diff1.put(key1, new DataRow(1));

        ObjectId key2 = new ObjectId(Artist.class, Artist.ARTIST_ID_PK_COLUMN, 2);
        Map diff2 = new HashMap();
        diff2.put(key2, new DataRow(1));

        ObjectId key3 = new ObjectId(Artist.class, Artist.ARTIST_ID_PK_COLUMN, 3);
        Map diff3 = new HashMap();
        diff3.put(key3, new DataRow(1));

        cache.processSnapshotChanges(this, diff1, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        assertEquals(1, cache.size());

        cache.processSnapshotChanges(this, diff2, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        assertEquals(2, cache.size());

        // this addition must overflow the cache, and throw out the first item
        cache.processSnapshotChanges(this, diff3, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        assertEquals(2, cache.size());
        assertNotNull(cache.getCachedSnapshot(key2));
        assertNotNull(cache.getCachedSnapshot(key3));
        assertNull(cache.getCachedSnapshot(key1));
    }
    
    public void testCachedSnapshots() throws Exception {
        DataRowStore resultStore = new DataRowStore("test");

        List list = new ArrayList();

        assertNull(resultStore.getCachedSnapshots("key"));

        resultStore.cacheSnapshots("key", list);
        assertSame(list, resultStore.getCachedSnapshots("key"));
    }
}
