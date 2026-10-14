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
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.event.MockEventManager;

import java.util.Map;

/**
 * A "lightweight" DataRowStore.
 */
public class MockDataRowStore extends DataRowStore {

    private static final String NAME = "mock DataRowStore";
    private static final int MAX_SIZE = 10;

    public MockDataRowStore() {
        super(NAME, MAX_SIZE, new MockEventManager());
    }

    /**
     * A backdoor to add test snapshots.
     */
    public void putSnapshot(ObjectId id, DataRow snapshot) {
        snapshots.put(id, snapshot);
    }

    public void putSnapshot(ObjectId id, Map<String, ?> snapshot) {
        String[] names = new String[snapshot.size()];
        Object[] values = new Object[names.length];
        int i = 0;
        for (Map.Entry<String, ?> entry : snapshot.entrySet()) {
            names[i] = entry.getKey();
            values[i++] = entry.getValue();
        }
        snapshots.put(id, DataRow.of(id.getEntityName(), DataRowLayout.of(names), values, SnapshotVersion.next()));
    }

    public void putEmptySnapshot(ObjectId id) {
        snapshots.put(id, DataRow.of(id.getEntityName(), DataRowLayout.of(), new Object[0], SnapshotVersion.next()));
    }

}
