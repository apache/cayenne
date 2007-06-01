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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.apache.log4j.Logger;

/**
 * PoolManager is a pooling DataSource impementation. 
 * Internally to obtain connections PoolManager uses either a JDBC driver 
 * or another pooling datasource.
 *
 * <p>TODO: create a low priority thread that will do pool maintenance.</p>
 *
 * @author Andrei Adamchik
 */
public class PoolManager implements DataSource, ConnectionEventListener {
    private static Logger logObj = Logger.getLogger(PoolManager.class);
    
    /** 
     * Defines a maximum time in milliseconds that a connection
     * request could wait in the connection queue. After this period
     * expires, an exception will be thrown in the calling method.
     * In the future this parameter should be made configurable.
     */
    public static final int MAX_QUEUE_WAIT = 20000;

    protected ConnectionPoolDataSource poolDataSource;
    protected int minConnections;
    protected int maxConnections;
    protected String dataSourceUrl;
    protected String jdbcDriver;
    protected String password;
    protected String userName;

    protected List unusedPool;
    protected List usedPool;

    private PoolMaintenanceThread poolMaintenanceThread;


    /** 
     * Creates new PoolManager using org.objectstyle.cayenne.conn.PoolDataSource
     * for an underlying ConnectionPoolDataSource. 
     */
    public PoolManager(
        String jdbcDriver,
        String dataSourceUrl,
        int minCons,
        int maxCons,
        String userName,
        String password)
        throws SQLException {

        this(jdbcDriver, dataSourceUrl, minCons, maxCons, userName, password, null);
    }

    public PoolManager(
        String jdbcDriver,
        String dataSourceUrl,
        int minCons,
        int maxCons,
        String userName,
        String password,
        ConnectionEventLoggingDelegate logger)
        throws SQLException {

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
        init(poolDS, minCons, maxCons, userName, password);
    }

    /** Creates new PoolManager with the specified policy for
     *  connection pooling and a ConnectionPoolDataSource object.
     *
     *  @param poolDataSource data source for pooled connections
     *  @param minCons Non-negative integer that specifies a minimum number of open connections
     *  to keep in the pool at all times
     *  @param maxCons Non-negative integer that specifies maximum number of simultaneuosly open connections
     *
     *  @throws SQLException if pool manager can not be created.
     */
    public PoolManager(
        ConnectionPoolDataSource poolDataSource,
        int minCons,
        int maxCons,
        String userName,
        String password)
        throws SQLException {
        init(poolDataSource, minCons, maxCons, userName, password);
    }

    /** Initializes pool. Normally called from constructor. */
    protected void init(
        ConnectionPoolDataSource poolDataSource,
        int minCons,
        int maxCons,
        String userName,
        String password)
        throws SQLException {

        // do sanity checks...
        if (maxConnections < 0) {
            throw new SQLException(
                "Maximum number of connections can not be negative (" + maxCons + ").");
        }

        if (minConnections < 0) {
            throw new SQLException(
                "Minimum number of connections can not be negative (" + minCons + ").");
        }

        if (minConnections > maxConnections) {
            throw new SQLException("Minimum number of connections can not be bigger then maximum.");
        }

        // init properties
        this.userName = userName;
        this.password = password;
        this.minConnections = minCons;
        this.maxConnections = maxCons;
        this.poolDataSource = poolDataSource;

        // init pool... use linked lists to use the queue in the FIFO manner
        usedPool = new LinkedList();
        unusedPool = new LinkedList();
        growPool(minConnections, userName, password);

        startMaintenanceThread();
    }
    
    protected synchronized void startMaintenanceThread() {
        disposeOfMaintenanceThread();
        this.poolMaintenanceThread = new PoolMaintenanceThread(this);
        this.poolMaintenanceThread.start();
    }

    /** 
     * Creates and returns new PooledConnection object, adding itself as a listener 
     * for connection events. 
     */
    protected PooledConnection newPooledConnection(String userName, String password)
        throws SQLException {
        PooledConnection connection =
            (userName != null)
                ? poolDataSource.getPooledConnection(userName, password)
                : poolDataSource.getPooledConnection();
        connection.addConnectionEventListener(this);
        return connection;
    }

    /** Closes all existing connections, removes them from the pool. */
    public void dispose() throws SQLException {
        synchronized (this) {
            // clean connections from the pool
            ListIterator unusedIterator = unusedPool.listIterator();
            while (unusedIterator.hasNext()) {
                PooledConnection con = (PooledConnection) unusedIterator.next();
                // close connection
                con.close();
                // remove connection from the list
                unusedIterator.remove();
            }

            // clean used connections
            ListIterator usedIterator = usedPool.listIterator();
            while (usedIterator.hasNext()) {
                PooledConnection con = (PooledConnection) usedIterator.next();
                // stop listening for connection events
                con.removeConnectionEventListener(this);
                // close connection
                con.close();
                // remove connection from the list
                usedIterator.remove();
            }
        }

        disposeOfMaintenanceThread();
    }
    
