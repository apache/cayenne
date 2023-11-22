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

package org.apache.cayenne.tx;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A Cayenne transaction. Currently supports managing JDBC connections.
 *
 * @since 4.0
 */
public abstract class BaseTransaction implements Transaction {

    /**
     * A ThreadLocal that stores current thread transaction.
     */
    static final ThreadLocal<Transaction> CURRENT_TRANSACTION = new InheritableThreadLocal<>();

    protected static final int STATUS_ACTIVE = 1;
    protected static final int STATUS_COMMITTING = 2;
    protected static final int STATUS_COMMITTED = 3;
    protected static final int STATUS_ROLLEDBACK = 4;
    protected static final int STATUS_ROLLING_BACK = 5;
    protected static final int STATUS_NO_TRANSACTION = 6;
    protected static final int STATUS_MARKED_ROLLEDBACK = 7;

    protected Map<String, Connection> connections;
    protected Collection<TransactionListener> listeners;
    protected int status;
    protected int defaultIsolationLevel = -1;
    protected TransactionDescriptor descriptor;

    static String decodeStatus(int status) {
        switch (status) {
            case STATUS_ACTIVE:
                return "STATUS_ACTIVE";
            case STATUS_COMMITTING:
                return "STATUS_COMMITTING";
            case STATUS_COMMITTED:
                return "STATUS_COMMITTED";
            case STATUS_ROLLEDBACK:
                return "STATUS_ROLLEDBACK";
            case STATUS_ROLLING_BACK:
                return "STATUS_ROLLING_BACK";
            case STATUS_NO_TRANSACTION:
                return "STATUS_NO_TRANSACTION";
            case STATUS_MARKED_ROLLEDBACK:
                return "STATUS_MARKED_ROLLEDBACK";
            default:
                return "Unknown Status - " + status;
        }
    }

    /**
     * Binds a Transaction to the current thread.
     */
    public static void bindThreadTransaction(Transaction transaction) {
        CURRENT_TRANSACTION.set(transaction);
    }

    /**
     * Returns a Transaction associated with the current thread, or null if
     * there is no such Transaction.
     */
    public static Transaction getThreadTransaction() {
        return CURRENT_TRANSACTION.get();
    }

    /**
     * Creates new inactive transaction.
     */
    protected BaseTransaction(TransactionDescriptor descriptor) {
        this.status = STATUS_NO_TRANSACTION;
        this.descriptor = descriptor;
    }

    @Override
    public void setRollbackOnly() {
        this.status = STATUS_MARKED_ROLLEDBACK;
    }

    @Override
    public boolean isRollbackOnly() {
        return status == STATUS_MARKED_ROLLEDBACK;
    }

    @Override
    public void addListener(TransactionListener listener) {
        if (listeners == null) {
            listeners = new LinkedHashSet<>();
        }

        listeners.add(listener);
    }

    /**
     * Starts a Transaction. If Transaction is not started explicitly, it will
     * be started when the first connection is added.
     */
    @Override
    public void begin() {
        if (status != BaseTransaction.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("Transaction must have 'STATUS_NO_TRANSACTION' to begin. "
                    + "Current status: " + BaseTransaction.decodeStatus(status));
        }

        status = BaseTransaction.STATUS_ACTIVE;
    }

    @Override
    public void commit() {

        if (status == BaseTransaction.STATUS_NO_TRANSACTION) {
            return;
        }

        if (status != BaseTransaction.STATUS_ACTIVE) {
            throw new IllegalStateException("Transaction must have 'STATUS_ACTIVE' to be committed. "
                    + "Current status: " + BaseTransaction.decodeStatus(status));
        }

        if (listeners != null) {
            for (TransactionListener listener : listeners) {
                listener.willCommit(this);
            }
        }

        processCommit();

        status = BaseTransaction.STATUS_COMMITTED;

        close();
    }

    protected abstract void processCommit();

