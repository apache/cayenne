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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @since 4.0
 */
public class ObjectStoreTest {

    private ObjectStore objectStore;

    @Before
    public void before() {
        DataRowStore sharedCache = mock(DataRowStore.class);
        this.objectStore = new ObjectStore(sharedCache, new HashMap<Object, Persistent>());
    }

    @Test
    public void testRegisterNode() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = mock(Persistent.class);

        objectStore.registerNode(id, object);
        assertSame(object, objectStore.getNode(id));
    }

    @Test
    public void testUnregisterNode() {

        ObjectId id = ObjectId.of("E1", "ID", 500);
        Persistent object = mock(Persistent.class);

        objectStore.registerNode(id, object);
        Object unregistered = objectStore.unregisterNode(id);
        assertSame(object, unregistered);

        verify(object, times(0)).setObjectId(null);
        verify(object).setObjectContext(null);
        verify(object).setPersistenceState(PersistenceState.TRANSIENT);    }
}