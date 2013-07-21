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
import java.util.Iterator;

import org.apache.cayenne.CayenneException;

/**
 * Represents a container-managed transaction.
 * 
 * @since 1.2 moved to a top-level class.
 */
class ExternalTransaction extends Transaction {

    ExternalTransaction() {
    }

    ExternalTransaction(TransactionDelegate delegate) {
        setDelegate(delegate);
    }

    @Override
    public synchronized void begin() {
        if (status != Transaction.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException(
                    "Transaction must have 'STATUS_NO_TRANSACTION' to begin. "
                            + "Current status: "
                            + Transaction.decodeStatus(status));
        }

        status = Transaction.STATUS_ACTIVE;
    }

    @Override
    public boolean addConnection(String name, Connection connection) throws SQLException {
        if (super.addConnection(name, connection)) {

            // implicitly begin transaction
            if (status == Transaction.STATUS_NO_TRANSACTION) {
                begin();
            }

            if (status != Transaction.STATUS_ACTIVE) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' to add a connection. "
                                + "Current status: "
                                + Transaction.decodeStatus(status));
            }

            fixConnectionState(connection);
            return true;
        }
        else {
            return false;
        }

    }

    @Override
    public void commit() throws IllegalStateException, SQLException, CayenneException {

        if (status == Transaction.STATUS_NO_TRANSACTION) {
            return;
        }

        if (delegate != null && !delegate.willCommit(this)) {
            return;
        }

        if (status != Transaction.STATUS_ACTIVE) {
            throw new IllegalStateException(
                    "Transaction must have 'STATUS_ACTIVE' to be committed. "
                            + "Current status: "
                            + Transaction.decodeStatus(status));
        }

        processCommit();

        status = Transaction.STATUS_COMMITTED;

        if (delegate != null) {
            delegate.didCommit(this);
        }

        close();
    }

    @Override
    public void rollback() throws IllegalStateException, SQLException, CayenneException {

        try {
            if (status == Transaction.STATUS_NO_TRANSACTION
                    || status == Transaction.STATUS_ROLLEDBACK
                    || status == Transaction.STATUS_ROLLING_BACK) {
                return;
            }

            if (delegate != null && !delegate.willRollback(this)) {
                return;
            }

            if (status != Transaction.STATUS_ACTIVE
                    && status != Transaction.STATUS_MARKED_ROLLEDBACK) {
                throw new IllegalStateException(
                        "Transaction must have 'STATUS_ACTIVE' or 'STATUS_MARKED_ROLLEDBACK' to be rolled back. "
                                + "Current status: "
                                + Transaction.decodeStatus(status));
            }

            processRollback();

            status = Transaction.STATUS_ROLLEDBACK;
            if (delegate != null) {
                delegate.didRollback(this);
            }
        }
        finally {
            close();
        }
    }

    void fixConnectionState(Connection connection) throws SQLException {
        // NOOP
    }

    void processCommit() throws SQLException, CayenneException {
        jdbcEventLogger.logCommitTransaction("no commit - transaction controlled externally.");
    }

    void processRollback() throws SQLException, CayenneException {
        jdbcEventLogger.logRollbackTransaction("no rollback - transaction controlled externally.");
    }

    /**
     * Closes all connections associated with transaction.
     */
    void close() {
        if (connections == null || connections.isEmpty()) {
            return;
        }

        Iterator<?> it = connections.values().iterator();
        while (it.hasNext()) {
            try {

                ((Connection) it.next()).close();
            }
            catch (Throwable th) {
                // TODO: chain exceptions...
                // ignore for now
            }
        }
    }
}
