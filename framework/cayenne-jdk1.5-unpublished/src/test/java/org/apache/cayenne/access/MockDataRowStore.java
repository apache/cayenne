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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.event.MockEventManager;

/**
 * A "lightweight" DataRowStore.
 */
public class MockDataRowStore extends DataRowStore {

    private static final Map TEST_DEFAULTS = new HashMap();

    static {
        TEST_DEFAULTS.put(DataRowStore.SNAPSHOT_CACHE_SIZE_PROPERTY, new Integer(10));
        TEST_DEFAULTS.put(DataRowStore.REMOTE_NOTIFICATION_PROPERTY, Boolean.FALSE);
    }

    public MockDataRowStore() {
        super("mock DataRowStore", TEST_DEFAULTS, new MockEventManager());
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
