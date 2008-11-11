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

import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * A helper class that loads DBCP DataSource from properties.
 * 
 * @since 2.0
 */
class DBCPDataSourceBuilder {

    // DataSource Properties
    static final String DRIVER_CLASS_NAME = "driverClassName";
    static final String URL = "url";
    static final String USER_NAME = "username";
    static final String PASSWORD = "password";

    // Connection Pool Properties
    static final String MAX_ACTIVE = "maxActive";
    static final String MIN_IDLE = "minIdle";
    static final String MAX_IDLE = "maxIdle";
    static final String MAX_WAIT = "maxWait";
    static final String VALIDATION_QUERY = "validationQuery";
    static final String TEST_ON_BORROW = "testOnBorrow";
    static final String TEST_ON_RETURN = "testOnReturn";
    static final String TEST_IDLE = "testWhileIdle";
    static final String TIME_BETWEEN_EVICTIONS = "timeBetweenEvictionRunsMillis";
    static final String NUM_TEST_PER_EVICTION = "numTestsPerEvictionRun";
    static final String MIN_EVICTABLE_TIME = "minEvictableIdleTimeMillis";
    static final String EXHAUSTED_ACTION = "whenExhaustedAction";
    static final String AUTO_COMMIT = "defaultAutoCommit";
    static final String READ_ONLY = "defaultReadOnly";
    static final String TRANSACTION_ISOLATION = "defaultTransactionIsolation";
    static final String CONNECTION_NOWRAP = "accessToUnderlyingConnectionAllowed";
    static final String CATALOG = "defaultCatalog";

    // PreparedStatementPool properties

    static final String POOL_PS = "poolPreparedStatements";
    static final String PS_MAX_ACTIVE = "ps.maxActive";
    static final String PS_MAX_IDLE = "ps.maxIdle";
    static final String PS_MAX_TOTAL = "ps.maxTotal";
    static final String PS_MAX_WAIT = "ps.maxWait";
    static final String PS_MIN_EVICTABLE_TIME = "ps.minEvictableIdleTimeMillis";
    static final String PS_NUM_TEST_PER_EVICTION = "ps.numTestsPerEvictionRun";
    static final String PS_TEST_ON_BORROW = "ps.testOnBorrow";
    static final String PS_TEST_ON_RETURN = "ps.testOnReturn";
    static final String PS_TEST_IDLE = "ps.testWhileIdle";
    static final String PS_TIME_BETWEEN_EVICTIONS = "ps.timeBetweenEvictionRunsMillis";
    static final String PS_EXHAUSTED_ACTION = "ps.whenExhaustedAction";

    private DBCPDataSourceProperties config;

    DBCPDataSourceBuilder(DBCPDataSourceProperties properties) {
        this.config = properties;
    }

    DataSource createDataSource() {
        boolean connectionNoWrap = config.getBoolean(CONNECTION_NOWRAP, false);
        ObjectPool connectionPool = createConnectionPool();
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        dataSource.setAccessToUnderlyingConnectionAllowed(connectionNoWrap);

        return dataSource;
    }

    private void loadDriverClass() {
        String driver = config.getString(DRIVER_CLASS_NAME, true);
        try {
            Class.forName(driver);
        }
        catch (ClassNotFoundException e) {
            throw new ConfigurationException("Error loading driver " + driver, e);
        }
    }

    private ObjectPool createConnectionPool() {

        ConnectionFactory factory = createConnectionFactory();
        GenericObjectPool.Config poolConfig = createConnectionPoolConfig();
        KeyedObjectPoolFactory statementPool = createPreparedStatementPool();

        String validationQuery = config.getString(VALIDATION_QUERY);
        boolean defaultReadOnly = config.getBoolean(READ_ONLY, false);
        boolean defaultAutoCommit = config.getBoolean(AUTO_COMMIT, false);
        int defaultTransactionIsolation = config.getTransactionIsolation(
                TRANSACTION_ISOLATION,
                Connection.TRANSACTION_SERIALIZABLE);
        String defaultCatalog = config.getString(CATALOG);

        ObjectPool connectionPool = new GenericObjectPool(null, poolConfig);

        // a side effect of PoolableConnectionFactory constructor call is that newly
        // created factory object is assigned to "connectionPool", which is definitely a
        // very confusing part of DBCP - new object is not visibly assigned to anything,
        // still it is preserved...
        new PoolableConnectionFactory(
                factory,
                connectionPool,
                statementPool,
                validationQuery,
                defaultReadOnly ? Boolean.TRUE : Boolean.FALSE,
                defaultAutoCommit,
                defaultTransactionIsolation,
                defaultCatalog,
                null);

        return connectionPool;
    }

