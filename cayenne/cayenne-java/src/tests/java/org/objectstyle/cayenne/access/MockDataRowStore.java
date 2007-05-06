package org.objectstyle.cayenne.access;

import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;

/**
 * A "lightweight" DataRowStore.
 * 
 * @author Andrus Adamchik
 */
public class MockDataRowStore extends DataRowStore {

    private static final Map TEST_DEFAULTS = new HashMap();

    static {
        TEST_DEFAULTS.put(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, new Integer(10));
        TEST_DEFAULTS.put(DataRowStore.REMOTE_NOTIFICATION_PROPERTY, Boolean.FALSE);
    }

    public MockDataRowStore() {
        super("mock DataRowStore", TEST_DEFAULTS);
    }

    /**
     * A backdoor to add test snapshots.
     */
    public void putSnapshot(ObjectId id, DataRow snapshot) {
        snapshots.put(id, snapshot);
    }

    public void putSnapshot(ObjectId id, Map snapshot) {
        snapshots.put(id, new DataRow(snapshot));
    }

    public void putEmptySnapshot(ObjectId id) {
        snapshots.put(id, new DataRow(2));
    }

}
