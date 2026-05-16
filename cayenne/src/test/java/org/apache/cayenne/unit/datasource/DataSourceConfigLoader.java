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
package org.apache.cayenne.unit.datasource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.unit.testcontainers.Db2ContainerStarter;
import org.apache.cayenne.unit.testcontainers.DbContainerStarter;
import org.apache.cayenne.unit.testcontainers.MariaDbContainerStarter;
import org.apache.cayenne.unit.testcontainers.MysqlContainerStarter;
import org.apache.cayenne.unit.testcontainers.OracleContainerStarter;
import org.apache.cayenne.unit.testcontainers.PostgresContainerStarter;
import org.apache.cayenne.unit.testcontainers.SqlServerContainerStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class DataSourceConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfigLoader.class);

    private static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    private static final String CONNECTION_DB_VERSION = "cayenneTestDbVersion";
    private static final String ADAPTER_KEY_MAVEN = "cayenneAdapter";
    private static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    private static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    private static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    private static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";

    static final int MIN_CONNECTIONS = 1;
    static final int MAX_CONNECTIONS = 3;

    public static DataSourceDescriptor load() {

        String connectionName = System.getProperty(CONNECTION_NAME_KEY);
        if (connectionName == null) {
            LOGGER.info("DataSource name is not specified, assuming 'hsql'");
            connectionName = "hsql";
        } else {
            LOGGER.info("DataSource name: {}", connectionName);
        }

        DataSourceDescriptor descriptor = DataSourceConfigFile.load().get(connectionName);

        if (descriptor == null) {
            descriptor = checkInMemoryDataSource(connectionName);
        }

        if (descriptor == null) {
            descriptor = checkTestContainersDataSource(connectionName);
        }

        descriptor = applyOverrides(descriptor);

        if (descriptor == null) {
            throw new ConfigurationException("No DataSource descriptor for key: " + connectionName);
        }

        LOGGER.info("loaded DataSource descriptor: " + descriptor);
        return descriptor;
    }

    private static DataSourceDescriptor checkInMemoryDataSource(String connectionName) {
        return switch (connectionName) {
            case "hsql" -> {
                DataSourceDescriptor descriptor = new DataSourceDescriptor();
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:hsqldb:mem:aname;sql.regular_names=false");
                descriptor.setJdbcDriver("org.hsqldb.jdbcDriver");
                descriptor.setMinConnections(MIN_CONNECTIONS);
                descriptor.setMaxConnections(MAX_CONNECTIONS);
                yield descriptor;
            }
            case "h2" -> {
                DataSourceDescriptor descriptor = new DataSourceDescriptor();
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:h2:mem:aname;DB_CLOSE_DELAY=-1;");
                descriptor.setJdbcDriver("org.h2.Driver");
                descriptor.setMinConnections(MIN_CONNECTIONS);
                descriptor.setMaxConnections(MAX_CONNECTIONS);
                yield descriptor;
            }
            case "derby" -> {
                DataSourceDescriptor descriptor = new DataSourceDescriptor();
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:derby:target/testdb;create=true");
                descriptor.setJdbcDriver("org.apache.derby.jdbc.EmbeddedDriver");
                descriptor.setMinConnections(MIN_CONNECTIONS);
                descriptor.setMaxConnections(MAX_CONNECTIONS);
                yield descriptor;
            }
            case "sqlite" -> {
                DataSourceDescriptor descriptor = new DataSourceDescriptor();
                descriptor.setUserName("sa");
                descriptor.setPassword("");
                descriptor.setDataSourceUrl("jdbc:sqlite:file:memdb?mode=memory&cache=shared&date_class=text");
                descriptor.setJdbcDriver("org.sqlite.JDBC");
                descriptor.setMinConnections(MIN_CONNECTIONS);
                descriptor.setMaxConnections(MAX_CONNECTIONS);
                yield descriptor;
            }
            default -> null;
        };
    }

    private static DataSourceDescriptor checkTestContainersDataSource(String connectionName) {

        DbContainerStarter containerStarter = switch (connectionName) {
            case "mysql-tc" -> new MysqlContainerStarter();
            case "mariadb-tc" -> new MariaDbContainerStarter();
            case "postgres-tc" -> new PostgresContainerStarter();
            case "sqlserver-tc" -> new SqlServerContainerStarter();
            case "oracle-tc" -> new OracleContainerStarter();
            case "db2-tc" -> new Db2ContainerStarter();
            default -> null;
        };

        if (containerStarter == null) {
            return null;
        }

        String version = System.getProperty(CONNECTION_DB_VERSION);
        JdbcDatabaseContainer<?> container = containerStarter.startContainer(version);

        DataSourceDescriptor sourceInfo = new DataSourceDescriptor();
        sourceInfo.setUserName(container.getUsername());
        sourceInfo.setPassword(container.getPassword());
        sourceInfo.setDataSourceUrl(container.getJdbcUrl());
        sourceInfo.setJdbcDriver(container.getDriverClassName());
        sourceInfo.setMinConnections(MIN_CONNECTIONS);
        sourceInfo.setMaxConnections(MAX_CONNECTIONS);
        return sourceInfo;
    }

    private static DataSourceDescriptor applyOverrides(DataSourceDescriptor descriptor) {

        String adapter = System.getProperty(ADAPTER_KEY_MAVEN);
        String user = System.getProperty(USER_NAME_KEY_MAVEN);
        String pass = System.getProperty(PASSWORD_KEY_MAVEN);
        String url = System.getProperty(URL_KEY_MAVEN);
        String driver = System.getProperty(DRIVER_KEY_MAVEN);

        // no overrides, do nothing
        if (adapter == null && user == null && pass == null && url == null && driver == null) {
            return descriptor;
        }

        if (descriptor == null) {
            // only create a brand new DSI if overrides contains a DB url...
            if (url == null) {
                return null;
            }

            descriptor = new DataSourceDescriptor();
            descriptor.setMinConnections(MIN_CONNECTIONS);
            descriptor.setMaxConnections(MAX_CONNECTIONS);
        }

        if (user != null) {
            descriptor.setUserName(user);
        }

        if (pass != null) {
            descriptor.setPassword(pass);
        }

        if (url != null) {
            descriptor.setDataSourceUrl(url);
        }

        if (driver != null) {
            descriptor.setJdbcDriver(driver);
        }

        return descriptor;
    }
}
