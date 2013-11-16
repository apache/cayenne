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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.DataSource;
import javax.sql.PooledConnection;
import javax.sql.StatementEventListener;

/**
 * PooledConnectionImpl is an implementation of a pooling wrapper for the database
 * connection as per JDBC3 spec. Most of the modern JDBC drivers should have its own
 * implementation that may be used instead of this class.
 * 
 */
public class PooledConnectionImpl implements PooledConnection {

    private Connection connectionObj;
    private List<ConnectionEventListener> connectionEventListeners;
    private boolean hadErrors;
    private DataSource connectionSource;
    private String userName;
    private String password;

    protected PooledConnectionImpl() {
        // TODO: maybe remove synchronization and use
        // FastArrayList from commons-collections? After
        // all the only listener is usually pool manager.
        this.connectionEventListeners = Collections
                .synchronizedList(new ArrayList<ConnectionEventListener>(10));
    }

    /** Creates new PooledConnection */
    public PooledConnectionImpl(DataSource connectionSource, String userName,
            String password) {

        this();

        this.connectionSource = connectionSource;
        this.userName = userName;
        this.password = password;

    }

    public void reconnect() throws SQLException {
        if (connectionObj != null) {
            try {
                connectionObj.close();
            }
            catch (SQLException ex) {
                // ignore exception, since connection is expected
                // to be in a bad state
            }
            finally {
                connectionObj = null;
            }
        }

        connectionObj = (userName != null) ? connectionSource.getConnection(
                userName,
                password) : connectionSource.getConnection();
    }

    public void addConnectionEventListener(ConnectionEventListener listener) {
        synchronized (connectionEventListeners) {
            if (!connectionEventListeners.contains(listener))
                connectionEventListeners.add(listener);
        }
    }

    public void removeConnectionEventListener(ConnectionEventListener listener) {
        synchronized (connectionEventListeners) {
            connectionEventListeners.remove(listener);
        }
    }

    public void close() throws SQLException {

        synchronized (connectionEventListeners) {
            // remove all listeners
            connectionEventListeners.clear();
        }

        if (connectionObj != null) {
            try {
                connectionObj.close();
            }
            finally {
                connectionObj = null;
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (connectionObj == null) {
            reconnect();
        }

        // set autocommit to false to return connection
        // always in consistent state
        if (!connectionObj.getAutoCommit()) {

            try {
                connectionObj.setAutoCommit(true);
            }
            catch (SQLException sqlEx) {
                // try applying Sybase patch
                ConnectionWrapper.sybaseAutoCommitPatch(connectionObj, sqlEx, true);
            }
        }

        connectionObj.clearWarnings();
        return new ConnectionWrapper(connectionObj, this);
    }

    protected void returnConnectionToThePool() throws SQLException {
        // do not return to pool bad connections
        if (hadErrors)
            close();
        else
            // notify the listeners that connection is no longer used by application...
            this.connectionClosedNotification();
    }

    /**
     * This method creates and sents an event to listeners when an error occurs in the
     * underlying connection. Listeners can have special logic to analyze the error and do
     * things like closing this PooledConnection (if the error is fatal), etc...
     */
    public void connectionErrorNotification(SQLException exception) {
        // hint for later to avoid returning bad connections to the pool
        hadErrors = true;

        synchronized (connectionEventListeners) {
            if (connectionEventListeners.size() == 0)
                return;

            ConnectionEvent closedEvent = new ConnectionEvent(this, exception);
            for (final ConnectionEventListener nextListener : connectionEventListeners) {
                nextListener.connectionErrorOccurred(closedEvent);
            }
        }
    }

    /**
     * Creates and sends an event to listeners when a user closes java.sql.Connection
     * object belonging to this PooledConnection.
     */
    protected void connectionClosedNotification() {
        synchronized (connectionEventListeners) {
            if (connectionEventListeners.size() == 0)
                return;

            ConnectionEvent closedEvent = new ConnectionEvent(this);

            for (final ConnectionEventListener nextListener : connectionEventListeners) {
                nextListener.connectionClosed(closedEvent);
            }
        }
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public void addStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public void removeStatementEventListener(StatementEventListener listener) {
        throw new UnsupportedOperationException();
    }
}
