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

import org.apache.cayenne.conf.PasswordEncoding;
import org.apache.cayenne.conf.PlainTextPasswordEncoder;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper JavaBean class that holds DataSource login information.
 * 
 */
public class DataSourceInfo implements Cloneable, Serializable {

    private static Log logger = LogFactory.getLog(DataSourceInfo.class);

    protected String userName;
    protected String password;
    protected String jdbcDriver;
    protected String dataSourceUrl;
    protected String adapterClassName;
    protected int minConnections = 1;
    protected int maxConnections = 1;

    // Constants for passwordLocation
    public static final String PASSWORD_LOCATION_CLASSPATH = "classpath";
    public static final String PASSWORD_LOCATION_EXECUTABLE = "executable";
    public static final String PASSWORD_LOCATION_MODEL = "model";
    public static final String PASSWORD_LOCATION_URL = "url";

    // Extended parameters
    protected String passwordEncoderClass = PasswordEncoding.standardEncoders[0];
    protected String passwordEncoderKey = "";
    protected String passwordLocation = PASSWORD_LOCATION_MODEL;
    protected String passwordSourceExecutable = "";
    protected String passwordSourceFilename = "";
    protected final String passwordSourceModel = "Not Applicable";
    protected String passwordSourceUrl = "";

    @Override
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
        if (!Util.nullSafeEquals(this.passwordEncoderClass, dsi.passwordEncoderClass))
            return false;
        if (!Util.nullSafeEquals(this.passwordEncoderKey, dsi.passwordEncoderKey))
            return false;
        if (!Util.nullSafeEquals(this.passwordSourceFilename, dsi.passwordSourceFilename))
            return false;
        if (!Util.nullSafeEquals(this.passwordSourceModel, dsi.passwordSourceModel))
            return false;
        if (!Util.nullSafeEquals(this.passwordSourceUrl, dsi.passwordSourceUrl))
            return false;
        if (!Util.nullSafeEquals(this.passwordLocation, dsi.passwordLocation))
            return false;

        return true;
    }

    public DataSourceInfo cloneInfo() {
        try {
            return (DataSourceInfo) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Cloning error", ex);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("[").append(this.getClass().getName()).append(":").append(
                "\n   user name: ").append(userName).append("\n   password: ");

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
                .append(maxConnections);

        if (!PlainTextPasswordEncoder.class.getName().equals(passwordEncoderClass)) {
            buf.append("\n   encoder class: ").append(passwordEncoderClass).append(
                    "\n   encoder key: ").append(passwordEncoderKey);
        }

        if (!PASSWORD_LOCATION_MODEL.equals(passwordLocation)) {
            buf.append("\n   password location: ").append(passwordLocation).append(
                    "\n   password source: ").append(getPasswordSource());
        }

        buf.append("\n]");
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

    public PasswordEncoding getPasswordEncoder() {
        try {
            return (PasswordEncoding)
                Util.getJavaClass(getPasswordEncoderClass()).newInstance();
        }
        catch (InstantiationException e) {
            ; // Swallow it -- no need to throw/etc.
        }
        catch (IllegalAccessException e) {
            ; // Swallow it -- no need to throw/etc.
        }
        catch (ClassNotFoundException e) {
            ; // Swallow it -- no need to throw/etc.
        }

        logger.error("Failed to obtain specified Password Encoder '" +
                     getPasswordEncoderClass() + "' -- please check CLASSPATH");

        return null;
    }

    /**
     * @return the passwordEncoderClass
     */
    public String getPasswordEncoderClass() {
        return passwordEncoderClass;
    }

    /**
     * @param passwordEncoderClass the passwordEncoderClass to set
     */
    public void setPasswordEncoderClass(String passwordEncoderClass) {
        if (passwordEncoderClass == null)
            this.passwordEncoderClass = PasswordEncoding.standardEncoders[0];
        else
            this.passwordEncoderClass = passwordEncoderClass;
    }

    /**
     * @return the passwordEncoderKey
     */
    public String getPasswordEncoderKey() {
        return passwordEncoderKey;
    }

    /**
     * @param passwordEncoderKey the passwordEncoderKey to set
     */
    public void setPasswordEncoderKey(String passwordEncoderKey) {
        this.passwordEncoderKey = passwordEncoderKey;
    }

    /**
     * @return the passwordLocationFilename
     */
    public String getPasswordSourceFilename() {
        return passwordSourceFilename;
    }

    /**
     * @param passwordSourceFilename the passwordSourceFilename to set
     */
    public void setPasswordSourceFilename(String passwordSourceFilename) {
        this.passwordSourceFilename = passwordSourceFilename;
    }

    /**
     * @return the passwordLocationModel
     */
    public String getPasswordSourceModel() {
        return passwordSourceModel;
    }

    /**
     * @return the passwordLocationUrl
     */
    public String getPasswordSourceUrl() {
        return passwordSourceUrl;
    }

    /**
     * @param passwordSourceUrl the passwordSourceUrl to set
     */
    public void setPasswordSourceUrl(String passwordSourceUrl) {
        this.passwordSourceUrl = passwordSourceUrl;
    }

    /**
     * @return the passwordLocationExecutable
     */
    public String getPasswordSourceExecutable() {
        return passwordSourceExecutable;
    }

    /**
     * @param passwordSourceExecutable the passwordSourceExecutable to set
     */
    public void setPasswordSourceExecutable(String passwordSourceExecutable) {
        this.passwordSourceExecutable = passwordSourceExecutable;
    }

    public String getPasswordSource() {
        if (getPasswordLocation().equals(PASSWORD_LOCATION_CLASSPATH))
            return getPasswordSourceFilename();
        else if (getPasswordLocation().equals(PASSWORD_LOCATION_EXECUTABLE))
            return getPasswordSourceExecutable();
        else if (getPasswordLocation().equals(PASSWORD_LOCATION_MODEL))
            return getPasswordSourceModel();
        else if (getPasswordLocation().equals(PASSWORD_LOCATION_URL))
            return getPasswordSourceUrl();

        throw new RuntimeException("Invalid password source detected");
    }

    public void setPasswordSource(String passwordSource) {
        // The location for the model is omitted since it cannot change
        if (getPasswordLocation().equals(PASSWORD_LOCATION_CLASSPATH))
            setPasswordSourceFilename(passwordSource);
        else if (getPasswordLocation().equals(PASSWORD_LOCATION_EXECUTABLE))
            setPasswordSourceExecutable(passwordSource);
        else if (getPasswordLocation().equals(PASSWORD_LOCATION_URL))
            setPasswordSourceUrl(passwordSource);
    }

    /**
     * @return the passwordLocation
     */
    public String getPasswordLocation() {
        return passwordLocation;
    }

    /**
     * @param passwordLocation the passwordLocation to set
     */
    public void setPasswordLocation(String passwordLocation) {
        if (passwordLocation == null)
            this.passwordLocation = DataSourceInfo.PASSWORD_LOCATION_MODEL;
        else
            this.passwordLocation = passwordLocation;
    }
}
