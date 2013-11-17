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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.di.Provider;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerCaseDataSourceInfoProvider implements Provider<DataSourceInfo> {

    private static Log logger = LogFactory.getLog(ServerCaseDataSourceInfoProvider.class);

    private static final String PROPERTIES_FILE = "connection.properties";
    private static final String CONNECTION_NAME_KEY = "cayenneTestConnection";

    private static final String ADAPTER_KEY_MAVEN = "cayenneAdapter";
    private static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    private static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    private static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    private static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";

    private Map<String, DataSourceInfo> inMemoryDataSources;
    private ConnectionProperties connectionProperties;

    public ServerCaseDataSourceInfoProvider() throws IOException {

        File file = connectionPropertiesFile();
        ExtendedProperties properties = file.exists() ? new ExtendedProperties(file.getAbsolutePath())
                : new ExtendedProperties();

        this.connectionProperties = new ConnectionProperties(properties);
        logger.info("Loaded  " + connectionProperties.size() + " DataSource configurations from properties file");

        this.inMemoryDataSources = new HashMap<String, DataSourceInfo>();

        // preload default in-memory DataSources. Will use them as defaults if
        // nothing is configured in ~/.cayenne/connection.properties
        DataSourceInfo hsqldb = new DataSourceInfo();
        hsqldb.setAdapterClassName(HSQLDBAdapter.class.getName());
        hsqldb.setUserName("sa");
        hsqldb.setPassword("");
        hsqldb.setDataSourceUrl("jdbc:hsqldb:mem:aname");
        hsqldb.setJdbcDriver("org.hsqldb.jdbcDriver");
        inMemoryDataSources.put("hsql", hsqldb);

        DataSourceInfo h2 = new DataSourceInfo();
        h2.setAdapterClassName(H2Adapter.class.getName());
        h2.setUserName("sa");
        h2.setPassword("");
        h2.setDataSourceUrl("jdbc:h2:mem:aname;MVCC=TRUE");
        h2.setJdbcDriver("org.h2.Driver");
        inMemoryDataSources.put("h2", h2);

        DataSourceInfo derby = new DataSourceInfo();
        derby.setAdapterClassName(DerbyAdapter.class.getName());
        derby.setUserName("sa");
        derby.setPassword("");
        derby.setDataSourceUrl("jdbc:derby:target/testdb;create=true");
        derby.setJdbcDriver("org.apache.derby.jdbc.EmbeddedDriver");
        inMemoryDataSources.put("derby", h2);
    }

    @Override
    public DataSourceInfo get() throws ConfigurationException {

        String connectionKey = property(CONNECTION_NAME_KEY);
        if (connectionKey == null) {
            connectionKey = "hsql";
        }

        logger.info("Connection key: " + connectionKey);
        DataSourceInfo connectionInfo = connectionProperties.getConnection(connectionKey);

        // attempt default if invalid key is specified
        if (connectionInfo == null) {
            connectionInfo = inMemoryDataSources.get(connectionKey);
        }

        connectionInfo = applyOverrides(connectionInfo);

        if (connectionInfo == null) {
            throw new ConfigurationException("No connection info for key: " + connectionKey);
        }

        logger.info("loaded connection info: " + connectionInfo);
        return connectionInfo;
    }

    private File connectionPropertiesFile() {
        return new File(cayenneUserDir(), PROPERTIES_FILE);
    }

    private File cayenneUserDir() {
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir, ".cayenne");
    }

    private DataSourceInfo applyOverrides(DataSourceInfo connectionInfo) {
        String adapter = property(ADAPTER_KEY_MAVEN);
        String user = property(USER_NAME_KEY_MAVEN);
        String pass = property(PASSWORD_KEY_MAVEN);
        String url = property(URL_KEY_MAVEN);
        String driver = property(DRIVER_KEY_MAVEN);

        if (connectionInfo == null) {
            // only create a brand new DSI if overrides contains a DB url...
            if (url == null) {
                return null;
            }

            connectionInfo = new DataSourceInfo();
        }

        connectionInfo = connectionInfo.cloneInfo();
        if (adapter != null) {
            connectionInfo.setAdapterClassName(adapter);
        }

        if (user != null) {
            connectionInfo.setUserName(user);
        }

        if (pass != null) {
            connectionInfo.setPassword(pass);
        }

        if (url != null) {
            connectionInfo.setDataSourceUrl(url);
        }

        if (driver != null) {
            connectionInfo.setJdbcDriver(driver);
        }

        return connectionInfo;
    }

    private String property(String name) {
        String p = System.getProperty(name);
        return p == null || p.startsWith("$") ? null : p;
    }
}
