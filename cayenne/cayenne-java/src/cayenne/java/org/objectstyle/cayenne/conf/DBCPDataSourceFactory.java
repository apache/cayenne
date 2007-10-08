/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.conf;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.ConfigurationException;

/**
 * An implementation of DataSourceFactory that instantiates a DataSource from Apache
 * Commons DBCP. Configured via a properties file specified by the location hint in the
 * modeler and under datasource attribute in cayenne.xml. Note that if location doesn't
 * have a ".properties" extension, such extension is added automatically. Sample
 * properties file:
 * 
 * <pre>
 * 
 *  
 *   
 *    
 *     
 *           #
 *           # This file defines the configuration properties for Commons DBCP pool
 *           # which is used for Cayenne during some of the test cases. 
 *           # For more info on setting parameters see the documentation for commons
 *           # dbcp and commons pool. The following prefixes are required:
 *           # cayenne.dbcp.&lt;param&gt; = configure the connection pool
 *           # cayenne.dbcp.ps.&lt;param&gt; = configuration for the prepared connection pools
 *           # that are associated with each pooled connections
 *          
 *          
 *           #driver class to use to connect to the database
 *           cayenne.dbcp.driverClassName=net.sourceforge.jtds.jdbc.Driver
 *          
 *           #url to the database, the parameters should be part of the connection string
 *           #and not here
 *           cayenne.dbcp.url=jdbc:jtds:sqlserver://192.168.20.2:1433/x_test2;TDS=8.0
 *          
 *           #username to use to connect to the database
 *           cayenne.dbcp.username=garyj
 *          
 *           #password to use to connect to the database
 *           cayenne.dbcp.password=somepass
 *          
 *           #maximum number of active connections
 *           cayenne.dbcp.maxActive=500
 *          
 *           #minimum number of idle connections
 *           cayenne.dbcp.minIdle=10
 *          
 *           #maximum number of active connections that can remain idle in the pool
 *           cayenne.dbcp.maxIdle=10
 *          
 *           #maximum number of milliseconds to wait for a connection to be returned to the 
 *           #pool before throwing an exception when the connection is required and the pool
 *           #is exhaused of the active connections. -1 for indefinetly
 *           cayenne.dbcp.maxWait=10000
 *          
 *           #sql query to be used to validate connections from the pool. Must return
 *           #at least one row
 *           cayenne.dbcp.validationQuery=SELECT GETDATE()
 *          
 *           #should the object be validated when it is borrowed from the pool
 *           cayenne.dbcp.testOnBorrow=false
 *          
 *           #should the object be validated when it is returned to the pool
 *           cayenne.dbcp.testOnReturn=true
 *          
 *           #should the object be validated when it is idle
 *           cayenne.dbcp.testWhileIdle=true
 *          
 *           #number of milliseconds to sleep between runs of the idle object evictor thread
 *           cayenne.dbcp.timeBetweenEvictionRunsMillis=120000
 *          
 *          
 *           #number of objects to examin during each run of the idle object evictor
 *           cayenne.dbcp.numTestsPerEvictionRun=10
 *          
 *           #minimum time an object may sit idle in the pool before it is elegible for
 *           #an eviction
 *           cayenne.dbcp.minEvictableIdleTimeMillis=2000000
 *          
 *           #action to take the the pool is exhausted of all active connections
 *           #see GenericObjectPool class
 *           #this value can be set as either an int or a String the setter method
 *           #will attempt to convert the String value to it's resective representation
 *           #in the GenericObjectPool class and if successfull will use the byte 
 *           #value as the config paramter to the pool. If not the default value will
 *           #be used
 *           cayenne.dbcp.whenExhaustedAction=WHEN_EXHAUSTED_GROW
 *          
 *           #The default auto-commit state of connections created by this pool
 *           caynne.dbcp.defaultAutoCommit=false
 *          
 *           #Default read only state of connections created by the pool. Can be left
 *           #as null for driver default
 *           cayenne.dbcp.defaultReadOnly=true
 *          
 *          
 *           # Default TransactionIsolation state of connections created by this pool. This can
 *           # be either a String representation of the isolation level defined in the interface 
 *           # java.sql.Connection. Can be left as null for 
 *           # driver default
 *           cayenne.dbcp.defaultTransactionIsolation=TRANSACTION_SERIALIZABLE
 *          
 *           #If set to true the application will be able to get access to the
 *           #actual connection object which is normally wrapped by a poolable connections
 *           cayenne.dbcp.accessToUnderlyingConnectionAllowed=true
 *          
 *           #Default catalog of connections created by this pool
 *           cayenne.dbcp.defaultCatalog=someCat
 *          
 *           #Specifies whether prepared statments should be pooled
 *           cayenne.dbcp.poolPreparedStatements=true
 *          
 *          
 *           #Controlls the maximum number of objects that can be borrowed from the pool at 
 *           #one time
 *           cayenne.dbcp.ps.maxActive=500
 *          
 *           #Maximum number of idle objects in the pool
 *           cayenne.dbcp.ps.maxIdle=50
 *          
 *           #Maximum number of objects that can exist in the prepared statement pool at one time
 *           cayenne.dbcp.ps.maxTotal=600
 *          
 *          
 *           # Minimum number of milliseconds to wait for an objec the the pool of 
 *           # prepared statements is exhausted and the whenExhaustedAction is set to 
 *           # 1 (WHEN_EXHAUSTED_BLOCK)
 *           cayenne.dbcp.ps.maxWait=10000
 *          
 *          
 *           # Number of milliseconds an object can sit idle in the pool before it is 
 *           # elegible for eviction
 *           cayenne.dbcp.ps.minEvictableIdleTimeMillis=2000000
 *          
 *          
 *           #Number of idle objects that should be examined per eviction run
 *           cayenne.dbcp.ps.numTestsPerEvictionRun=20
 *          
 *          
 *           #Specifies whether objects should be validated before they are borrowed from this pool
 *           cayenne.dbcp.ps.testOnBorrow=false
 *          
 *           #Specifies whether objects should be validated when they are returned to the pool
 *           cayenne.dbcp.ps.testOnReturn=true
 *          
 *          
 *           #Specifies whether objects should be validated in the idle eviction thread
 *           cayenne.dbcp.ps.testWhileIdle=true
 *          
 *           #Specifies the time between the runs of the eviction thread
 *           cayenne.dbcp.ps.timeBetweenEvictionRunsMillis=120000
 *          
 *           # action to take when the the pool is exhausted of all active objects.
 *           # acceptable values are strings (WHEN_EXHAUSTED_FAIL, WHEN_EXHAUSTED_BLOCK (default), 
 *           # WHEN_EXHAUSTED_GROW), or their corresponding int values defined in commons-pool GenericObjectPool:
 *           cayenne.dbcp.ps.whenExhaustedAction=WHEN_EXHAUSTED_FAIL
 *      
 *     
 *    
 *   
 *  
 * </pre>
 * 
 * @since 1.2
 * @author Gary Jarrel
 */
