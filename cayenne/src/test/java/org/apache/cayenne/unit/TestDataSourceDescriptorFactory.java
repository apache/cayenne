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
package org.apache.cayenne.unit;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.unit.testcontainers.Db2ContainerProvider;
import org.apache.cayenne.unit.testcontainers.MariaDbContainerProvider;
import org.apache.cayenne.unit.testcontainers.MysqlContainerProvider;
import org.apache.cayenne.unit.testcontainers.OracleContainerProvider;
import org.apache.cayenne.unit.testcontainers.PostgresContainerProvider;
import org.apache.cayenne.unit.testcontainers.SqlServerContainerProvider;
import org.apache.cayenne.unit.testcontainers.TestContainerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestDataSourceDescriptorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataSourceDescriptorFactory.class);

    private static final String PROPERTIES_FILE = "connection.properties";
    private static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    private static final String CONNECTION_DB_VERSION = "cayenneTestDbVersion";
    private static final String ADAPTER_KEY_MAVEN = "cayenneAdapter";
    private static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    private static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    private static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    private static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";


    public static UnitDataSourceDescriptor create() {

        Map<String, String> propertiesMap = new HashMap<>();

        File file = connectionPropertiesFile();
        if (file.exists()) {
            Properties properties = new Properties();
            properties.load(new FileReader(file));
            properties.forEach((k, v) -> propertiesMap.put(k.toString(), v.toString()));
        }

        ConnectionProperties connectionProperties = new ConnectionProperties(propertiesMap);
        LOGGER.info("Loaded  " + connectionProperties.size() + " DataSource configurations from properties file");

        String connectionKey = property(CONNECTION_NAME_KEY);
        if (connectionKey == null) {
            connectionKey = "hsql";
        }

        LOGGER.info("Connection key: " + connectionKey);
        UnitDataSourceDescriptor connectionInfo = connectionProperties.getConnection(connectionKey);

        // attempt default if invalid key is specified
        if (connectionInfo == null) {
            connectionInfo = checkInMemoryDataSource(connectionKey);
        }

        if (connectionInfo == null) {
            connectionInfo = checkTestContainersDataSource(connectionKey);
        }

        connectionInfo = applyOverrides(connectionInfo);

        if (connectionInfo == null) {
            throw new ConfigurationException("No connection info for key: " + connectionKey);
        }

        LOGGER.info("loaded connection info: " + connectionInfo);
        return connectionInfo;
    }

    private static UnitDataSourceDescriptor checkInMemoryDataSource(String connectionKey) {
        return switch (connectionKey) {
            case "hsql" -> {
                UnitDataSourceDescriptor descriptor = new UnitDataSourceDescriptor();
                descriptor.setAdapterClassName(HSQLDBAdapter.class.getName());
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:hsqldb:mem:aname;sql.regular_names=false");
                descriptor.setJdbcDriver("org.hsqldb.jdbcDriver");
                descriptor.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
                descriptor.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
                yield descriptor;
            }
            case "h2" -> {
                UnitDataSourceDescriptor descriptor = new UnitDataSourceDescriptor();
                descriptor.setAdapterClassName(H2Adapter.class.getName());
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:h2:mem:aname;DB_CLOSE_DELAY=-1;");
                descriptor.setJdbcDriver("org.h2.Driver");
                descriptor.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
                descriptor.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
                yield descriptor;
            }
            case "derby" -> {
                UnitDataSourceDescriptor descriptor = new UnitDataSourceDescriptor();
                descriptor.setAdapterClassName(DerbyAdapter.class.getName());
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:derby:target/testdb;create=true");
                descriptor.setJdbcDriver("org.apache.derby.jdbc.EmbeddedDriver");
                descriptor.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
                descriptor.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
                yield descriptor;
            }
            case "sqlite" -> {
                UnitDataSourceDescriptor descriptor = new UnitDataSourceDescriptor();
                descriptor.setAdapterClassName(SQLiteAdapter.class.getName());
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:sqlite:file:memdb?mode=memory&cache=shared&date_class=text");
                descriptor.setJdbcDriver("org.sqlite.JDBC");
                descriptor.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
                descriptor.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
                yield descriptor;
            }
            default -> null;
        };
    }

    private static UnitDataSourceDescriptor checkTestContainersDataSource(String connectionKey) {

        // special case for the testcontainers profile
        if (!connectionKey.endsWith("-tc")) {
            return null;
        }

        String db = connectionKey.substring(0, connectionKey.length() - 3);

        TestContainerProvider testContainerProvider = switch (db) {
            case "mysql" -> new MysqlContainerProvider();
            case "mariadb" -> new MariaDbContainerProvider();
            case "postgres" -> new PostgresContainerProvider();
            case "sqlserver" -> new SqlServerContainerProvider();
            case "oracle" -> new OracleContainerProvider();
            case "db2" -> new Db2ContainerProvider();
            default -> null;
        };

        if (testContainerProvider == null) {
            return null;
        }

        String version = System.getProperty(CONNECTION_DB_VERSION);
        JdbcDatabaseContainer<?> container = testContainerProvider.startContainer(version);

        UnitDataSourceDescriptor sourceInfo = new UnitDataSourceDescriptor();
        sourceInfo.setAdapterClassName(testContainerProvider.getAdapterClass().getName());
        sourceInfo.setUserName(container.getUsername());
        sourceInfo.setPassword(container.getPassword());
        sourceInfo.setDataSourceUrl(container.getJdbcUrl());
        sourceInfo.setJdbcDriver(container.getDriverClassName());
        sourceInfo.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
        sourceInfo.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        return sourceInfo;
    }

    private static File connectionPropertiesFile() {
        return new File(cayenneUserDir(), PROPERTIES_FILE);
    }

    private static File cayenneUserDir() {
        File homeDir = new File(System.getProperty("user.home"));
        return new File(homeDir, ".cayenne");
    }

    private static UnitDataSourceDescriptor applyOverrides(UnitDataSourceDescriptor connectionInfo) {
        String adapter = System.getProperty(ADAPTER_KEY_MAVEN);
        String user = System.getProperty(USER_NAME_KEY_MAVEN);
        String pass = System.getProperty(PASSWORD_KEY_MAVEN);
        String url = System.getProperty(URL_KEY_MAVEN);
        String driver = System.getProperty(DRIVER_KEY_MAVEN);
        // no overrides, do nothing
        if (adapter == null && user == null && pass == null && url == null && driver == null) {
            return connectionInfo;
        }

        if (connectionInfo == null) {
            // only create a brand new DSI if overrides contains a DB url...
            if (url == null) {
                return null;
            }

            connectionInfo = new UnitDataSourceDescriptor();
            connectionInfo.setMinConnections(ConnectionProperties.MIN_CONNECTIONS);
            connectionInfo.setMaxConnections(ConnectionProperties.MAX_CONNECTIONS);
        }

        connectionInfo = connectionInfo.copy();
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
}
