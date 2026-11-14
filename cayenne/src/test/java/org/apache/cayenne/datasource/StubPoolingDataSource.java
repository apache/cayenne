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
package org.apache.cayenne.datasource;

import org.apache.cayenne.unit.jdbc.TestDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool with no connections of its own, for the tests of the code around the pool. It hands out the queued
 * connections, from {@link #getConnection()} and on reconnect, failing when there are none, and counts the calls
 * to the methods that manage the pool.
 */
class StubPoolingDataSource extends UnmanagedPoolingDataSource {

    final Queue<Connection> connections = new ConcurrentLinkedQueue<>();
    final Queue<Connection> reconnects = new ConcurrentLinkedQueue<>();
    final AtomicInteger createUnwrappedCalls = new AtomicInteger();
    final AtomicInteger managePoolCalls = new AtomicInteger();
    final AtomicInteger closeCalls = new AtomicInteger();

    StubPoolingDataSource(Connection... connections) {
        super(new TestDataSource(), parameters());
        this.connections.addAll(List.of(connections));
    }

    private static PoolingDataSourceParameters parameters() {
        PoolingDataSourceParameters parameters = new PoolingDataSourceParameters();
        parameters.setMaxConnections(1);
        return parameters;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection c = connections.poll();
        if (c == null) {
            throw new SQLException("No more connections");
        }
        return c;
    }

    @Override
    Connection createUnwrapped() throws SQLException {
        createUnwrappedCalls.incrementAndGet();
        Connection c = reconnects.poll();
        if (c == null) {
            throw new SQLException("Can't reconnect");
        }
        return c;
    }

    @Override
    void managePool() {
        managePoolCalls.incrementAndGet();
    }

    @Override
    public void close() {
        closeCalls.incrementAndGet();
    }
}
