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

package org.apache.cayenne.modeler.pref;

import java.sql.Driver;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.util.Util;

public class DBConnectionInfo extends _DBConnectionInfo {

    /**
     * Creates a DbAdapter based on configured values.
     */
    public DbAdapter makeAdapter(ClassLoadingService classLoader) throws Exception {
        String adapterClassName = getDbAdapter();

        if (adapterClassName == null
                || AutoAdapter.class.getName().equals(adapterClassName)) {
            return new AutoAdapter(makeDataSource(classLoader));
        }

        try {
            return (DbAdapter) classLoader.loadClass(adapterClassName).newInstance();
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            throw new Exception("DbAdapter load error: " + th.getLocalizedMessage());
        }
    }

    /**
     * Returns a DataSource that uses connection information from this object. Returned
     * DataSource is not pooling its connections. It can be wrapped in PoolManager if
     * pooling is needed.
     */
    public DataSource makeDataSource(ClassLoadingService classLoader) throws SQLException {

        // validate...
        if (getJdbcDriver() == null) {
            throw new SQLException("No JDBC driver set.");
        }

        if (getUrl() == null) {
            throw new SQLException("No DB URL set.");
        }

        // load driver...
        Driver driver;

        try {
            driver = (Driver) classLoader.loadClass(getJdbcDriver()).newInstance();
        }
        catch (Throwable th) {
            th = Util.unwindException(th);
            throw new SQLException("Driver load error: " + th.getLocalizedMessage());
        }

        return new DriverDataSource(driver, getUrl(), getUserName(), getPassword());
    }

    /**
     * Updates another DBConnectionInfo with this object's values.
     */
    public boolean copyTo(DBConnectionInfo dataSourceInfo) {
        boolean updated = false;

        if (!Util.nullSafeEquals(dataSourceInfo.getUrl(), getUrl())) {
            dataSourceInfo.setUrl(getUrl());
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

        if (!Util.nullSafeEquals(dataSourceInfo.getDbAdapter(), getDbAdapter())) {
            dataSourceInfo.setDbAdapter(getDbAdapter());
            updated = true;
        }

        return updated;
    }

    /**
     * Updates DataSourceInfo with this object's values.
     * <p>
     * <i>Currently doesn't set the adapter property. Need to change the UI to handle
     * adapter via DataSourceInfo first, and then it should be safe to do an adapter
     * update here. </i>
     * </p>
     */
    public boolean copyTo(DataSourceInfo dataSourceInfo) {
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

    public boolean copyFrom(DataSourceInfo dataSourceInfo) {
        boolean updated = false;

        if (!Util.nullSafeEquals(dataSourceInfo.getDataSourceUrl(), getUrl())) {
            setUrl(dataSourceInfo.getDataSourceUrl());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getUserName(), getUserName())) {
            setUserName(dataSourceInfo.getUserName());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getPassword(), getPassword())) {
            setPassword(dataSourceInfo.getPassword());
            updated = true;
        }

        if (!Util.nullSafeEquals(dataSourceInfo.getJdbcDriver(), getJdbcDriver())) {
            setJdbcDriver(dataSourceInfo.getJdbcDriver());
            updated = true;
        }

        return updated;
    }
}
