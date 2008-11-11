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

package org.apache.cayenne.jpa.conf;

import java.sql.SQLException;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;

import org.apache.cayenne.access.ConnectionLogger;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.jpa.JpaProviderException;
import org.apache.cayenne.jpa.Provider;
import org.apache.cayenne.util.Util;

/**
 * A {@link JpaDataSourceFactory} that attempts to create a DataSource based on Cayenne
 * provider-specific properties. If such properties are not present, a DataSource is
 * obtained via JNDI.
 * <p>
 * Properties are specified in the corresponding section of the <em>persistence.xml</em>
 * file. The following properties are supported:
 * </p>
 * <ul>
 * <li>org.apache.cayenne.datasource.jdbc.driver - (required) JDBC driver class</li>
 * <li>org.apache.cayenne.datasource.jdbc.url - (required) Database URL</li>
 * <li>org.apache.cayenne.datasource.jdbc.username - Database login id</li>
 * <li>org.apache.cayenne.datasource.jdbc.password - Database password</li>
 * <li>org.apache.cayenne.datasource.jdbc.minConnections - (optional) Minimal pool size</li>
 * <li>org.apache.cayenne.datasource..jdbc.maxConnections - (optional) Maximum pool size</li>
 * </ul>
 * 
 */
public class DefaultDataSourceFactory implements JpaDataSourceFactory {

    public DataSource getJtaDataSource(String name, PersistenceUnitInfo info) {
        return getDataSource(name, info);
    }

    public DataSource getNonJtaDataSource(String name, PersistenceUnitInfo info) {
        return getDataSource(name, info);
    }

    protected DataSource getJndiDataSource(String name, PersistenceUnitInfo info) {
        if (name == null) {
            return null;
        }

        try {
            return (DataSource) new InitialContext().lookup(name);
        }
        catch (NamingException namingEx) {
            return null;
        }
    }

    protected DataSource getDataSource(String name, PersistenceUnitInfo info) {
        DataSource ds = null;

        // non-null name indicates that there is a named DataSource in persistence.xml, so
        // try JNDI first, and then fail over to Cayenne
        if (name != null) {
            ds = getJndiDataSource(name, info);
        }

        if (ds == null) {
            ds = getCayenneDataSource(info.getProperties());
        }

        return ds;
    }

    protected DataSource getCayenneDataSource(Properties properties) {

        String driverName = properties.getProperty(Provider.DATA_SOURCE_DRIVER_PROPERTY);
        if (Util.isEmptyString(driverName)) {
            return null;
        }

        String url = properties.getProperty(Provider.DATA_SOURCE_URL_PROPERTY);
        if (Util.isEmptyString(url)) {
            return null;
        }

        int minConnection;
        try {
            minConnection = Integer.parseInt(properties
                    .getProperty(Provider.DATA_SOURCE_MIN_CONNECTIONS_PROPERTY));
        }
        catch (Exception e) {
            minConnection = 1;
        }

        int maxConnection;
        try {
            maxConnection = Integer.parseInt(properties
                    .getProperty(Provider.DATA_SOURCE_MAX_CONNECTIONS_PROPERTY));
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
                    properties.getProperty(Provider.DATA_SOURCE_USER_NAME_PROPERTY),
                    properties.getProperty(Provider.DATA_SOURCE_PASSWORD_PROPERTY),
                    new ConnectionLogger());
        }
        catch (SQLException e) {
            QueryLogger.logConnectFailure(e);
            throw new JpaProviderException("Error creating connection pool", e);
        }
    }
}
