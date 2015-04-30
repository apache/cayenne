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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
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

/**
 * A {@link DataSource} with a pool of connections, that can automatically grow
 * to the max size as more connections are requested.
 * 
 * @since 4.0
 */
public class PoolingDataSource implements ScopeEventListener, DataSource, ConnectionEventListener {

	/**
	 * Defines a maximum time in milliseconds that a connection request could
	 * wait in the connection queue. After this period expires, an exception
	 * will be thrown in the calling method.
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

	protected ConnectionPoolDataSource pooledConnectionFactory;

	private int minConnections;
	private int maxConnections;
	private long maxQueueWaitTime;
	private String validationQuery;

	private List<PooledConnection> unusedPool;
	private List<PooledConnection> usedPool;
	private boolean shuttingDown;

	/**
	 * Creates new PoolManager with the specified policy for connection pooling
	 * and a ConnectionPoolDataSource object.
	 * 
	 * @param poolDataSource
	 *            data source for pooled connections
	 * @param minCons
	 *            Non-negative integer that specifies a minimum number of open
	 *            connections to keep in the pool at all times
	 * @param maxCons
	 *            Non-negative integer that specifies maximum number of
	 *            simultaneously open connections
	 * @throws SQLException
	 *             if pool manager can not be created.
	 * @since 4.0
	 */
	public PoolingDataSource(ConnectionPoolDataSource poolDataSource, PoolingDataSourceParameters parameters)
			throws SQLException {

		this.pooledConnectionFactory = poolDataSource;

		// clone parameters to keep DataSource immutable
		this.minConnections = parameters.getMinConnections();
		this.maxConnections = parameters.getMaxConnections();
		this.maxQueueWaitTime = parameters.getMaxQueueWaitTime();
		this.validationQuery = parameters.getValidationQuery();

		// init pool... use linked lists to use the queue in the FIFO manner
		this.usedPool = new LinkedList<PooledConnection>();
		this.unusedPool = new LinkedList<PooledConnection>();
		growPool(minConnections);
	}

	/**
	 * Creates and returns new PooledConnection object, adding itself as a
	 * listener for connection events.
	 */
	protected PooledConnection newPooledConnection() throws SQLException {
		PooledConnection connection = pooledConnectionFactory.getPooledConnection();
		connection.addConnectionEventListener(this);
		return connection;
	}

