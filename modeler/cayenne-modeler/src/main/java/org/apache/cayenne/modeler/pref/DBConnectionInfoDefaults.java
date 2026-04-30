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
package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.CayenneRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages the named collection of saved DB connection configurations,
 * backed by java.util.prefs.Preferences.
 */
public class DBConnectionInfoDefaults {

    private final Preferences node;
    private final Map<String, DBConnectionInfo> connections;
    private final List<String> toRemove;

    public DBConnectionInfoDefaults() {
        this.node = CayennePreference.getRoot().node(DBConnectionInfo.DB_CONNECTION_INFO);
        this.connections = new HashMap<>();
        this.toRemove = new ArrayList<>();
        try {
            for (String name : node.childrenNames()) {
                connections.put(name, new DBConnectionInfo(name, true));
            }
        } catch (BackingStoreException e) {
            throw new CayenneRuntimeException("Error loading data source preferences", e);
        }
    }

    public Map<String, DBConnectionInfo> getAll() {
        return connections;
    }

    public DBConnectionInfo get(String name) {
        return connections.get(name);
    }

    /** Creates a new entry, registers it, and returns it. */
    public DBConnectionInfo create(String name) {
        DBConnectionInfo info = new DBConnectionInfo(name, false);
        connections.put(name, info);
        return info;
    }

    public void remove(String name) {
        toRemove.add(name);
        connections.remove(name);
    }

    public void save() {
        for (String name : toRemove) {
            try {
                node.node(name).removeNode();
            } catch (BackingStoreException e) {
                throw new CayenneRuntimeException("Error removing data source preference", e);
            }
        }
        toRemove.clear();
        connections.values().forEach(DBConnectionInfo::saveObjectPreference);
    }

    public void cancel() {
        connections.clear();
        toRemove.clear();
        try {
            for (String name : node.childrenNames()) {
                connections.put(name, new DBConnectionInfo(name, true));
            }
        } catch (BackingStoreException e) {
            throw new CayenneRuntimeException("Error reverting data source preferences", e);
        }
    }
}
