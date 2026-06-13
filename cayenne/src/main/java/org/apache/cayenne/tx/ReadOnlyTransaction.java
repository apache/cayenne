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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A minimal {@link Transaction} for read-only (selecting) queries that run outside a managed
 * transaction. It is not a real transaction - it exists only so that a DataNode has a thread-bound
 * transaction to acquire and release its connection through, and it keeps that connection in
 * autocommit mode (each select commits on its own, which is optimal for reads). It never commits or
 * rolls back, and fires no {@link TransactionListener} events.
 * <p>
 * Reads that require a real transaction (e.g. materializing PostgreSQL {@code oid}/BLOB large
 * objects) are not routed here - they use a {@link CayenneTransaction}.
 *
 * @since 5.0
 */
public class ReadOnlyTransaction implements Transaction {

    private Map<String, Connection> connections;
    private boolean rollbackOnly;

    @Override
    public Connection getOrCreateConnection(String connectionName, DataSource dataSource) throws SQLException {
        if (connections == null) {
            connections = new HashMap<>();
        }

        Connection c = connections.get(connectionName);
        if (c == null || c.isClosed()) {
            c = createConnection(dataSource);
            connections.put(connectionName, c);
        }

        // prevent callers from closing the connection - this transaction closes it on commit/rollback
        return new TransactionConnectionDecorator(c);
    }

    private Connection createConnection(DataSource dataSource) throws SQLException {
        Connection c = dataSource.getConnection();

        // Auto commit saves time on explicit commit to DB, reducing the overall load and increasing throughput.
        // This is the main reason we have ReadOnlyTransaction.
        if (!c.getAutoCommit()) {
            c.setAutoCommit(true);
        }

        return c;
    }

    @Override
    public void commit() {
        closeConnections();
    }

    @Override
    public void rollback() {
        closeConnections();
    }

    private void closeConnections() {
        if (connections == null) {
            return;
        }

        for (Connection c : connections.values()) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // returning the connection to the pool; nothing useful to do on failure
            }
        }
        connections = null;
    }

    @Override
    public Map<String, Connection> getConnections() {
        return connections != null ? Collections.unmodifiableMap(connections) : Collections.emptyMap();
    }

    @Override
    public void begin() {
        // no real transaction is started
    }

    @Override
    public void setRollbackOnly() {
        // tracked so the TransactionManager closes the connection via rollback() on the error path;
        // there is no actual DB transaction to roll back
        this.rollbackOnly = true;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void addListener(TransactionListener listener) {
        // read-only queries intentionally fire no transaction events
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
