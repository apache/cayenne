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
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * We pass null as EventManager parameter, as having it not null will start
 * really heavy EventBridge (JavaGroupsBridge implementation) inside DataRowStore
 * and this behaviour is not anyhow tested here nor it affects existing tests.
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataRowStoreIT extends RuntimeCase {

    private DataRowStore cache;

    @After
    public void cleanDataStore() {
        if(cache != null) {
            cache.shutdown();
            cache = null;
        }
    }

    @Test
    public void testDefaultConstructor() {
        cache = new DataRowStore(
                "cacheXYZ",
                new DefaultRuntimeProperties(Collections.<String, String>emptyMap()),
                null);
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(cache.getSnapshotEventSubject().getSubjectName().contains("cacheXYZ"));
    }

    /**
     * Tests LRU cache behavior.
     */
    @Test
    public void testMaxSize() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put(Constants.SNAPSHOT_CACHE_SIZE_PROPERTY, String.valueOf(2));

        cache = new DataRowStore(
                "cacheXYZ",
                new DefaultRuntimeProperties(props),
                null);
        assertEquals(2, cache.maximumSize());
        assertEquals(0, cache.size());

        ObjectId key1 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        Map<ObjectId, DataRow> diff1 = new HashMap<>();
        diff1.put(key1, new DataRow(1));

        ObjectId key2 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<ObjectId, DataRow> diff2 = new HashMap<>();
        diff2.put(key2, new DataRow(1));

        ObjectId key3 = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Map<ObjectId, DataRow> diff3 = new HashMap<>();
        diff3.put(key3, new DataRow(1));

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
}
