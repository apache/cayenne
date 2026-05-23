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

package org.apache.cayenne.modeler.pref.dbconnector;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DBConnectorsTest {

    @Test
    public void putAddsConnectorAndFiresListener() {
        DBConnectors registry = new DBConnectors();
        List<String> updated = new ArrayList<>();
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { updated.add(name); }
            public void connectionRemoved(String name) { }
        });

        registry.put("main", new DBConnector());

        assertEquals(List.of("main"), updated);
        assertTrue(registry.getAll().containsKey("main"));
    }

    @Test
    public void putOnExistingNameFiresUpdateListener() {
        DBConnectors registry = new DBConnectors();
        registry.put("main", new DBConnector());

        List<String> updated = new ArrayList<>();
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { updated.add(name); }
            public void connectionRemoved(String name) { }
        });

        registry.put("main", new DBConnector());

        assertEquals(List.of("main"), updated);
    }

    @Test
    public void removeExistingFiresListener() {
        DBConnectors registry = new DBConnectors();
        registry.put("main", new DBConnector());

        List<String> removed = new ArrayList<>();
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { }
            public void connectionRemoved(String name) { removed.add(name); }
        });

        registry.remove("main");

        assertEquals(List.of("main"), removed);
        assertFalse(registry.getAll().containsKey("main"));
    }

    @Test
    public void removeNonExistentDoesNotFireListener() {
        DBConnectors registry = new DBConnectors();
        List<String> removed = new ArrayList<>();
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { }
            public void connectionRemoved(String name) { removed.add(name); }
        });

        registry.remove("nonexistent");

        assertTrue(removed.isEmpty());
    }

    @Test
    public void getAllReturnsUnmodifiableMap() {
        DBConnectors registry = new DBConnectors();
        registry.put("main", new DBConnector());

        Map<String, DBConnector> all = registry.getAll();

        assertThrows(UnsupportedOperationException.class, () -> all.put("other", new DBConnector()));
    }

    @Test
    public void getAllPreservesInsertionOrder() {
        DBConnectors registry = new DBConnectors();
        registry.put("alpha", new DBConnector());
        registry.put("beta", new DBConnector());
        registry.put("gamma", new DBConnector());

        List<String> keys = new ArrayList<>(registry.getAll().keySet());
        assertEquals(List.of("alpha", "beta", "gamma"), keys);
    }

    @Test
    public void multipleListenersAllNotified() {
        DBConnectors registry = new DBConnectors();
        List<String> calls1 = new ArrayList<>();
        List<String> calls2 = new ArrayList<>();

        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { calls1.add(name); }
            public void connectionRemoved(String name) { }
        });
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { calls2.add(name); }
            public void connectionRemoved(String name) { }
        });

        registry.put("conn", new DBConnector());

        assertEquals(List.of("conn"), calls1);
        assertEquals(List.of("conn"), calls2);
    }
}
