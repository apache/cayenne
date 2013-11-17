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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataRowStoreTest extends ServerCase {

    public void testDefaultConstructor() {
        DataRowStore cache = new DataRowStore(
                "cacheXYZ",
                Collections.EMPTY_MAP,
                new DefaultEventManager());
        assertEquals("cacheXYZ", cache.getName());
        assertNotNull(cache.getSnapshotEventSubject());
        assertTrue(cache.getSnapshotEventSubject().getSubjectName().contains("cacheXYZ"));

        assertEquals(DataRowStore.REMOTE_NOTIFICATION_DEFAULT, cache
                .isNotifyingRemoteListeners());
    }

    public void testConstructorWithProperties() {
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(DataRowStore.REMOTE_NOTIFICATION_PROPERTY, String
                .valueOf(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT));

        DataRowStore cache = new DataRowStore(
                "cacheXYZ",
                props,
                new DefaultEventManager());
        assertEquals("cacheXYZ", cache.getName());
        assertEquals(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT, cache
                .isNotifyingRemoteListeners());
    }

    public void testNotifyingRemoteListeners() {
        DataRowStore cache = new DataRowStore(
                "cacheXYZ",
                Collections.EMPTY_MAP,
                new DefaultEventManager());

        assertEquals(DataRowStore.REMOTE_NOTIFICATION_DEFAULT, cache
                .isNotifyingRemoteListeners());

        cache.setNotifyingRemoteListeners(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT);
        assertEquals(!DataRowStore.REMOTE_NOTIFICATION_DEFAULT, cache
                .isNotifyingRemoteListeners());
    }

    /**
     * Tests LRU cache behavior.
     */
    public void testMaxSize() throws Exception {
        Map<Object, Object> props = new HashMap<Object, Object>();
        props.put(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, String.valueOf(2));

        DataRowStore cache = new DataRowStore(
                "cacheXYZ",
                props,
                new DefaultEventManager());
        assertEquals(2, cache.maximumSize());
        assertEquals(0, cache.size());

        ObjectId key1 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        Map<Object, Object> diff1 = new HashMap<Object, Object>();
        diff1.put(key1, new DataRow(1));

        ObjectId key2 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 2);
        Map<Object, Object> diff2 = new HashMap<Object, Object>();
        diff2.put(key2, new DataRow(1));

        ObjectId key3 = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 3);
        Map<Object, Object> diff3 = new HashMap<Object, Object>();
        diff3.put(key3, new DataRow(1));

        cache.processSnapshotChanges(
                this,
                diff1,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
        assertEquals(1, cache.size());

        cache.processSnapshotChanges(
                this,
                diff2,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
        assertEquals(2, cache.size());

        // this addition must overflow the cache, and throw out the first item
        cache.processSnapshotChanges(
                this,
                diff3,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST,
                Collections.EMPTY_LIST);
        assertEquals(2, cache.size());
        assertNotNull(cache.getCachedSnapshot(key2));
        assertNotNull(cache.getCachedSnapshot(key3));
        assertNull(cache.getCachedSnapshot(key1));
    }
}