	/**
	 * Shuts down the pool, closing all open connections. This is an
	 * implementation of {@link ScopeEventListener}.
	 * 
	 * @since 3.1
	 */
	@Override
	public synchronized void beforeScopeEnd() {
		try {

			// using boolean variable instead of locking PoolManager instance
			// due to possible deadlock during shutdown when one of connections
			// locks its event listeners list trying to invoke locked
			// PoolManager's listener methods
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
		} catch (SQLException e) {
			throw new CayenneRuntimeException("Error while shutting down");
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
	 * @throws SQLException
	 *             if an error happens when creating a new connection.
	 */
	protected synchronized int growPool(int addConnections) throws SQLException {

		int i = 0;
		int startPoolSize = getPoolSize();
		for (; i < addConnections && startPoolSize + i < maxConnections; i++) {
			PooledConnection newConnection = newPooledConnection();
			unusedPool.add(newConnection);
		}

		return i;
	}

	public synchronized void shrinkPool(int closeConnections) {
		int idleSize = unusedPool.size();
		for (int i = 0; i < closeConnections && i < idleSize; i++) {
			PooledConnection con = unusedPool.remove(i);

			try {
				con.close();
			} catch (SQLException ex) {
				// ignore
			}
		}
	}

	public String getValidationQuery() {
		return validationQuery;
	}

	/**
	 * Returns maximum number of connections this pool can keep. This parameter
	 * when configured allows to limit the number of simultaneously open
	 * connections.
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Returns the absolute minimum number of connections allowed in this pool
	 * at any moment in time.
	 */
	public int getMinConnections() {
		return minConnections;
	}

	/**
	 * Returns current number of connections.
	 */
	public synchronized int getPoolSize() {
		return usedPool.size() + unusedPool.size();
	}

	/**
	 * Returns the number of connections obtained via this DataSource that are
	 * currently in use by the DataSource clients.
	 */
	public synchronized int getCurrentlyInUse() {
		return usedPool.size();
	}

	/**
	 * Returns the number of connections maintained in the pool that are
	 * currently not used by any clients and are available immediately via
	 * <code>getConnection</code> method.
	 */
	public synchronized int getCurrentlyUnused() {
		return unusedPool.size();
	}

	/**
	 * Returns connection from the pool using internal values of user name and
	 * password.
	 */
	@Override
	public synchronized Connection getConnection() throws SQLException {
		if (shuttingDown) {
			throw new SQLException("Pool manager is shutting down.");
		}

		PooledConnection pooledConnection = uncheckPooledConnection();

		try {
			return uncheckAndValidateConnection(pooledConnection);
		} catch (SQLException ex) {

			try {
				pooledConnection.close();
			} catch (SQLException ignored) {
			}

			// do one reconnect attempt...
			pooledConnection = uncheckPooledConnection();
			try {
				return uncheckAndValidateConnection(pooledConnection);
			} catch (SQLException reconnectEx) {
				try {
					pooledConnection.close();
				} catch (SQLException ignored) {
				}

				throw reconnectEx;
			}
		}
	}

	/**
	 * Returns connection from the pool.
	 */
	@Override
	public synchronized Connection getConnection(String userName, String password) throws SQLException {
		throw new UnsupportedOperationException(
				"Connections for a specific user are not supported by the pooled DataSource");
	}

	private Connection uncheckConnection(PooledConnection pooledConnection) throws SQLException {
		Connection c = pooledConnection.getConnection();

		// only do that on successfully unchecked connection...
		usedPool.add(pooledConnection);
		return c;
	}

	private Connection uncheckAndValidateConnection(PooledConnection pooledConnection) throws SQLException {
		Connection c = uncheckConnection(pooledConnection);

		if (validationQuery != null) {

			Statement statement = c.createStatement();
			try {
				ResultSet rs = statement.executeQuery(validationQuery);
				try {

					if (!rs.next()) {
						throw new SQLException("Connection validation failed, no result for query: " + validationQuery);
					}

				} finally {
					rs.close();
				}
			} finally {
				statement.close();
			}
		}

		return c;
	}

	private PooledConnection uncheckPooledConnection() throws SQLException {
		// wait for returned connections or the maintenance thread
		// to bump the pool size...

		if (unusedPool.size() == 0) {

			// first try to open a new connection
			if (canGrowPool()) {
				return newPooledConnection();
			}

			// can't open no more... will have to wait for others to return a
			// connection

			// note that if we were woken up
			// before the full wait period expired, and no connections are
			// available yet, go back to sleep. Otherwise we don't give a
			// maintenance
			// thread a chance to increase pool size
			long waitTill = System.currentTimeMillis() + maxQueueWaitTime;

			do {
				try {
					wait(maxQueueWaitTime);
				} catch (InterruptedException iex) {
					// ignoring
				}

			} while (unusedPool.size() == 0 && (maxQueueWaitTime == 0 || waitTill > System.currentTimeMillis()));

			if (unusedPool.size() == 0) {
				throw new ConnectionUnavailableException(
						"Can't obtain connection. Request timed out. Total used connections: " + usedPool.size());
			}
		}

		// get first connection... lets cycle them in FIFO manner
		return unusedPool.remove(0);
	}

	@Override
	public int getLoginTimeout() throws java.sql.SQLException {
		return pooledConnectionFactory.getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws java.sql.SQLException {
		pooledConnectionFactory.setLoginTimeout(seconds);
	}

	@Override
	public PrintWriter getLogWriter() throws java.sql.SQLException {
		return pooledConnectionFactory.getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws java.sql.SQLException {
		pooledConnectionFactory.setLogWriter(out);
	}

	/**
	 * Returns closed connection to the pool.
	 */
	@Override
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
		// other possibility is that this is a bad connection, so just ignore
		// its closing
		// event,
		// since it was unregistered in "connectionErrorOccurred"
	}

	/**
	 * Removes connection with an error from the pool. This method is called by
	 * PoolManager connections on connection errors to notify PoolManager that
	 * connection is in invalid state.
	 */
	@Override
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
		} else {
			int unusedInd = unusedPool.indexOf(errorSrc);
			if (unusedInd >= 0) {
				unusedPool.remove(unusedInd);
			}
		}

		// do not close connection,
		// let the code that catches the exception handle it
		// ....
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return PoolingDataSource.class.equals(iface);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (PoolingDataSource.class.equals(iface)) {
			return (T) this;
		}

		throw new SQLException("Not a wrapper for " + iface);
	}

	// JDBC 4.1 compatibility under Java <= 1.6
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}

}
