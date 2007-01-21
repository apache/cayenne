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


package org.apache.cayenne.jpa.cspi;

import java.sql.SQLException;
import java.util.Properties;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.spi.JndiJpaDataSourceFactory;
import org.apache.cayenne.jpa.spi.JpaDataSourceFactory;
import org.apache.cayenne.access.ConnectionLogger;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.util.Util;

/**
 * A {@link JpaDataSourceFactory} that attempts to create a DataSource based on Cayenne
 * provider-specific properties. If such properties are not present, a DataSource is
 * obtained via JNDI.
 * <p>
 * Properties are specified in the correspondign section of the <em>persistence.xml</em>
 * file. All property names related to a given named DataSource must be prefixed with
 * <em>"CayenneDataSource.[datasource name]."</em>. The following properties are
 * supported:
 * </p>
 * <ul>
 * <li>cayenne.ds.[datasource name].jdbc.driver - (required) JDBC driver class</li>
 * <li>cayenne.ds.[datasource name].jdbc.url - (required) Database URL</li>
 * <li>cayenne.ds.[datasource name].jdbc.username - Database login id</li>
 * <li>cayenne.ds.[datasource name].jdbc.password - Database password</li>
 * <li>cayenne.ds.[datasource name].jdbc.minConnections - (optional) Minimal pool
 * size</li>
 * <li>cayenne.ds.[datasource name].jdbc.maxConnections - (optional) Maximum pool
 * size</li>
 * </ul>
 * <p>
 * Another optional property is
 * <em>cayenne.ds.[datasource name].cayenne.adapter</em>. It is not strictly
 * related to the DataSource configuration, but Cayenne provider will use to configure the
 * same {@link org.apache.cayenne.access.DataNode} that will use the DataSource. If
 * not set, an AutoAdapter is used.
 * 
 * @author Andrus Adamchik
 */
public class CjpaDataSourceFactory extends JndiJpaDataSourceFactory {

    public static final String DATA_SOURCE_PREFIX = "cayenne.ds.";
    public static final String MIN_CONNECTIONS_SUFFIX = "jdbc.minConnections";
    public static final String MAX_CONNECTIONS_SUFFIX = "jdbc.maxConnections";

    static String getPropertyName(String dataSourceName, String suffix) {
        return DATA_SOURCE_PREFIX + dataSourceName + "." + suffix;
    }

    protected DataSource getDataSource(String name, PersistenceUnitInfo info) {
        if (name == null) {
            return null;
        }

        DataSource ds = getCayenneDataSource(name, info.getProperties());
        return ds != null ? ds : super.getDataSource(name, info);
    }

    protected DataSource getCayenneDataSource(String name, Properties properties) {

        String driverName = properties.getProperty(getDriverKey(name));
        if (Util.isEmptyString(driverName)) {
            return null;
        }

        String url = properties.getProperty(getUrlKey(name));
        if (Util.isEmptyString(url)) {
            return null;
        }

        int minConnection;
        try {
            minConnection = Integer.parseInt(properties
                    .getProperty(getMinConnectionsKey(name)));
        }
        catch (Exception e) {
            minConnection = 1;
        }

        int maxConnection;
        try {
            maxConnection = Integer.parseInt(properties
                    .getProperty(getMaxConnectionsKey(name)));
        }
        catch (Exception e) {
            maxConnection = 1;
        }

        // this code follows Cayenne DriverDataSourceFactory logic...
        try {
            return new PoolManager(
                    driverName,
                    url,
                    minConnection,
                    maxConnection,
                    properties.getProperty(getUserKey(name)),
                    properties.getProperty(getPasswordKey(name)),
                    new ConnectionLogger());
        }
        catch (SQLException e) {
            QueryLogger.logConnectFailure(e);
            throw new JpaProviderException("Error creating connection pool", e);
        }
    }

    protected String getDriverKey(String dataSourceName) {
        return getPropertyName(dataSourceName, ConnectionProperties.DRIVER_KEY);
    }

    protected String getUrlKey(String dataSourceName) {
        return getPropertyName(dataSourceName, ConnectionProperties.URL_KEY);
    }

    protected String getUserKey(String dataSourceName) {
        return getPropertyName(dataSourceName, ConnectionProperties.USER_NAME_KEY);
    }

    protected String getPasswordKey(String dataSourceName) {
        return getPropertyName(dataSourceName, ConnectionProperties.PASSWORD_KEY);
    }

    protected String getMinConnectionsKey(String dataSourceName) {
        return getPropertyName(dataSourceName, MIN_CONNECTIONS_SUFFIX);
    }

    protected String getMaxConnectionsKey(String dataSourceName) {
        return getPropertyName(dataSourceName, MAX_CONNECTIONS_SUFFIX);
    }

}
