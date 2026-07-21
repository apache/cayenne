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
package org.apache.cayenne.datasource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * An entry point for manually building DataSources. Produces instances of Cayenne own DataSource. Alternatively, you
 * can use any JDBC-compliant third-party DataSources with Cayenne.
 *
 * @since 5.0
 */
public class CayenneDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CayenneDataSource.class);

    /**
     * Starts building a DataSource for the given database URL.
     */
    public static Builder of(String url) {
        return new Builder(url);
    }

    /**
     * Starts building a DataSource with settings taken from the following map keys:
     * <ul>
     * <li>cayenne.jdbc.url
     * <li>cayenne.jdbc.driver
     * <li>cayenne.jdbc.username
     * <li>cayenne.jdbc.password
     * <li>cayenne.jdbc.min_connections
     * <li>cayenne.jdbc.max_connections
     * <li>cayenne.jdbc.max_wait
     * <li>cayenne.jdbc.validation_query
     * </ul>
     * The URL property is required. Pooling is enabled if at least one of the connections count properties is set.
     * Within the Cayenne stack, use {@code RuntimeProperties.toMap()} to pass the runtime properties here.
     */
    public static Builder fromProperties(Map<String, String> properties) {
        Objects.requireNonNull(properties, "Null 'properties'");
        return fromResolvedProperties(properties, "");
    }

    /**
     * Starts building a DataSource with settings taken from a properties map, same as {@link #fromProperties(Map)},
     * but first checking properties with a ".nodeName" suffix (e.g. "cayenne.jdbc.url.mynode"), and falling back to
     * the unsuffixed ones.
     */
    public static Builder fromProperties(Map<String, String> properties, String nodeName) {
        Objects.requireNonNull(properties, "Null 'properties'");
        Objects.requireNonNull(nodeName, "Null 'nodeName'");
        return fromResolvedProperties(properties, "." + nodeName);
    }

    private static Builder fromResolvedProperties(Map<String, String> props, String suffix) {

        String url = prop(props, Constants.JDBC_URL_PROPERTY, suffix);
        if (url == null) {
            throw new CayenneRuntimeException("Missing DataSource URL property '%s%s'",
                    Constants.JDBC_URL_PROPERTY,
                    suffix);
        }

        Builder builder = new Builder(url)
                .userName(prop(props, Constants.JDBC_USERNAME_PROPERTY, suffix))
                .password(prop(props, Constants.JDBC_PASSWORD_PROPERTY, suffix));

        String driverClassName = prop(props, Constants.JDBC_DRIVER_PROPERTY, suffix);
        if (driverClassName != null) {
            builder.driverClass(driverClassName);
        }

        int minConnections = intProp(props, Constants.JDBC_MIN_CONNECTIONS_PROPERTY, suffix, -1);
        int maxConnections = intProp(props, Constants.JDBC_MAX_CONNECTIONS_PROPERTY, suffix, -1);
        if (minConnections >= 0 || maxConnections >= 0) {
            int min = minConnections >= 0 ? minConnections : 1;
            builder.pool(min, maxConnections >= 0 ? maxConnections : Math.max(min, 1));
        }

        long maxQueueWaitTime = longProp(props, Constants.JDBC_MAX_QUEUE_WAIT_TIME, suffix, -1);
        if (maxQueueWaitTime >= 0) {
            builder.maxQueueWaitTime(maxQueueWaitTime);
        }

        String validationQuery = prop(props, Constants.JDBC_VALIDATION_QUERY_PROPERTY, suffix);
        if (validationQuery != null) {
            builder.validationQuery(validationQuery);
        }

        return builder;
    }

    private static String prop(Map<String, String> props, String name, String suffix) {
        // fallback to default property shared by all data nodes
        String value = props.get(name + suffix);
        return value != null ? value : props.get(name);
    }

    private static int intProp(Map<String, String> props, String name, String suffix, int defaultValue) {
        String value = prop(props, name, suffix);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long longProp(Map<String, String> props, String name, String suffix, long defaultValue) {
        String value = prop(props, name, suffix);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private CayenneDataSource() {
    }

    public static class Builder {

        private final String url;
        private String userName;
        private String password;
        private String driverClass;

        private Integer minConnections;
        private Integer maxConnections;
        private Long maxQueueWaitTime;
        private String validationQuery;

        private Builder(String url) {
            this.url = Objects.requireNonNull(url, "Null 'url'");
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /**
         * Sets a class name of the JDBC driver. This i optional and is only used in special circumstances. Normally,
         * JDBC-compliant drivers are discovered automatically, and resolved based on the URL.
         */
        public Builder driverClass(String driverClassName) {
            this.driverClass = driverClassName;
            return this;
        }

        /**
         * Turns the built DataSource into a connection pool with the given connection count bounds.
         */
        public Builder pool(int minConnections, int maxConnections) {

            if (minConnections < 0) {
                throw new CayenneRuntimeException("Minimum number of connections can not be negative (%d)", minConnections);
            }

            if (maxConnections < 0) {
                throw new CayenneRuntimeException("Maximum number of connections can not be negative (%d)", maxConnections);
            }

            if (minConnections > maxConnections) {
                throw new CayenneRuntimeException("Minimum number of connections can not be bigger than maximum.");
            }

            this.minConnections = minConnections;
            this.maxConnections = maxConnections;
            return this;
        }

        /**
         * Sets a maximum time in milliseconds a connection request may wait for a free connection in the pool. Ignored
         * unless {@link #pool(int, int)} is also called.
         */
        public Builder maxQueueWaitTime(long maxQueueWaitTime) {
            this.maxQueueWaitTime = maxQueueWaitTime;
            return this;
        }

        /**
         * Sets a SQL query used by the pool to check connection health. Ignored unless {@link #pool(int, int)} is also
         * called.
         */
        public Builder validationQuery(String validationQuery) {
            this.validationQuery = validationQuery;
            return this;
        }

        /**
         * Builds a DataSource that is pooling if {@link #pool(int, int)} was called, and non-pooling otherwise. A
         * pooling DataSource is {@link AutoCloseable} and must be explicitly closed by the caller when no
         * longer in use.
         */
        public DataSource build() {

            if (minConnections == null) {
                if (maxQueueWaitTime != null) {
                    LOGGER.warn("'maxQueueWaitTime' is ignored for a non-pooling DataSource. Call 'pool(min, max)' to enable pooling.");
                }

                if (validationQuery != null) {
                    LOGGER.warn("'validationQuery' is ignored for a non-pooling DataSource. Call 'pool(min, max)' to enable pooling.");
                }
            }

            DataSource nonPooling = new DriverDataSource(loadDriver(), url, userName, password);
            return minConnections != null ? pool(nonPooling) : nonPooling;
        }

        private DataSource pool(DataSource nonPooling) {

            PoolingDataSourceParameters parameters = new PoolingDataSourceParameters();
            parameters.setMinConnections(minConnections);
            parameters.setMaxConnections(maxConnections);
            parameters.setMaxQueueWaitTime(
                    maxQueueWaitTime != null ? maxQueueWaitTime : UnmanagedPoolingDataSource.MAX_QUEUE_WAIT_DEFAULT);
            parameters.setValidationQuery(validationQuery);

            return new ManagedPoolingDataSource(new UnmanagedPoolingDataSource(nonPooling, parameters));
        }

        private Driver loadDriver() {

            if (driverClass == null) {
                try {
                    return DriverManager.getDriver(url);
                } catch (SQLException ex) {
                    throw new CayenneRuntimeException("No registered JDBC driver accepting the URL '%s': %s",
                            url,
                            ex.getMessage());
                }
            }

            return DriverManager.drivers()
                    .filter(d -> d.getClass().getName().equals(driverClass))
                    .findFirst()
                    .orElseGet(this::instantiateDriver);
        }

        private Driver instantiateDriver() {

            Class<?> driverClass;
            try {
                // note: implicitly using current class's ClassLoader ....
                driverClass = Class.forName(this.driverClass);
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Can not load JDBC driver named '%s': %s",
                        this.driverClass,
                        ex.getMessage());
            }

            try {
                return (Driver) driverClass.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error instantiating driver '%s': %s",
                        this.driverClass,
                        ex.getMessage());
            }
        }
    }
}
