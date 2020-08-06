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

package org.apache.cayenne.modeler.pref;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.pref.CayennePreference;
import org.apache.cayenne.util.Util;

public class DBConnectionInfo extends CayennePreference {

	private static final String EMPTY_STRING = "";
	public static final String DB_ADAPTER_PROPERTY = "dbAdapter";
	public static final String JDBC_DRIVER_PROPERTY = "jdbcDriver";
	public static final String PASSWORD_PROPERTY = "password";
	public static final String URL_PROPERTY = "url";
	public static final String USER_NAME_PROPERTY = "userName";
	private static final String DB_CONNECTION_INFO = "dbConnectionInfo";

	public static final String ID_PK_COLUMN = "id";

	private String nodeName;

	private String dbAdapter;
	private String jdbcDriver;
	private String password;
	private String url;
	private String userName;

	private Preferences dbConnectionInfoPreferences;

	public DBConnectionInfo() {
		dbConnectionInfoPreferences = getCayennePreference().node(DB_CONNECTION_INFO);
		setCurrentPreference(dbConnectionInfoPreferences);
	};

	public DBConnectionInfo(final String nameNode, final boolean initFromPreferences) {
		this();
		setNodeName(nameNode);
		if (initFromPreferences) {
			initObjectPreference();
		}
	};

	@Override
	public Preferences getCurrentPreference() {
		if (getNodeName() == null) {
			return super.getCurrentPreference();
		}
		return dbConnectionInfoPreferences.node(getNodeName());
	}

	@Override
	public void setObject(final CayennePreference object) {
		if (object instanceof DBConnectionInfo) {
			setUrl(((DBConnectionInfo) object).getUrl());
			setUserName(((DBConnectionInfo) object).getUserName());
			setPassword(((DBConnectionInfo) object).getPassword());
			setJdbcDriver(((DBConnectionInfo) object).getJdbcDriver());
			setDbAdapter(((DBConnectionInfo) object).getDbAdapter());
		}
	}

	@Override
	public void saveObjectPreference() {
		if (getCurrentPreference() != null) {
			if (getDbAdapter() != null) {
				getCurrentPreference().put(DB_ADAPTER_PROPERTY, getDbAdapter());
			}
			if (getUrl() != null) {
				getCurrentPreference().put(URL_PROPERTY, getUrl());
			}
			if (getUserName() != null) {
				getCurrentPreference().put(USER_NAME_PROPERTY, getUserName());
			}
			if (getPassword() != null) {
				getCurrentPreference().put(PASSWORD_PROPERTY, getPassword());
			}
			if (getJdbcDriver() != null) {
				getCurrentPreference().put(JDBC_DRIVER_PROPERTY, getJdbcDriver());
			}
		}
	}

	public void initObjectPreference() {
		if (getCurrentPreference() != null) {
			setDbAdapter(getCurrentPreference().get(DB_ADAPTER_PROPERTY, null));
			setUrl(getCurrentPreference().get(URL_PROPERTY, null));
			setUserName(getCurrentPreference().get(USER_NAME_PROPERTY, null));
			setPassword(getCurrentPreference().get(PASSWORD_PROPERTY, null));
			setJdbcDriver(getCurrentPreference().get(JDBC_DRIVER_PROPERTY, null));
			setNodeName(getCurrentPreference().name());
		}
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(final String nodeName) {
		this.nodeName = nodeName;
	}

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
		return password == null ? EMPTY_STRING : password;
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
		return userName == null ? EMPTY_STRING : userName;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	public Preferences getDbConnectionInfoPreferences() {
		return dbConnectionInfoPreferences;
	}

	public void setDbConnectionInfoPreferences(final Preferences dbConnectionInfoPreferences) {
		this.dbConnectionInfoPreferences = dbConnectionInfoPreferences;
	}

	/**
	 * Creates a DbAdapter based on configured values.
	 */
	public DbAdapter makeAdapter(final ClassLoadingService classLoader) throws Exception {
		String adapterClassName = getDbAdapter();
		Application appInstance = Application.getInstance();

		if (adapterClassName == null || AutoAdapter.class.getName().equals(adapterClassName)) {
			return appInstance.getInjector().getInstance(DbAdapterFactory.class)
					.createAdapter(null, makeDataSource(classLoader));
		}

		try {
			return appInstance.getInjector().getInstance(AdhocObjectFactory.class)
					.newInstance(DbAdapter.class, adapterClassName);
		} catch (Throwable th) {
			th = Util.unwindException(th);
			throw new Exception("DbAdapter load error: " + th.getLocalizedMessage());
		}
	}

	/**
	 * Returns a DataSource that uses connection information from this object.
	 * Returned DataSource is not pooling its connections. It can be wrapped in
	 * PoolManager if pooling is needed.
	 */
	public DataSource makeDataSource(final ClassLoadingService classLoader) throws SQLException {

		// validate...
		if (getJdbcDriver() == null) {
			throw new SQLException("No JDBC driver set.");
		}

		if (getUrl() == null) {
			throw new SQLException("No DB URL set.");
		}

		if (!Util.isBlank(getPassword()) && Util.isBlank(getUserName())) {
			throw new SQLException("No username when password is set.");
		}

		// load driver...
		Driver driver;

		try {
			driver = classLoader.loadClass(Driver.class, getJdbcDriver()).newInstance();
		} catch (Throwable th) {
			th = Util.unwindException(th);
			throw new SQLException("Driver load error: " + th.getLocalizedMessage());
		}

		return new DriverDataSource(driver, getUrl(), getUserName(), getPassword());
	}

	/**
	 * Updates another DBConnectionInfo with this object's values.
	 */
	public boolean copyTo(final DBConnectionInfo dataSourceInfo) {
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
	 * <i>Currently doesn't set the adapter property. Need to change the UI to
	 * handle adapter via DataSourceInfo first, and then it should be safe to do
	 * an adapter update here. </i>
	 * </p>
	 */
	public boolean copyTo(final DataSourceInfo dataSourceInfo) {
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

	public boolean copyFrom(final DataSourceInfo dataSourceInfo) {
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