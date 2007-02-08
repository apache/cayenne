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
package org.objectstyle.cayenne.access;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * A Cayenne transaction. Currently supports managing JDBC connections.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
public abstract class Transaction {

    /**
     * A ThreadLocal that stores current thread transaction.
     * 
     * @since 1.2
     */
    static final ThreadLocal currentTransaction = new InheritableThreadLocal();

    private static final Transaction NO_TRANSACTION = new Transaction() {

        public void begin() {

        }

        public void commit() {

        }

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

    protected Map connections;
    protected int status;
    protected TransactionDelegate delegate;

    /**
     * @deprecated since 1.2
     */
    protected Level logLevel;

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
        return (Transaction) currentTransaction.get();
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
    }

    /**
     * Helper method that wraps a number of queries in this transaction, runs them, and
     * commits or rolls back depending on the outcome. This method allows users to define
     * their own custom Transactions and wrap Cayenne queries in them.
     * 
     * @deprecated since 1.2 this method is not used in Cayenne and is deprecated.
     *             Thread-bound transactions should be used instead.
     */
    public void performQueries(
            QueryEngine engine,
            Collection queries,
            OperationObserver observer) throws CayenneRuntimeException {

        Transaction old = Transaction.getThreadTransaction();
        Transaction.bindThreadTransaction(this);

        try {
            // implicit begin..
            engine.performQueries(queries, observer);

            // don't commit iterated queries - leave it up to the caller
            // at the same time rollbacks of iterated queries must be processed here,
            // since caller will no longer be processing stuff on exception
            if (!observer.isIteratedResult()
                    && (getStatus() == Transaction.STATUS_ACTIVE)) {
                commit();
            }
        }
        catch (Exception ex) {
            setRollbackOnly();

            // must rethrow
            if (ex instanceof CayenneRuntimeException) {
                throw (CayenneRuntimeException) ex;
            }
            else {
                throw new CayenneRuntimeException(ex);
            }
        }
        finally {
            Transaction.bindThreadTransaction(old);
            if (getStatus() == Transaction.STATUS_MARKED_ROLLEDBACK) {
                try {
                    rollback();
                }
                catch (Exception rollbackEx) {
                }
            }
        }
    }

    /**
     * @deprecated since 1.2 unused
     */
    public Level getLogLevel() {
        return logLevel != null ? logLevel : Level.INFO;
    }

    /**
     * @deprecated since 1.2 unused
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
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

    /**
     * @deprecated since 1.2 use {@link #addConnection(String, Connection)}.
     */
    public void addConnection(Connection connection) throws IllegalStateException,
            SQLException, CayenneException {
        addConnection("x" + System.currentTimeMillis(), connection);
    }

    public abstract void commit() throws IllegalStateException, SQLException,
            CayenneException;

    public abstract void rollback() throws IllegalStateException, SQLException,
            CayenneException;

    /**
     * @since 1.2
     */
    public Connection getConnection(String name) {
        return (connections != null) ? (Connection) connections.get(name) : null;
    }

    /**
     * @since 1.2
     */
    public boolean addConnection(String name, Connection connection) throws SQLException {
        if (delegate != null && !delegate.willAddConnection(this, connection)) {
            return false;
        }

        if (connections == null) {
            connections = new HashMap();
        }

        return connections.put(name, connection) != connection;
    }
}