public class DBCPDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = Logger.getLogger(DBCPDataSourceFactory.class);

    /**
     * Suffix of the properties file
     */
    private static final String SUFFIX = ".properties";

    /**
     * All the properties in the configuration properties file should be prefixed with
     * this prefix, namely cayenne.dbcp. The config parameter as set out in commons dbcp
     * configuration should follow this prefix.
     */
    public static final String PROPERTY_PREFIX = "cayenne.dbcp.";

    /**
     * The the properties in the configuration file, related to Prepared Statement pooling
     * and used to configure <code>KeyedObjectPoolFactory</code> are followed by this
     * prefix. This constants is set to PROPERTY_PREFIX + ps.
     */
    public static final String PS_PROPERTY_PREFIX = PROPERTY_PREFIX + "ps.";

    protected Configuration parentConfiguration;

    public void initializeWithParentConfiguration(Configuration parentConfiguration) {
        this.parentConfiguration = parentConfiguration;
    }

    /**
     * @deprecated since 1.2
     */
    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        return getDataSource(location);
    }

    /**
     * Creates a DBCP <code>PoolingDataSource</code>
     * 
     * @return <code>DataSource</code> which is an instance of
     *         <code>PoolingDataSource</code>
     * @throws Exception
     */
    public DataSource getDataSource(String location) throws Exception {

        if (!location.endsWith(SUFFIX)) {
            location = location.concat(SUFFIX);
        }

        logger.info("Loading DBCP properties from " + location);

        Properties properties = loadProperties(location);
        logger.info("Loaded DBCP properties: " + properties);

        loadDriverClass(properties);

        // build and assemble parts of DBCP DataSource...
        ConnectionFactory factory = createConnectionFactory(properties);
        KeyedObjectPoolFactory statementPool = createStatementPool(properties);

        GenericObjectPool.Config config = createPoolConfig(properties);

        // PoolableConnectionFactory properties
        String validationQuery = stringProperty(properties, "validationQuery");
        boolean defaultReadOnly = booleanProperty(properties, "defaultReadOnly", false);
        boolean defaultAutoCommit = booleanProperty(
                properties,
                "defaultAutoCommit",
                false);
        int defaultTransactionIsolation = defaultTransactionIsolation(
                properties,
                "defaultTransactionIsolation",
                Connection.TRANSACTION_SERIALIZABLE);
        String defaultCatalog = stringProperty(properties, "defaultCatalog");

        // a side effect of PoolableConnectionFactory constructor call is that newly
        // created factory object is assigned to "connectionPool", which is definitely a
        // very confusing part of DBCP - new object is not visibly assigned to anything,
        // still it is preserved...
        ObjectPool connectionPool = new GenericObjectPool(null, config);
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

        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);
        dataSource.setAccessToUnderlyingConnectionAllowed(booleanProperty(
                properties,
                "accessToUnderlyingConnectionAllowed",
                false));

        return dataSource;
    }

    /**
     * Loads driver class into driver manager.
     */
    void loadDriverClass(Properties properties) throws Exception {
        String driver = stringProperty(properties, "driverClassName");
        logger.info("loading JDBC driver class: " + driver);

        if (driver == null) {
            throw new NullPointerException("No value for required property: "
                    + PROPERTY_PREFIX
                    + "driverClassName");
        }
        Class.forName(driver);
    }

    KeyedObjectPoolFactory createStatementPool(Properties properties) throws Exception {

        if (!booleanProperty(properties, "poolPreparedStatements", false)) {
            return null;
        }

        // the GenericKeyedObjectPool.Config object isn't used because
        // although it has provision for the maxTotal parameter when
        // passed to the GenericKeyedObjectPoolFactory constructor
        // this parameter is not being properly set as a default for
        // creating prepared statement pools

        int maxActive = intProperty(
                properties,
                "ps.maxActive",
                GenericObjectPool.DEFAULT_MAX_ACTIVE);
        byte whenExhaustedAction = whenExhaustedAction(
                properties,
                "ps.whenExhaustedAction",
                GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION);

        long maxWait = longProperty(
                properties,
                "ps.maxWait",
                GenericObjectPool.DEFAULT_MAX_WAIT);

        int maxIdle = intProperty(
                properties,
                "ps.maxIdle",
                GenericObjectPool.DEFAULT_MAX_IDLE);

        int maxTotal = intProperty(properties, "ps.maxTotal", 1);

        boolean testOnBorrow = booleanProperty(
                properties,
                "ps.testOnBorrow",
                GenericObjectPool.DEFAULT_TEST_ON_BORROW);
        boolean testOnReturn = booleanProperty(
                properties,
                "ps.testOnReturn",
                GenericObjectPool.DEFAULT_TEST_ON_RETURN);

        long timeBetweenEvictionRunsMillis = longProperty(
                properties,
                "ps.timeBetweenEvictionRunsMillis",
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        int numTestsPerEvictionRun = intProperty(
                properties,
                "ps.numTestsPerEvictionRun",
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);

        long minEvictableIdleTimeMillis = longProperty(
                properties,
                "ps.minEvictableIdleTimeMillis",
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);

        boolean testWhileIdle = booleanProperty(
                properties,
                "ps.testWhileIdle",
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

    ConnectionFactory createConnectionFactory(Properties properties) {
        String url = stringProperty(properties, "url");
        String userName = stringProperty(properties, "username");
        String password = stringProperty(properties, "password");

        // sanity check
        if (url == null) {
            throw new NullPointerException("No value for required property: "
                    + PROPERTY_PREFIX
                    + "url");
        }

        return new DriverManagerConnectionFactory(url, userName, password);
    }

    GenericObjectPool.Config createPoolConfig(Properties properties) throws Exception {
        GenericObjectPool.Config config = new GenericObjectPool.Config();

        config.maxIdle = intProperty(
                properties,
                "maxIdle",
                GenericObjectPool.DEFAULT_MAX_IDLE);
        config.minIdle = intProperty(
                properties,
                "minIdle",
                GenericObjectPool.DEFAULT_MIN_IDLE);
        config.maxActive = intProperty(
                properties,
                "maxActive",
                GenericObjectPool.DEFAULT_MAX_ACTIVE);
        config.maxWait = longProperty(
                properties,
                "maxWait",
                GenericObjectPool.DEFAULT_MAX_WAIT);

        config.testOnBorrow = booleanProperty(
                properties,
                "testOnBorrow",
                GenericObjectPool.DEFAULT_TEST_ON_BORROW);
        config.testOnReturn = booleanProperty(
                properties,
                "testOnReturn",
                GenericObjectPool.DEFAULT_TEST_ON_RETURN);
        config.testWhileIdle = booleanProperty(
                properties,
                "testWhileIdle",
                GenericObjectPool.DEFAULT_TEST_WHILE_IDLE);

        config.timeBetweenEvictionRunsMillis = longProperty(
                properties,
                "timeBetweenEvictionRunsMillis",
                GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        config.numTestsPerEvictionRun = intProperty(
                properties,
                "numTestsPerEvictionRun",
                GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
        config.minEvictableIdleTimeMillis = longProperty(
                properties,
                "minEvictableIdleTimeMillis",
                GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);

        config.whenExhaustedAction = whenExhaustedAction(
                properties,
                "whenExhaustedAction",
                GenericObjectPool.DEFAULT_WHEN_EXHAUSTED_ACTION);

        return config;
    }

    Properties loadProperties(String location) throws IOException {

        Properties properties = new Properties();
        InputStream in = getInputStream(location);
        if (in == null) {
            throw new ConfigurationException("DBCP properties file not found: "
                    + location);
        }

        try {
            properties.load(in);
        }
        finally {
            try {
                in.close();
            }
            catch (IOException ignore) {
            }
        }

        return properties;
    }

    int defaultTransactionIsolation(
            Properties properties,
            String property,
            int defaultValue) {

        String value = stringProperty(properties, property);

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

    byte whenExhaustedAction(Properties properties, String property, byte defaultValue)
            throws Exception {

        String value = stringProperty(properties, property);

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

    String stringProperty(Properties properties, String property) {
        return properties.getProperty(PROPERTY_PREFIX + property);
    }

    boolean booleanProperty(Properties properties, String property, boolean defaultValue) {
        String value = stringProperty(properties, property);
        return (value != null) ? "true".equalsIgnoreCase(stringProperty(
                properties,
                property)) : defaultValue;
    }

    int intProperty(Properties properties, String property, int defaultValue) {
        String value = stringProperty(properties, property);

        try {
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    long longProperty(Properties properties, String property, long defaultValue) {
        String value = stringProperty(properties, property);
        try {
            return (value != null) ? Long.parseLong(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    byte byteProperty(Properties properties, String property, byte defaultValue) {
        String value = stringProperty(properties, property);
        try {
            return (value != null) ? Byte.parseByte(value) : defaultValue;
        }
        catch (NumberFormatException nfex) {
            return defaultValue;
        }
    }

    /**
     * Returns an input stream for the file corresponding to location.
     */
    InputStream getInputStream(String location) {
        if (this.parentConfiguration == null) {
            throw new ConfigurationException(
                    "No parent Configuration set - cannot continue.");
        }

        return this.parentConfiguration.getResourceLocator().findResourceStream(location);
    }
}