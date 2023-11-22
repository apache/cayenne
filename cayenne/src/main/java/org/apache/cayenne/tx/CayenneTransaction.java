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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.log.JdbcEventLogger;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Represents a Cayenne-managed local Transaction.
 * 
 * @since 4.0
 */
public class CayenneTransaction extends BaseTransaction {

    protected JdbcEventLogger logger;

    public CayenneTransaction(JdbcEventLogger logger) {
        this(logger, TransactionDescriptor.defaultDescriptor());
    }

    /**
     * @since 4.1
     */
    public CayenneTransaction(JdbcEventLogger jdbcEventLogger, TransactionDescriptor descriptor) {
        super(descriptor);
        this.logger = jdbcEventLogger;
    }

    @Override
    public void begin() {
        super.begin();
        logger.logBeginTransaction("transaction started.");
    }

    @Override
    protected void connectionAdded(Connection connection) {
        super.connectionAdded(connection);

        try {
            fixConnectionState(connection);
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Exception changing connection state", e);
        }
    }

    void fixConnectionState(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            // some DBs are very particular about that, (e.g. Informix SE 7.0 per CAY-179), so do a try-catch and
            // ignore exception

            // TODO: maybe allow adapter to provide transaction instance?
            try {
                connection.setAutoCommit(false);
            } catch (SQLException cay179Ex) {
                // Can't set autocommit, ignoring...
            }
        }
    }

    @Override
    protected void processCommit() {
        status = BaseTransaction.STATUS_COMMITTING;

        if (connections == null || connections.isEmpty()) {
            return;
        }

        Throwable deferredException = null;
        for (Connection connection : connections.values()) {
            try {
                if (deferredException == null) {
                    connection.commit();
                } else {
                    // we must do a partial rollback if only to cleanup uncommitted connections.
                    connection.rollback();
                }
            } catch (Throwable th) {
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
            logger.logRollbackTransaction("transaction rolledback.");
            throw new CayenneRuntimeException(deferredException);
        } else {
            logger.logCommitTransaction("transaction committed.");
        }
    }

    @Override
    protected void processRollback() {
        status = BaseTransaction.STATUS_ROLLING_BACK;

        if (connections == null || connections.isEmpty()) {
            return;
        }

        Throwable deferredException = null;
        for (Connection connection : connections.values()) {
            try {
                // continue with rollback even if an exception was thrown
                // before
                connection.rollback();
            } catch (Throwable th) {
                // stores last exception
                // TODO: chain exceptions...
                deferredException = th;
            }
        }

        logger.logRollbackTransaction("transaction rolledback.");
        if (deferredException != null) {
            throw new CayenneRuntimeException(deferredException);
        }
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
