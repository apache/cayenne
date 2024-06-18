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

package org.apache.cayenne.configuration;

import java.io.Serializable;
import java.util.Objects;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Helper JavaBean class that holds DataSource information for the Cayenne-managed DataSource.
 * @since 5.0
 */
public class DataSourceDescriptor implements Serializable, XMLSerializable {

    protected String jdbcDriver;
    protected String dataSourceUrl;
    protected String userName;
    protected String password;
    protected int minConnections = 1;
    protected int maxConnections = 1;

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    public void setDataSourceUrl(String dataSourceUrl) {
        this.dataSourceUrl = dataSourceUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSourceDescriptor that = (DataSourceDescriptor) o;
        return minConnections == that.minConnections
                && maxConnections == that.maxConnections
                && Objects.equals(jdbcDriver, that.jdbcDriver)
                && Objects.equals(dataSourceUrl, that.dataSourceUrl)
                && Objects.equals(userName, that.userName)
                && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jdbcDriver, dataSourceUrl, userName, password, minConnections, maxConnections);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("data-source")
                .start("driver").attribute("value", jdbcDriver).end()
                .start("url").attribute("value", dataSourceUrl).end()
                .start("connectionPool")
                    .attribute("min", minConnections)
                    .attribute("max", maxConnections).end()
                .start("login")
                    .attribute("userName", userName)
                    .attribute("password", password).end()
                .end();
    }

    @Override
    public String toString() {
        return "[" + getClass().getName() + ":" +
                "\n   driver: " + jdbcDriver +
                "\n   url: " + dataSourceUrl +
                "\n   user name: " + userName +
                "\n   password: " + "**********" +
                "\n   min. connections: " + minConnections +
                "\n   max. connections: " + maxConnections +
                "\n]";
    }
}
