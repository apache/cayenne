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
 * Represents a Cayenne-managed local Transaction.
 * 
 * @since 1.2 moved to a top-level class.
 */
class InternalTransaction extends ExternalTransaction {

    InternalTransaction(TransactionDelegate delegate) {
        super(delegate);
    }

    @Override
    public void begin() {
        super.begin();
        jdbcEventLogger.logBeginTransaction("transaction started.");
    }

    @Override
    void fixConnectionState(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            // some DBs are very particular about that, (e.g. Informix SE 7.0 per
            // CAY-179), so do a try-catch and ignore exception

            // TODO: maybe allow adapter to provide transaction instance?
            try {
                connection.setAutoCommit(false);
            }
            catch (SQLException cay179Ex) {
                // Can't set autocommit, ignoring...
            }
        }
    }

    @Override
    void processCommit() throws SQLException, CayenneException {
        status = Transaction.STATUS_COMMITTING;

        if (connections != null && connections.size() > 0) {
            Throwable deferredException = null;
            Iterator<?> it = connections.values().iterator();
            while (it.hasNext()) {
                Connection connection = (Connection) it.next();
                try {

                    if (deferredException == null) {
                        connection.commit();
                    }
                    else {
                        // we must do a partial rollback if only to cleanup
                        // uncommitted
                        // connections.
                        connection.rollback();
                    }

                }
                catch (Throwable th) {
                    // there is no such thing as "partial" rollback in real
                    // transactions, so we can't set any meaningful status.
                    // status = ?;
                    setRollbackOnly();

                    // stores last exception
                    // TODO: chain exceptions...
                    deferredException = th;
                }
            }

            if (deferredException != null) {
                jdbcEventLogger.logRollbackTransaction("transaction rolledback.");
                if (deferredException instanceof SQLException) {
                    throw (SQLException) deferredException;
                }
                else {
                    throw new CayenneException(deferredException);
                }
            }
            else {
                jdbcEventLogger.logCommitTransaction("transaction committed.");
            }
        }
    }

    @Override
    void processRollback() throws SQLException, CayenneException {
        status = Transaction.STATUS_ROLLING_BACK;

        if (connections != null && connections.size() > 0) {
            Throwable deferredException = null;

            Iterator<?> it = connections.values().iterator();
            while (it.hasNext()) {
                Connection connection = (Connection) it.next();

                try {
                    // continue with rollback even if an exception was thrown before
                    connection.rollback();
                }
                catch (Throwable th) {
                    // stores last exception
                    // TODO: chain exceptions...
                    deferredException = th;
                }
            }

            if (deferredException != null) {
                if (deferredException instanceof SQLException) {
                    throw (SQLException) deferredException;
                }
                else {
                    throw new CayenneException(deferredException);
                }
            }
        }
    }
}
