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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.pref.CayennePreference;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Loads {@link DBConnectors} from the user prefs and binds the registry's
 * change events to the prefs node, so registry mutations are mirrored to disk.
 */
public class DBConnectorPrefs {

    private static final String DB_CONNECTION_INFO = "dbConnectionInfo";

    private static final String DB_ADAPTER_PROPERTY = "dbAdapter";
    private static final String JDBC_DRIVER_PROPERTY = "jdbcDriver";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String URL_PROPERTY = "url";
    private static final String USER_NAME_PROPERTY = "userName";

    public static DBConnectors loadAndBind() {
        Preferences root = CayennePreference.getRoot().node(DB_CONNECTION_INFO);

        DBConnectors connectors = new DBConnectors();

        try {
            for (String name : root.childrenNames()) {
                connectors.put(name, loadConnector(root.node(name)));
            }
        } catch (BackingStoreException e) {
            throw new CayenneRuntimeException("Error loading data source preferences", e);
        }

        connectors.addListener(new DBConnectors.Listener() {
            @Override
            public void connectionUpdated(String name) {
                storeConnector(root.node(name), connectors.get(name));
            }

            @Override
            public void connectionRemoved(String name) {
                try {
                    root.node(name).removeNode();
                } catch (BackingStoreException e) {
                    throw new CayenneRuntimeException("Error removing data source preference", e);
                }
            }
        });

        return connectors;
    }

    private static DBConnector loadConnector(Preferences n) {
        DBConnector connector = new DBConnector();
        connector.setDbAdapter(n.get(DB_ADAPTER_PROPERTY, null));
        connector.setUrl(n.get(URL_PROPERTY, null));
        connector.setUserName(n.get(USER_NAME_PROPERTY, null));
        connector.setPassword(n.get(PASSWORD_PROPERTY, null));
        connector.setJdbcDriver(n.get(JDBC_DRIVER_PROPERTY, null));
        return connector;
    }

    private static void storeConnector(Preferences n, DBConnector connector) {
        putOrRemove(n, DB_ADAPTER_PROPERTY, connector.getDbAdapter());
        putOrRemove(n, URL_PROPERTY, connector.getUrl());
        putOrRemove(n, USER_NAME_PROPERTY, connector.getUserName());
        putOrRemove(n, PASSWORD_PROPERTY, connector.getPassword());
        putOrRemove(n, JDBC_DRIVER_PROPERTY, connector.getJdbcDriver());
    }

    private static void putOrRemove(Preferences n, String key, String value) {
        if (value == null) {
            n.remove(key);
        } else {
            n.put(key, value);
        }
    }
}