    protected void disposeOfMaintenanceThread() {
        if (poolMaintenanceThread != null) {
            this.poolMaintenanceThread.dispose();
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
        String password)
        throws SQLException {

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
            PooledConnection con = (PooledConnection) unusedPool.remove(i);

            try {
                con.close();
            } catch (SQLException ex) {
                logObj.info("Error closing connection. Ignoring.", ex);
            }
        }
    }

    /** 
     * Returns maximum number of connections this pool can keep.
     * This parameter when configured allows to limit the number of simultaneously
     * open connections.
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /** Returns the absolute minimum number of connections allowed 
      * in this pool at any moment in time. */
    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    /** Returns a database URL used to initialize this pool.
      * Will return null if the pool was initialized with ConnectionPoolDataSource. */
    public String getDataSourceUrl() {
        return dataSourceUrl;
    }

    /** Returns a name of a JDBC driver used to initialize this pool.
      * Will return null if the pool was initialized with ConnectionPoolDataSource. */
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
     * Returns the number of connections obtained via this DataSource
     * that are currently in use by the DataSource clients. 
     */
    public synchronized int getCurrentlyInUse() {
        return usedPool.size();
    }

    /** 
     * Returns the number of connections maintained in the 
     * pool that are currently not used by any clients and are
     * available immediately via <code>getConnection</code> method. 
     */
    public synchronized int getCurrentlyUnused() {
        return unusedPool.size();
    }

    /** 
     * Returns connection from the pool using internal values of user name
     * and password. Eqivalent to calling: 
     * 
     * <p><code>ds.getConnection(ds.getUserName(), ds.getPassword())</code></p> 
     */
    public Connection getConnection() throws SQLException {
        return getConnection(userName, password);
    }

    /** Returns connection from the pool. */
    public synchronized Connection getConnection(String userName, String password)
            throws SQLException {

        PooledConnection pooledConnection = uncheckPooledConnection(userName, password);

        try {
            return uncheckConnection(pooledConnection);
        }
        catch (SQLException ex) {
            logObj.info("Error getting connection", ex);

            try {
                pooledConnection.close();
            }
            catch (SQLException ignored) {
            }

            logObj.info("Reconnecting...");
            
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
            long waitTill =
                System.currentTimeMillis()
                + MAX_QUEUE_WAIT;

            do {
                try {
                    wait(MAX_QUEUE_WAIT);
                } catch (InterruptedException iex) {
                    // ignoring
                }

            } while (unusedPool.size() == 0 && waitTill > System.currentTimeMillis());

            if (unusedPool.size() == 0) {
                throw new SQLException(
                    "Can't obtain connection. Request timed out. Total used connections: "
                        + usedPool.size());
            }
        }

        // get first connection... lets cycle them in FIFO manner
        return (PooledConnection) unusedPool.remove(0);
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
        // other possibility is that this is a bad connection, so just ignore its closing event,
        // since it was unregistered in "connectionErrorOccurred"
    }

    /** 
     * Removes connection with an error from the pool. This method
     * is called by PoolManager connections on connection errors
     * to notify PoolManager that connection is in invalid state.
     */
    public synchronized void connectionErrorOccurred(ConnectionEvent event) {
        // later on we should analyze the error to see if this
        // is fatal... right now just kill this PooledConnection

        PooledConnection errorSrc = (PooledConnection) event.getSource();

        // remove this connection from the list of connections
        // managed by this pool...

        int usedInd = usedPool.indexOf(errorSrc);
        if (usedInd >= 0) {
            usedPool.remove(usedInd);
        } else {
            int unusedInd = unusedPool.indexOf(errorSrc);
            if (unusedInd >= 0)
                unusedPool.remove(unusedInd);
        }

        // do not close connection,
        // let the code that catches the exception handle it
        // ....
    }

    static class PoolMaintenanceThread extends Thread {
        protected boolean shouldDie;
        protected PoolManager pool;

        PoolMaintenanceThread(PoolManager pool) {
            super.setName("PoolManagerCleanup-" + pool.hashCode());
            super.setDaemon(true);
            this.pool = pool;
        }

        public void run() {
            // periodically wakes up to check if the pool should grow or shrink 
            while (true) {

                try {
                    // don't do it too often
                    sleep(600000);
                } catch (InterruptedException iex) {
                    // ignore...
                }

                if (shouldDie) {
                    break;
                }

                synchronized (pool) {
                    // TODO: implement a smarter algorithm for pool management... 
                    // right now it will simply close one connection if the count is
                    // above median and there are any idle connections.

                    int unused = pool.getCurrentlyUnused();
                    int used = pool.getCurrentlyInUse();
                    int total = unused + used;
                    int median =
                        pool.minConnections
                            + 1
                            + (pool.maxConnections - pool.minConnections) / 2;

                    if (unused > 0 && total > median) {
                        pool.shrinkPool(1);
                        logObj.debug("decreased pool size to " + (total - 1) + " connections.");
                    }
                }
            }
        }

        /**
         * Stops the thread.
         */
        public void dispose() {
            shouldDie = true;
        }
    }
}