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
package org.apache.cayenne.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.Properties;

import org.apache.cayenne.ConfigurationException;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * A helper class providing access to DBCP properties.
 * 
 * @since 2.0
 */
class DBCPDataSourceProperties {

    private static final String PROPERTY_PREFIX = "cayenne.dbcp.";

    private Properties properties;

    /**
     * Loads properties from the specified location.
     */
    static Properties loadProperties(ResourceFinder resourceLocator, String location)
            throws IOException {

        URL url = resourceLocator.getResource(location);

        // try appending ".properties" extension..
        if (url == null && !location.endsWith(".properties")) {
            url = resourceLocator.getResource(location + ".properties");
        }

        if (url == null) {
            throw new ConfigurationException("DBCP properties file not found: "
                    + location);
        }

        Properties properties = new Properties();
        InputStream in = url.openStream();
        try {
            properties.load(in);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException e) {
            }
        }

        return properties;
    }

    DBCPDataSourceProperties(ResourceFinder resourceLocator, String location)
            throws IOException {
        this(loadProperties(resourceLocator, location));
    }

    DBCPDataSourceProperties(Properties properties) {
        this.properties = properties;
    }

    Properties getProperties() {
        return properties;
    }

    String getString(String property, boolean required) {
        String value = properties.getProperty(PROPERTY_PREFIX + property);

        if (required && value == null) {
            throw new ConfigurationException("No value for required property: "
                    + PROPERTY_PREFIX
                    + property);
        }

        return value;
    }

    String getString(String property) {
        return getString(property, false);
    }

    boolean getBoolean(String property, boolean defaultValue) {
        String value = getString(property);
        return (value != null)
                ? "true".equalsIgnoreCase(getString(property))
                : defaultValue;
    }

    int getInt(String property, int defaultValue) {
        String value = getString(property);

        try {
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    long getLong(String property, long defaultValue) {
        String value = getString(property);
        try {
            return (value != null) ? Long.parseLong(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    byte getByte(String property, byte defaultValue) {
        String value = getString(property);
        try {
            return (value != null) ? Byte.parseByte(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    byte getWhenExhaustedAction(String property, byte defaultValue)
            throws ConfigurationException {

        String value = getString(property);

        if (value == null) {
            return defaultValue;
        }

        // try byte...
        try {
            return Byte.parseByte(value);
        }
        catch (NumberFormatException nfex) {
            // try symbolic
            try {
                return GenericObjectPool.class.getField(value).getByte(null);
            }
            catch (Throwable th) {
                throw new ConfigurationException("Invalid 'whenExhaustedAction': "
                        + value);
            }
        }
    }

    int getTransactionIsolation(String property, int defaultValue) {

        String value = getString(property);

        if (value == null) {
            return defaultValue;
        }

        // try int...
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException nfex) {
            // try symbolic
            try {
                return Connection.class.getField(value).getInt(null);
            }
            catch (Throwable th) {
                throw new ConfigurationException(
                        "Invalid 'defaultTransactionIsolation': " + value);
            }
        }
    }

}
