/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.pref;

import java.sql.Driver;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.DriverDataSource;
import org.objectstyle.cayenne.dba.AutoAdapter;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.modeler.ClassLoadingService;
import org.objectstyle.cayenne.util.Util;

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
