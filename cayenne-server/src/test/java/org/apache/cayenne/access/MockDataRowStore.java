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
import org.apache.cayenne.event.MockEventManager;

import java.util.HashMap;
import java.util.Map;

/**
 * A "lightweight" DataRowStore.
 */
public class MockDataRowStore extends DataRowStore {

    private static final Map<String, String> TEST_DEFAULTS = new HashMap<>();

    static {
        TEST_DEFAULTS.put(Constants.SNAPSHOT_CACHE_SIZE_PROPERTY, Integer.toString(10));
    }

    public MockDataRowStore() {
        super("mock DataRowStore", new DefaultRuntimeProperties(TEST_DEFAULTS), new MockEventManager());
    }

    /**
     * A backdoor to add test snapshots.
     */
    public void putSnapshot(ObjectId id, DataRow snapshot) {
        snapshots.put(id, snapshot);
    }

    public void putSnapshot(ObjectId id, Map<String, ?> snapshot) {
        snapshots.put(id, new DataRow(snapshot));
    }

    public void putEmptySnapshot(ObjectId id) {
        snapshots.put(id, new DataRow(2));
    }

}