    @Override
    public void rollback() {

        try {

            if (status == BaseTransaction.STATUS_NO_TRANSACTION || status == BaseTransaction.STATUS_ROLLEDBACK
                    || status == BaseTransaction.STATUS_ROLLING_BACK) {
                return;
            }

            if (status != BaseTransaction.STATUS_ACTIVE && status != BaseTransaction.STATUS_MARKED_ROLLEDBACK) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' or 'STATUS_MARKED_ROLLEDBACK' to be rolled back. "
                                + "Current status: " + BaseTransaction.decodeStatus(status));
            }

            if (listeners != null) {
                for (TransactionListener listener : listeners) {
                    listener.willRollback(this);
                }
            }

            processRollback();

            status = BaseTransaction.STATUS_ROLLEDBACK;

        } finally {
            close();
        }
    }

    protected abstract void processRollback();

    @Override
    public Map<String, Connection> getConnections() {
        return connections != null ? Collections.unmodifiableMap(connections) : Collections.<String, Connection>emptyMap();
    }

    @Override
    public Connection getOrCreateConnection(String connectionName, DataSource dataSource) throws SQLException {

        Connection c = getExistingConnection(connectionName);

        if (c == null || c.isClosed()) {
            if(descriptor.getConnectionSupplier() != null) {
                c = descriptor.getConnectionSupplier().get();
            } else {
                c = dataSource.getConnection();
            }
            addConnection(connectionName, c);
        }

        // wrap transaction-attached connections in a decorator that prevents them from being closed by callers, as
        // transaction should take care of them on commit or rollback.
        return new TransactionConnectionDecorator(c);
    }

    protected Connection getExistingConnection(String name) {
        return (connections != null) ? connections.get(name) : null;
    }

    protected Connection addConnection(String connectionName, Connection connection) {

        setIsolationLevelFrom(connection);

        TransactionConnectionDecorator wrapper = null;

        if (listeners != null) {
            for (TransactionListener listener : listeners) {
                connection = listener.decorateConnection(this, connection);
            }

            wrapper = new TransactionConnectionDecorator(connection);

            for (TransactionListener listener : listeners) {
                listener.willAddConnection(this, connectionName, wrapper);
            }
        }

        if (connections == null) {
            // transaction is single-threaded, so using a non-concurrent map...
            connections = new HashMap<>();
        }

        if (wrapper == null) {
            wrapper = new TransactionConnectionDecorator(connection);
        }

        if (connections.put(connectionName, wrapper) != wrapper) {
            connectionAdded(connection);
        }

        return wrapper;
    }

    private void setIsolationLevelFrom(Connection connection) {
        if (descriptor.getIsolation() != TransactionDescriptor.ISOLATION_DEFAULT) {
            try {
                defaultIsolationLevel = connection.getTransactionIsolation();
                connection.setTransactionIsolation(descriptor.getIsolation());
            } catch (SQLException ex) {
                throw new CayenneRuntimeException("Unable to set required isolation level: " + descriptor.getIsolation(), ex);
            }
        }
    }

    protected void connectionAdded(Connection connection) {

        // implicitly begin transaction
        if (status == BaseTransaction.STATUS_NO_TRANSACTION) {
            begin();
        }

        if (status != BaseTransaction.STATUS_ACTIVE) {
            throw new IllegalStateException("Transaction must have 'STATUS_ACTIVE' to add a connection. "
                    + "Current status: " + BaseTransaction.decodeStatus(status));
        }
    }

    /**
     * Closes all connections associated with transaction.
     */
    protected void close() {
        if (connections == null || connections.isEmpty()) {
            return;
        }

        for (Connection c : connections.values()) {
            try {
                // make sure we unwrap TX connection before closing it, as the TX wrapper's "close" does nothing.
                c.unwrap(Connection.class).close();
            } catch (Throwable th) {
                // TODO: chain exceptions...
                // ignore for now
            } finally {
                // restore connection default isolation level ...
                if (defaultIsolationLevel != -1) {
                    try {
                        c.setTransactionIsolation(defaultIsolationLevel);
                    } catch (SQLException ignore) {
                        // have no meaningful options here...
                    }
                }
            }
        }
    }
}
