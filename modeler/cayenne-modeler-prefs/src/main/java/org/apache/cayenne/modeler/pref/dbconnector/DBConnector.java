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

package org.apache.cayenne.modeler.pref.dbconnector;

import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.util.Util;

/**
 * Configuration record for a named database connection profile stored in user preferences.
 * Holds the five connection parameters (adapter, driver, url, user, password). Runtime
 * operations (creating a live {@code DataSource} or {@code DbAdapter}) are in
 * {@code DBConnectorFactory} in the Modeler module.
 */
public class DBConnector {

    public static final String DB_ADAPTER_PROPERTY = "dbAdapter";
    public static final String JDBC_DRIVER_PROPERTY = "jdbcDriver";
    public static final String PASSWORD_PROPERTY = "password";
    public static final String URL_PROPERTY = "url";
    public static final String USER_NAME_PROPERTY = "userName";

    private String dbAdapter;
    private String jdbcDriver;
    private String password;
    private String url;
    private String userName;

    public String getDbAdapter() {
        return dbAdapter;
    }

    public void setDbAdapter(final String dbAdapter) {
        this.dbAdapter = dbAdapter;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(final String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Copies this connector's values into another {@link DBConnector}.
     * Returns {@code true} if any field changed.
     */
    public boolean copyTo(DBConnector connector) {
        boolean updated = false;

        if (!Util.nullSafeEquals(connector.getUrl(), getUrl())) {
            connector.setUrl(getUrl());
            updated = true;
        }

        if (!Util.nullSafeEquals(connector.getUserName(), getUserName())) {
            connector.setUserName(getUserName());
            updated = true;
        }

        if (!Util.nullSafeEquals(connector.getPassword(), getPassword())) {
            connector.setPassword(getPassword());
            updated = true;
        }

        if (!Util.nullSafeEquals(connector.getJdbcDriver(), getJdbcDriver())) {
            connector.setJdbcDriver(getJdbcDriver());
            updated = true;
        }

        if (!Util.nullSafeEquals(connector.getDbAdapter(), getDbAdapter())) {
            connector.setDbAdapter(getDbAdapter());
            updated = true;
        }

        return updated;
    }

    /**
     * Updates a {@link DataSourceDescriptor} with this connector's values.
     * Returns {@code true} if any field changed.
     */
    public boolean copyTo(final DataSourceDescriptor dataSourceInfo) {
        boolean updated = false;

        if (!Util.nullSafeEquals(dataSourceInfo.getDataSourceUrl(), getUrl())) {
            dataSourceInfo.setDataSourceUrl(getUrl());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getUserName(), getUserName())) {
            dataSourceInfo.setUserName(getUserName());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getPassword(), getPassword())) {
            dataSourceInfo.setPassword(getPassword());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getJdbcDriver(), getJdbcDriver())) {
            dataSourceInfo.setJdbcDriver(getJdbcDriver());
            updated = true;
        }

        return updated;
    }
}
