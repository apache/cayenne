/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.conn;

import java.io.Serializable;

import org.objectstyle.cayenne.util.Util;

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
