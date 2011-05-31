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

package org.apache.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.NoopJdbcEventLogger;

/**
 * A Cayenne transaction. Currently supports managing JDBC connections.
 * 
 * @since 1.1
 */
public abstract class Transaction {

    /**
     * A ThreadLocal that stores current thread transaction.
     * 
     * @since 1.2
     */
    static final ThreadLocal<Transaction> currentTransaction = new InheritableThreadLocal<Transaction>();

    private static final Transaction NO_TRANSACTION = new Transaction() {

        @Override
        public void begin() {

        }

        @Override
        public void commit() {

        }

        @Override
        public void rollback() {

        }
    };

    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_COMMITTING = 2;
    public static final int STATUS_COMMITTED = 3;
    public static final int STATUS_ROLLEDBACK = 4;
    public static final int STATUS_ROLLING_BACK = 5;
    public static final int STATUS_NO_TRANSACTION = 6;
    public static final int STATUS_MARKED_ROLLEDBACK = 7;

    protected Map<String, Connection> connections;
    protected int status;
    protected TransactionDelegate delegate;
    
    protected JdbcEventLogger jdbcEventLogger;

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
     * 
     * @since 1.2
     */
    public static void bindThreadTransaction(Transaction transaction) {
        currentTransaction.set(transaction);
    }

    /**
     * Returns a Transaction associated with the current thread, or null if there is no
     * such Transaction.
     * 
     * @since 1.2
     */
    public static Transaction getThreadTransaction() {
        return currentTransaction.get();
    }

    /**
     * Factory method returning a new transaction instance that would propagate
     * commit/rollback to participating connections. Connections will be closed when
     * commit or rollback is called.
     */
    public static Transaction internalTransaction(TransactionDelegate delegate) {
        return new InternalTransaction(delegate);
    }

    /**
     * Factory method returning a new transaction instance that would NOT propagate
     * commit/rollback to participating connections. Connections will still be closed when
     * commit or rollback is called.
     */
    public static Transaction externalTransaction(TransactionDelegate delegate) {
        return new ExternalTransaction(delegate);
    }

    /**
     * Factory method returning a transaction instance that does not alter the state of
     * participating connections in any way. Commit and rollback methods do not do
     * anything.
     */
    public static Transaction noTransaction() {
        return NO_TRANSACTION;
    }

    /**
     * Creates new inactive transaction.
     */
    protected Transaction() {
        status = STATUS_NO_TRANSACTION;
        jdbcEventLogger = NoopJdbcEventLogger.getInstance();
    }

    public TransactionDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(TransactionDelegate delegate) {
        this.delegate = delegate;
    }

    public int getStatus() {
        return status;
    }
    
    /**
     * @since 3.1
     */
    public void setJdbcEventLogger(JdbcEventLogger jdbcEventLogger) {
        this.jdbcEventLogger = jdbcEventLogger;
    }
    
    /**
     * @since 3.1
     */
    public JdbcEventLogger getJdbcEventLogger() {
        return this.jdbcEventLogger;
    }

    public synchronized void setRollbackOnly() {
        setStatus(STATUS_MARKED_ROLLEDBACK);
    }

    public synchronized void setStatus(int status) {
        if (delegate != null
                && status == STATUS_MARKED_ROLLEDBACK
                && !delegate.willMarkAsRollbackOnly(this)) {
            return;
        }

        this.status = status;
    }

    /**
     * Starts a Transaction. If Transaction is not started explicitly, it will be started
     * when the first connection is added.
     */
    public abstract void begin();

    public abstract void commit() throws IllegalStateException, SQLException,
            CayenneException;

    public abstract void rollback() throws IllegalStateException, SQLException,
            CayenneException;

    /**
     * @since 1.2
     */
    public Connection getConnection(String name) {
        return (connections != null) ? connections.get(name) : null;
    }

    /**
     * @since 1.2
     */
    public boolean addConnection(String name, Connection connection) throws SQLException {
        if (delegate != null && !delegate.willAddConnection(this, connection)) {
            return false;
        }

        if (connections == null) {
            connections = new HashMap<String, Connection>();
        }

        return connections.put(name, connection) != connection;
    }
}
