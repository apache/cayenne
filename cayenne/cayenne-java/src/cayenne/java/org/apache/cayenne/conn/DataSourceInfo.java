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


package org.apache.cayenne.conn;

import java.io.Serializable;

import org.apache.cayenne.util.Util;

/** 
 * Helper JavaBean class that holds DataSource login information. 
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @author Andrei Adamchik
 */
public class DataSourceInfo implements Cloneable, Serializable {	
	protected String userName;
	protected String password;
	protected String jdbcDriver;
	protected String dataSourceUrl;
	protected String adapterClassName;
	protected int minConnections = 1;
	protected int maxConnections = 1;

	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (obj.getClass() != this.getClass())
			return false;

		DataSourceInfo dsi = (DataSourceInfo) obj;
		if (!Util.nullSafeEquals(this.userName, dsi.userName))
			return false;

		if (!Util.nullSafeEquals(this.password, dsi.password))
			return false;

		if (!Util.nullSafeEquals(this.jdbcDriver, dsi.jdbcDriver))
			return false;

		if (!Util.nullSafeEquals(this.dataSourceUrl, dsi.dataSourceUrl))
			return false;

		if (!Util.nullSafeEquals(this.adapterClassName, dsi.adapterClassName))
			return false;

		if (this.minConnections != dsi.minConnections)
			return false;

		if (this.maxConnections != dsi.maxConnections)
			return false;

		return true;
	}

	public DataSourceInfo cloneInfo() {
		try {
			return (DataSourceInfo) super.clone();
		} catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Cloning error", ex);
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf
			.append("[")
			.append(this.getClass().getName())
			.append(":")
			.append("\n   user name: ")
			.append(userName)
			.append("\n   password: ");

		if (password == null)
			buf.append("null");
		else
			buf.append("**********");

		buf
			.append("\n   driver: ")
			.append(jdbcDriver)
			.append("\n   db adapter class: ")
			.append(adapterClassName)
			.append("\n   url: ")
			.append(dataSourceUrl)
			.append("\n   min. connections: ")
			.append(minConnections)
			.append("\n   max. connections: ")
			.append(maxConnections)
			.append("\n]");

		return buf.toString();
	}

	public String getAdapterClassName() {
		return adapterClassName;
	}

	public void setAdapterClassName(String adapterClassName) {
		this.adapterClassName = adapterClassName;
	}

	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	public int getMinConnections() {
		return minConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setDataSourceUrl(String dataSourceUrl) {
		this.dataSourceUrl = dataSourceUrl;
	}

	public String getDataSourceUrl() {
		return dataSourceUrl;
	}

}
