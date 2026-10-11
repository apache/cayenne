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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.event.SnapshotEvent;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * We pass null as EventManager parameter, as having it not null will start
 * really heavy EventBridge (JavaGroupsBridge implementation) inside DataRowStore
 * and this behaviour is not anyhow tested here nor it affects existing tests.
 */
public class DataRowStoreIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);


    private DataRowStore cache;

    
    public void cleanDataStore() {
        if(cache != null) {
            cache.shutdown();
            cache = null;
        }
    }

    @Test
    public void defaultConstructor() {
        cache = new DataRowStore("cacheXYZ", DefaultDataRowStoreFactory.SNAPSHOT_CACHE_SIZE_DEFAULT, null);
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(cache.getSnapshotEventSubject().getSubjectName().contains("cacheXYZ"));
    }

    /**
     * Tests LRU cache behavior.
     */
    @Test
    public void maxSize() throws Exception {
        cache = new DataRowStore("cacheXYZ", 2, null);
        assertEquals(2, cache.maximumSize());
        assertEquals(0, cache.size());

        ObjectId key1 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        Map<ObjectId, DataRow> diff1 = new HashMap<>();
        diff1.put(key1, new DataRow("Artist", new HashMap<>(1), SnapshotVersion.next()));

        ObjectId key2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<ObjectId, DataRow> diff2 = new HashMap<>();
        diff2.put(key2, new DataRow("Artist", new HashMap<>(1), SnapshotVersion.next()));

        ObjectId key3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Map<ObjectId, DataRow> diff3 = new HashMap<>();
        diff3.put(key3, new DataRow("Artist", new HashMap<>(1), SnapshotVersion.next()));

        cache.processSnapshotChanges(
                this,
                diff1,
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList());
        assertEquals(1, cache.size());

        cache.processSnapshotChanges(
                this,
                diff2,
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList());
        assertEquals(2, cache.size());

        // this addition must overflow the cache, and throw out the first item
        cache.processSnapshotChanges(
                this,
                diff3,
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList(),
                Collections.<ObjectId>emptyList());
        assertEquals(2, cache.size());
        assertNotNull(cache.getCachedSnapshot(key2));
        assertNotNull(cache.getCachedSnapshot(key3));
        assertNull(cache.getCachedSnapshot(key1));
    }

    /**
     * An updated snapshot that replaces the cached one is broadcast as a diff: only the changed keys, with the new
     * values and the new snapshot's version. An unchanged snapshot is not broadcast at all.
     */
    @Test
    public void updatedSnapshotDiff() {
        DefaultEventManager eventManager = new DefaultEventManager();
        try {
            cache = new DataRowStore("cacheXYZ", 10, eventManager);
            List<SnapshotEvent> events = new ArrayList<>();
            eventManager.addListener(events, SnapshotEvent.class, List::add, cache.getSnapshotEventSubject());

            ObjectId key = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
            DataRow old = new DataRow("Artist",
                    new HashMap<>(Map.of("ARTIST_ID", 1, "ARTIST_NAME", "a", "DATE_OF_BIRTH", "d")),
                    SnapshotVersion.next());
            cache.processSnapshotChanges(this, Map.of(key, old), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());
            assertTrue(events.isEmpty(), "a new snapshot is not an update");

            DataRow same = new DataRow("Artist", new HashMap<>(old), SnapshotVersion.next());
            same.setReplacesVersion(old.getVersion());
            cache.processSnapshotChanges(this, Map.of(key, same), Collections.emptyList(), Collections.emptyList(),
                    Collections.emptyList());
            assertTrue(events.isEmpty(), "an unchanged snapshot is not broadcast");

            DataRow changed = new DataRow("Artist",
                    new HashMap<>(Map.of("ARTIST_ID", 1, "ARTIST_NAME", "b", "DATE_OF_BIRTH", "d")),
                    SnapshotVersion.next());
            changed.setReplacesVersion(same.getVersion());
            cache.processSnapshotChanges(this, Map.of(key, changed), Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
            assertEquals(1, events.size());

            DataRow diff = events.getFirst().getModifiedDiffs().get(key);
            assertEquals(Map.of("ARTIST_NAME", "b"), diff);
            assertEquals(changed.getVersion(), diff.getVersion());
        } finally {
            eventManager.shutdown();
        }
    }
}
