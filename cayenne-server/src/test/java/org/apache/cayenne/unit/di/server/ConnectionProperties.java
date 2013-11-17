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

package org.apache.cayenne.unit.di.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.commons.collections.ExtendedProperties;

/**
 * ConnectionProperties handles a set of DataSourceInfo objects using
 * information stored in $HOME/.cayenne/connection.properties. As of now this is
 * purely a utility class. Its features are not used in deployment.
 */
class ConnectionProperties {

    private static final String ADAPTER_KEY = "adapter";
    private static final String ADAPTER20_KEY = "cayenne.adapter";
    private static final String USER_NAME_KEY = "jdbc.username";
    private static final String PASSWORD_KEY = "jdbc.password";
    private static final String URL_KEY = "jdbc.url";
    private static final String DRIVER_KEY = "jdbc.driver";

    private Map<String, DataSourceInfo> connectionInfos;

    /**
     * Constructor for ConnectionProperties.
     */
    ConnectionProperties(ExtendedProperties props) {
        connectionInfos = new HashMap<String, DataSourceInfo>();
        for (String name : extractNames(props)) {
            DataSourceInfo dsi = buildDataSourceInfo(props.subset(name));
            connectionInfos.put(name, dsi);
        }
    }

    int size() {
        return connectionInfos.size();
    }

    /**
     * Returns DataSourceInfo object for a symbolic name. If name does not match
     * an existing object, returns null.
     */
    DataSourceInfo getConnection(String name) {
        return connectionInfos.get(name);
    }

    /**
     * Creates a DataSourceInfo object from a set of properties.
     */
    private DataSourceInfo buildDataSourceInfo(ExtendedProperties props) {
        DataSourceInfo dsi = new DataSourceInfo();

        String adapter = props.getString(ADAPTER_KEY);

        // try legacy adapter key
        if (adapter == null) {
            adapter = props.getString(ADAPTER20_KEY);
        }

        dsi.setAdapterClassName(adapter);
        dsi.setUserName(props.getString(USER_NAME_KEY));
        dsi.setPassword(props.getString(PASSWORD_KEY));
        dsi.setDataSourceUrl(props.getString(URL_KEY));
        dsi.setJdbcDriver(props.getString(DRIVER_KEY));

        return dsi;
    }

    /**
     * Returns a list of connection names configured in the properties object.
     */
    private List<String> extractNames(ExtendedProperties props) {
        Iterator<?> it = props.getKeys();
        List<String> list = new ArrayList<String>();

        while (it.hasNext()) {
            String key = (String) it.next();

            int dotInd = key.indexOf('.');
            if (dotInd <= 0 || dotInd >= key.length()) {
                continue;
            }

            String name = key.substring(0, dotInd);
            if (!list.contains(name)) {
                list.add(name);
            }
        }

        return list;
    }
}
