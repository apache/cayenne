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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.ScopeEventListener;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * PoolManager is a Cayenne implementation of a pooling DataSource.
 */
public class PoolManager implements ScopeEventListener, DataSource,
        ConnectionEventListener {

    /**
     * Defines a maximum time in milliseconds that a connection request could wait in the
     * connection queue. After this period expires, an exception will be thrown in the
     * calling method. 
     */
    public static final int MAX_QUEUE_WAIT_DEFAULT = 20000;

    /**
     * An exception indicating that a connection request waiting in the queue
     * timed out and was unable to obtain a connection.
     */
    public static class ConnectionUnavailableException extends SQLException {
        private static final long serialVersionUID = 1063973806941023165L;

        public ConnectionUnavailableException(String message) {
    		super(message);
    	}
    }
    
    protected ConnectionPoolDataSource poolDataSource;
    protected int minConnections;
    protected int maxConnections;
    protected String dataSourceUrl;
    protected String jdbcDriver;
    protected String password;
    protected String userName;

    protected List<PooledConnection> unusedPool;
    protected List<PooledConnection> usedPool;

    private PoolMaintenanceThread poolMaintenanceThread;

    private boolean shuttingDown;
    private long maxQueueWaitTime;
    
    /**
     * Creates new PoolManager using org.apache.cayenne.conn.PoolDataSource for an
     * underlying ConnectionPoolDataSource.
     * 
     * @deprecated since 3.2 This constructor causes implicit class loading that should avoided.
     */
    @Deprecated
    public PoolManager(String jdbcDriver, String dataSourceUrl, int minCons, int maxCons,
            String userName, String password) throws SQLException {

        this(jdbcDriver, dataSourceUrl, minCons, maxCons, userName, password, null, MAX_QUEUE_WAIT_DEFAULT);
    }

    /**
     * @deprecated since 3.2 This constructor causes implicit class loading that should avoided.
     */
    @Deprecated
    public PoolManager(String jdbcDriver, String dataSourceUrl, int minCons, int maxCons,
            String userName, String password, JdbcEventLogger logger, long maxQueueWaitTime) throws SQLException {

        if (logger != null) {
            DataSourceInfo info = new DataSourceInfo();
            info.setJdbcDriver(jdbcDriver);
            info.setDataSourceUrl(dataSourceUrl);
            info.setMinConnections(minCons);
            info.setMaxConnections(maxCons);
            info.setUserName(userName);
            info.setPassword(password);
            logger.logPoolCreated(info);
        }

        this.jdbcDriver = jdbcDriver;
        this.dataSourceUrl = dataSourceUrl;
        DriverDataSource driverDS = new DriverDataSource(jdbcDriver, dataSourceUrl);
        driverDS.setLogger(logger);
        PoolDataSource poolDS = new PoolDataSource(driverDS);
        init(poolDS, minCons, maxCons, userName, password, maxQueueWaitTime);
    }
    
    /**
     * Creates new PoolManager with the specified policy for connection pooling and a
     * ConnectionPoolDataSource object.
     * 
     * @param poolDataSource data source for pooled connections
     * @param minCons Non-negative integer that specifies a minimum number of open
     *            connections to keep in the pool at all times
     * @param maxCons Non-negative integer that specifies maximum number of simultaneuosly
     *            open connections
     * @throws SQLException if pool manager can not be created.
     * @deprecated since 3.2 use {@link #PoolManager(ConnectionPoolDataSource, int, int, String, String, long)}
     */
    public PoolManager(ConnectionPoolDataSource poolDataSource, int minCons, int maxCons,
            String userName, String password) throws SQLException {
        this(poolDataSource, minCons, maxCons, userName, password, PoolManager.MAX_QUEUE_WAIT_DEFAULT);
    }

    /**
     * Creates new PoolManager with the specified policy for connection pooling and a
     * ConnectionPoolDataSource object.
     * 
     * @param poolDataSource data source for pooled connections
     * @param minCons Non-negative integer that specifies a minimum number of open
     *            connections to keep in the pool at all times
     * @param maxCons Non-negative integer that specifies maximum number of simultaneuosly
     *            open connections
     * @throws SQLException if pool manager can not be created.
     * @since 3.2
     */
    public PoolManager(ConnectionPoolDataSource poolDataSource, int minCons, int maxCons,
            String userName, String password, long maxQueueWaitTime) throws SQLException {
        init(poolDataSource, minCons, maxCons, userName, password, maxQueueWaitTime);
    }

    /** Initializes pool. Normally called from constructor. */
    protected void init(
            ConnectionPoolDataSource poolDataSource,
            int minCons,
            int maxCons,
            String userName,
            String password,
            long maxQueueWaitTime) throws SQLException {

        // do sanity checks...
        if (maxConnections < 0) {
            throw new SQLException("Maximum number of connections can not be negative ("
                    + maxCons
                    + ").");
        }

        if (minConnections < 0) {
            throw new SQLException("Minimum number of connections can not be negative ("
                    + minCons
                    + ").");
        }

        if (minConnections > maxConnections) {
            throw new SQLException(
                    "Minimum number of connections can not be bigger then maximum.");
        }

        // init properties
        this.userName = userName;
        this.password = password;
        this.minConnections = minCons;
        this.maxConnections = maxCons;
        this.poolDataSource = poolDataSource;
        this.maxQueueWaitTime = maxQueueWaitTime;
        
        // init pool... use linked lists to use the queue in the FIFO manner
        usedPool = new LinkedList<PooledConnection>();
        unusedPool = new LinkedList<PooledConnection>();
        growPool(minConnections, userName, password);

        startMaintenanceThread();
    }

    protected synchronized void startMaintenanceThread() {
        disposeOfMaintenanceThread();
        this.poolMaintenanceThread = new PoolMaintenanceThread(this);
        this.poolMaintenanceThread.start();
    }

    /**
     * Creates and returns new PooledConnection object, adding itself as a listener for
     * connection events.
     */
    protected PooledConnection newPooledConnection(String userName, String password)
            throws SQLException {
        PooledConnection connection = (userName != null) ? poolDataSource
                .getPooledConnection(userName, password) : poolDataSource
                .getPooledConnection();
        connection.addConnectionEventListener(this);
        return connection;
    }

    /**
     * Closes all existing connections, drains the pool and stops the maintenance thread.
     * 
     * @since 3.1
     */
    public synchronized void shutdown() throws SQLException {

        // disposing maintenance thread first to avoid any changes to pools
        // during shutdown
        disposeOfMaintenanceThread();

        // using boolean variable instead of locking PoolManager instance due to
        // possible deadlock during shutdown when one of connections locks its
        // event listeners list trying to invoke locked PoolManager's listener methods
        shuttingDown = true;

        ListIterator<PooledConnection> unusedIterator = unusedPool.listIterator();
        while (unusedIterator.hasNext()) {
            PooledConnection con = unusedIterator.next();
            // close connection
            con.close();
            // remove connection from the list
            unusedIterator.remove();
        }

        // clean used connections
        ListIterator<PooledConnection> usedIterator = usedPool.listIterator();
        while (usedIterator.hasNext()) {
            PooledConnection con = usedIterator.next();
            // stop listening for connection events
            con.removeConnectionEventListener(this);
            // close connection
            con.close();
            // remove connection from the list
            usedIterator.remove();
        }
    }

    /**
     * An implementation of {@link ScopeEventListener} that simply calls
     * {@link #shutdown()}.
     * 
     * @since 3.1
     */
    public void beforeScopeEnd() {
        try {
            shutdown();
        }
        catch (SQLException e) {
            throw new CayenneRuntimeException("Error while shutting down");
        }
    }

    protected void disposeOfMaintenanceThread() {
        if (poolMaintenanceThread != null) {
            poolMaintenanceThread.shutdown();
            poolMaintenanceThread = null;
        }
    }

    /**
     * @return true if at least one more connection can be added to the pool.
     */
    protected synchronized boolean canGrowPool() {
        return getPoolSize() < maxConnections;
    }

    /**
     * Increases connection pool by the specified number of connections.
     * 
     * @return the actual number of created connections.
     * @throws SQLException if an error happens when creating a new connection.
     */
    protected synchronized int growPool(
            int addConnections,
            String userName,
            String password) throws SQLException {

        int i = 0;
        int startPoolSize = getPoolSize();
        for (; i < addConnections && startPoolSize + i < maxConnections; i++) {
            PooledConnection newConnection = newPooledConnection(userName, password);
            unusedPool.add(newConnection);
        }

        return i;
    }

    protected synchronized void shrinkPool(int closeConnections) {
        int idleSize = unusedPool.size();
        for (int i = 0; i < closeConnections && i < idleSize; i++) {
            PooledConnection con = unusedPool.remove(i);

            try {
                con.close();
            }
            catch (SQLException ex) {
                // ignore
            }
        }
    }

    /**
     * Returns maximum number of connections this pool can keep. This parameter when
     * configured allows to limit the number of simultaneously open connections.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * Returns the absolute minimum number of connections allowed in this pool at any
     * moment in time.
     */
    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    /**
     * Returns a database URL used to initialize this pool. Will return null if the pool
     * was initialized with ConnectionPoolDataSource.
     */
    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    /**
     * Returns a name of a JDBC driver used to initialize this pool. Will return null if
     * the pool was initialized with ConnectionPoolDataSource.
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    /** Returns a data source password used to initialize this pool. */
    public String getPassword() {
        return password;
    }

    /** Returns a data source user name used to initialize this pool. */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns current number of connections.
     */
    public synchronized int getPoolSize() {
        return usedPool.size() + unusedPool.size();
    }

    /**
     * Returns the number of connections obtained via this DataSource that are currently
     * in use by the DataSource clients.
     */
    public synchronized int getCurrentlyInUse() {
        return usedPool.size();
    }

    /**
     * Returns the number of connections maintained in the pool that are currently not
     * used by any clients and are available immediately via <code>getConnection</code>
     * method.
     */
    public synchronized int getCurrentlyUnused() {
        return unusedPool.size();
    }

    /**
     * Returns connection from the pool using internal values of user name and password.
     * Equivalent to calling:
     * <p>
     * <code>ds.getConnection(ds.getUserName(), ds.getPassword())</code>
     * </p>
     */
    public Connection getConnection() throws SQLException {
        return getConnection(userName, password);
    }

    /** Returns connection from the pool. */
    public synchronized Connection getConnection(String userName, String password)
            throws SQLException {

        if (shuttingDown) {
            throw new SQLException("Pool manager is shutting down.");
        }

        PooledConnection pooledConnection = uncheckPooledConnection(userName, password);

        try {
            return uncheckConnection(pooledConnection);
        }
        catch (SQLException ex) {

            try {
                pooledConnection.close();
            }
            catch (SQLException ignored) {
            }

            // do one reconnect attempt...
            pooledConnection = uncheckPooledConnection(userName, password);
            try {
                return uncheckConnection(pooledConnection);
            }
            catch (SQLException reconnectEx) {
                try {
                    pooledConnection.close();
                }
                catch (SQLException ignored) {
                }

                throw reconnectEx;
            }
        }
    }

    private Connection uncheckConnection(PooledConnection pooledConnection)
            throws SQLException {
        Connection c = pooledConnection.getConnection();

        // only do that on successfully unchecked connection...
        usedPool.add(pooledConnection);
        return c;
    }

    private PooledConnection uncheckPooledConnection(String userName, String password)
            throws SQLException {
        // wait for returned connections or the maintenance thread
        // to bump the pool size...

        if (unusedPool.size() == 0) {

            // first try to open a new connection
            if (canGrowPool()) {
                return newPooledConnection(userName, password);
            }

            // can't open no more... will have to wait for others to return a connection

            // note that if we were woken up
            // before the full wait period expired, and no connections are
            // available yet, go back to sleep. Otherwise we don't give a maintenance
            // thread a chance to increase pool size
            long waitTill = System.currentTimeMillis() + maxQueueWaitTime;
        	
            do {
                try {
                    wait(maxQueueWaitTime);
                }
                catch (InterruptedException iex) {
                    // ignoring
                }

            } while (unusedPool.size() == 0 && (maxQueueWaitTime == 0 || waitTill > System.currentTimeMillis()));

            if (unusedPool.size() == 0) {
                throw new ConnectionUnavailableException(
                        "Can't obtain connection. Request timed out. Total used connections: "
                                + usedPool.size());
            }
        }

        // get first connection... lets cycle them in FIFO manner
        return unusedPool.remove(0);
    }

    public int getLoginTimeout() throws java.sql.SQLException {
        return poolDataSource.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws java.sql.SQLException {
        poolDataSource.setLoginTimeout(seconds);
    }

    public PrintWriter getLogWriter() throws java.sql.SQLException {
        return poolDataSource.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws java.sql.SQLException {
        poolDataSource.setLogWriter(out);
    }

    /**
     * Returns closed connection to the pool.
     */
    public synchronized void connectionClosed(ConnectionEvent event) {

        if (shuttingDown) {
            return;
        }

        // return connection to the pool
        PooledConnection closedConn = (PooledConnection) event.getSource();

        // remove this connection from the list of connections
        // managed by this pool...
        int usedInd = usedPool.indexOf(closedConn);
        if (usedInd >= 0) {
            usedPool.remove(usedInd);
            unusedPool.add(closedConn);

            // notify threads waiting for connections
            notifyAll();
        }
        // else ....
        // other possibility is that this is a bad connection, so just ignore its closing
        // event,
        // since it was unregistered in "connectionErrorOccurred"
    }

    /**
     * Removes connection with an error from the pool. This method is called by
     * PoolManager connections on connection errors to notify PoolManager that connection
     * is in invalid state.
     */
    public synchronized void connectionErrorOccurred(ConnectionEvent event) {

        if (shuttingDown) {
            return;
        }

        // later on we should analyze the error to see if this
        // is fatal... right now just kill this PooledConnection

        PooledConnection errorSrc = (PooledConnection) event.getSource();

        // remove this connection from the list of connections
        // managed by this pool...

        int usedInd = usedPool.indexOf(errorSrc);
        if (usedInd >= 0) {
            usedPool.remove(usedInd);
        }
        else {
            int unusedInd = unusedPool.indexOf(errorSrc);
            if (unusedInd >= 0)
                unusedPool.remove(unusedInd);
        }

        // do not close connection,
        // let the code that catches the exception handle it
        // ....
    }

    static class PoolMaintenanceThread extends Thread {

        private boolean shouldDie;
        private PoolManager pool;

        PoolMaintenanceThread(PoolManager pool) {
            super.setName("PoolManagerCleanup-" + pool.hashCode());
            super.setDaemon(true);
            this.pool = pool;
        }

        @Override
        public void run() {
            // periodically wakes up to check if the pool should grow or shrink
            while (true) {

                try {
                    // don't do it too often
                    sleep(600000);
                }
                catch (InterruptedException iex) {
                    // ignore...
                }

                synchronized (pool) {
                    // TODO: implement a smarter algorithm for pool management...
                    // right now it will simply close one connection if the count is
                    // above median and there are any idle connections.

                    if (shouldDie) {
                        break;
                    }

                    int unused = pool.getCurrentlyUnused();
                    int used = pool.getCurrentlyInUse();
                    int total = unused + used;
                    int median = pool.minConnections
                            + 1
                            + (pool.maxConnections - pool.minConnections)
                            / 2;

                    if (unused > 0 && total > median) {
                        pool.shrinkPool(1);
                    }
                }
            }
        }

        /**
         * Stops the maintenance thread.
         */
        void shutdown() {
            shouldDie = true;
            interrupt();
        }
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.1 JDBC 4.1 compatibility under Java 1.5
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new UnsupportedOperationException();
    }
}
