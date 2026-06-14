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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceConfigLoader.class);

    private static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    private static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    private static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    private static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    private static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";

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
            descriptor = switch (connectionName) {
                case "hsql" -> HsqlDataSource.start();
                case "h2" -> H2DataSource.start();
                case "derby" -> DerbyDataSource.start();
                case "sqlite" -> SQLiteDataSource.start();
                case "mysql" -> MysqlDataSource.start();
                case "mariadb" -> MariaDbDataSource.start();
                case "postgres" -> PostgresDataSource.start();
                case "sqlserver" -> SqlServerDataSource.start();
                case "oracle" -> OracleDataSource.start();
                case "db2" -> Db2DataSource.start();
                default -> null;
            };
        }

        descriptor = applyOverrides(descriptor);

        if (descriptor == null) {
            throw new ConfigurationException("No DataSource descriptor for key: " + connectionName);
        }

        LOGGER.info("loaded DataSource descriptor: {}", descriptor);
        return descriptor;
    }

    private static DataSourceDescriptor applyOverrides(DataSourceDescriptor descriptor) {

        String user = System.getProperty(USER_NAME_KEY_MAVEN);
        String pass = System.getProperty(PASSWORD_KEY_MAVEN);
        String url = System.getProperty(URL_KEY_MAVEN);
        String driver = System.getProperty(DRIVER_KEY_MAVEN);

        // no overrides, do nothing
        if (user == null && pass == null && url == null && driver == null) {
            return descriptor;
        }

        if (descriptor == null) {
            // only create a brand new DSI if overrides contains a DB url...
            if (url == null) {
                return null;
            }

            descriptor = DataSourceDescriptorFactory.create();
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