    private ConnectionFactory createConnectionFactory() {
        loadDriverClass();
        String url = config.getString(URL, true);
        String userName = config.getString(USER_NAME);
        String password = config.getString(PASSWORD);
        return new DriverManagerConnectionFactory(url, userName, password);
    }

    private KeyedObjectPoolFactory createPreparedStatementPool() {

        if (!config.getBoolean("poolPreparedStatements", false)) {
            return null;
        }

        // the GenericKeyedObjectPool.Config object isn't used because
        // although it has provision for the maxTotal parameter when
        // passed to the GenericKeyedObjectPoolFactory constructor
        // this parameter is not being properly set as a default for
        // creating prepared statement pools

        int maxActive = config
                .getInt(PS_MAX_ACTIVE, GenericObjectPool.DEFAULT_MAX_ACTIVE);
        byte whenExhaustedAction = config.getWhenExhaustedAction(
                PS_EXHAUSTED_ACTION,
                GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION);
        long maxWait = config.getLong(PS_MAX_WAIT, GenericObjectPool.DEFAULT_MAX_WAIT);
        int maxIdle = config.getInt(PS_MAX_IDLE, GenericObjectPool.DEFAULT_MAX_IDLE);
        int maxTotal = config.getInt(PS_MAX_TOTAL, 1);

        boolean testOnBorrow = config.getBoolean(
                PS_TEST_ON_BORROW,
                GenericObjectPool.DEFAULT_TEST_ON_BORROW);
        boolean testOnReturn = config.getBoolean(
                PS_TEST_ON_RETURN,
                GenericObjectPool.DEFAULT_TEST_ON_RETURN);

        long timeBetweenEvictionRunsMillis = config.getLong(
                PS_TIME_BETWEEN_EVICTIONS,
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        int numTestsPerEvictionRun = config.getInt(
                PS_NUM_TEST_PER_EVICTION,
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);

        long minEvictableIdleTimeMillis = config.getLong(
                PS_MIN_EVICTABLE_TIME,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);

        boolean testWhileIdle = config.getBoolean(
                PS_TEST_IDLE,
                GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);

        return new GenericKeyedObjectPoolFactory(
                null,
                maxActive,
                whenExhaustedAction,
                maxWait,
                maxIdle,
                maxTotal,
                testOnBorrow,
                testOnReturn,
                timeBetweenEvictionRunsMillis,
                numTestsPerEvictionRun,
                minEvictableIdleTimeMillis,
                testWhileIdle);
    }

    private GenericObjectPool.Config createConnectionPoolConfig() {
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();

        poolConfig.maxIdle = config.getInt(MAX_IDLE, GenericObjectPool.DEFAULT_MAX_IDLE);
        poolConfig.minIdle = config.getInt(MIN_IDLE, GenericObjectPool.DEFAULT_MIN_IDLE);
        poolConfig.maxActive = config.getInt(
                MAX_ACTIVE,
                GenericObjectPool.DEFAULT_MAX_ACTIVE);
        poolConfig.maxWait = config.getLong(MAX_WAIT, GenericObjectPool.DEFAULT_MAX_WAIT);

        poolConfig.testOnBorrow = config.getBoolean(
                TEST_ON_BORROW,
                GenericObjectPool.DEFAULT_TEST_ON_BORROW);
        poolConfig.testOnReturn = config.getBoolean(
                TEST_ON_RETURN,
                GenericObjectPool.DEFAULT_TEST_ON_RETURN);
        poolConfig.testWhileIdle = config.getBoolean(
                TEST_IDLE,
                GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);

        poolConfig.timeBetweenEvictionRunsMillis = config.getLong(
                TIME_BETWEEN_EVICTIONS,
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        poolConfig.numTestsPerEvictionRun = config.getInt(
                NUM_TEST_PER_EVICTION,
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
        poolConfig.minEvictableIdleTimeMillis = config.getLong(
                MIN_EVICTABLE_TIME,
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);

        poolConfig.whenExhaustedAction = config.getWhenExhaustedAction(
                EXHAUSTED_ACTION,
                GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION);

        return poolConfig;
    }
}
