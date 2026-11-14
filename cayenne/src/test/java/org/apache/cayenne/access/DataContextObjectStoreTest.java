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

import java.util.HashMap;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.MockDataChannel;
import org.apache.cayenne.unit.TestObjects;
import org.apache.cayenne.event.NoopEventManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DataContextObjectStoreTest {

    private DataContext context;
    private DataContextObjectStore objectStore;

    @BeforeEach
    public void before() {
        this.context = DataContext.builder(new MockDataChannel())
                .snapshotCache(new DataRowStore("test", 100, new NoopEventManager()))
                .objectMap(new HashMap<>())
                .build();
        this.objectStore = context.getObjectStore();
    }

    @Test
    public void registerObject() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = TestObjects.committedObject(id);

        objectStore.registerObject(id, object);
        assertSame(object, objectStore.getObject(id));
    }

    @Test
    public void unregisterObject() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = TestObjects.committedObject(id);
        object.setObjectContext(context);

        objectStore.registerObject(id, object);
        Object unregistered = objectStore.unregisterObject(id);
        assertSame(object, unregistered);

        assertSame(id, object.getObjectId());
        assertNull(object.getObjectContext());
        assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
    }
}