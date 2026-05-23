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

package org.apache.cayenne.modeler.pref.adapters;

import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DBConnectorsPrefsTest {

    private final Preferences root = Preferences.userRoot()
            .node("cayenne-test/" + UUID.randomUUID().toString().replace("-", ""));
    private final DBConnectorsPrefs prefs = new DBConnectorsPrefs(root);

    @AfterEach
    public void cleanup() throws BackingStoreException {
        root.removeNode();
    }

    @Test
    public void emptyPrefsReturnsEmptyRegistry() {
        assertTrue(prefs.getConnectors().getAll().isEmpty());
    }

    @Test
    public void loadsConnectorFromPrefsNode() throws BackingStoreException {
        Preferences n = root.node("myconn");
        n.put("url", "jdbc:h2:mem");
        n.put("userName", "sa");
        n.put("password", "secret");
        n.put("jdbcDriver", "org.h2.Driver");
        n.put("dbAdapter", "org.apache.cayenne.dba.h2.H2Adapter");
        root.flush();

        DBConnectors registry = prefs.getConnectors();
        DBConnector loaded = registry.get("myconn");

        assertEquals("jdbc:h2:mem", loaded.getUrl());
        assertEquals("sa", loaded.getUserName());
        assertEquals("secret", loaded.getPassword());
        assertEquals("org.h2.Driver", loaded.getJdbcDriver());
        assertEquals("org.apache.cayenne.dba.h2.H2Adapter", loaded.getDbAdapter());
    }

    @Test
    public void loadsMultipleConnectors() throws BackingStoreException {
        root.node("conn1").put("url", "jdbc:h2:mem:db1");
        root.node("conn2").put("url", "jdbc:h2:mem:db2");
        root.flush();

        DBConnectors registry = prefs.getConnectors();
        assertEquals(2, registry.getAll().size());
        assertEquals("jdbc:h2:mem:db1", registry.get("conn1").getUrl());
        assertEquals("jdbc:h2:mem:db2", registry.get("conn2").getUrl());
    }

    @Test
    public void missingPropertiesLoadAsNull() throws BackingStoreException {
        root.node("sparse").put("url", "jdbc:h2:mem");
        root.flush();

        DBConnector loaded = prefs.getConnectors().get("sparse");
        assertEquals("jdbc:h2:mem", loaded.getUrl());
        assertNull(loaded.getUserName());
        assertNull(loaded.getPassword());
        assertNull(loaded.getJdbcDriver());
        assertNull(loaded.getDbAdapter());
    }

    @Test
    public void putMutationWritesToPrefs() throws BackingStoreException {
        DBConnectors registry = prefs.getConnectors();

        DBConnector connector = new DBConnector();
        connector.setUrl("jdbc:pg://localhost/mydb");
        connector.setUserName("admin");
        registry.put("pgconn", connector);

        Preferences stored = root.node("pgconn");
        assertEquals("jdbc:pg://localhost/mydb", stored.get("url", null));
        assertEquals("admin", stored.get("userName", null));
    }

    @Test
    public void removeMutationDeletesPrefsNode() throws BackingStoreException {
        root.node("toremove").put("url", "jdbc:h2:mem");
        root.flush();

        DBConnectors registry = prefs.getConnectors();
        registry.remove("toremove");

        assertFalse(root.nodeExists("toremove"));
    }

    @Test
    public void initialLoadDoesNotFireWriteback() throws BackingStoreException {
        // Write one connector to prefs before loading
        root.node("existing").put("url", "jdbc:h2:mem");
        root.flush();

        List<String> writebackNames = new ArrayList<>();
        DBConnectors registry = prefs.getConnectors();

        // Attach a spy listener after loading to detect any late writeback
        registry.addListener(new DBConnectors.Listener() {
            public void connectionUpdated(String name) { writebackNames.add(name); }
            public void connectionRemoved(String name) { writebackNames.add(name); }
        });

        // No mutations occurred — writeback list must be empty
        assertTrue(writebackNames.isEmpty());
    }
}
