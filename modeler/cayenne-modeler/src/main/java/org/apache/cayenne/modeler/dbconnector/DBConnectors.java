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

package org.apache.cayenne.modeler.dbconnector;

import org.apache.cayenne.modeler.pref.adapters.DBConnectorPrefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * App-wide registry of named local DB connection profiles. Source of truth at runtime;
 * fires events on add/remove so listeners (e.g. {@link DBConnectorPrefs}) can mirror
 * state to persistent storage.
 */
public class DBConnectors {

    public interface Listener {
        void connectionUpdated(String name);

        void connectionRemoved(String name);
    }

    private final Map<String, DBConnector> connectors;
    private final List<Listener> listeners;

    public DBConnectors() {
        connectors = new LinkedHashMap<>();
        listeners = new ArrayList<>();
    }

    public Map<String, DBConnector> getAll() {
        return Collections.unmodifiableMap(connectors);
    }

    public DBConnector get(String name) {
        return connectors.get(name);
    }

    public void put(String name, DBConnector connector) {
        connectors.put(name, connector);
        for (Listener l : listeners) {
            l.connectionUpdated(name);
        }
    }

    public void remove(String name) {
        if (connectors.remove(name) != null) {
            for (Listener l : listeners) {
                l.connectionRemoved(name);
            }
        }
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }
}
