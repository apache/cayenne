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

import java.sql.Connection;
import java.util.List;

import org.apache.cayenne.unit.jdbc.TestConnection;
import org.apache.cayenne.unit.jdbc.TestDataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ReadOnlyTransactionTest {

    @Test
    public void notExternal() {
        assertFalse(new ReadOnlyTransaction().isExternal());
    }

    @Test
    public void addListenerIsNoOp() {
        // read-only queries must not fire transaction events
        new ReadOnlyTransaction().addListener(new TransactionListener() {
            @Override
            public void willCommit(Transaction tx) {
            }

            @Override
            public void willRollback(Transaction tx) {
            }

            @Override
            public void willAddConnection(Transaction tx, String connectionName, Connection connection) {
            }
        });
    }

    @Test
    public void ensuresAutoCommitAndDoesNotCloseEarly() throws Exception {
        TestConnection connection = new TestConnection().autoCommit(false);
        TestDataSource dataSource = new TestDataSource();
        dataSource.setupConnection(connection);

        ReadOnlyTransaction tx = new ReadOnlyTransaction();
        Connection handed = tx.getOrCreateConnection("node", dataSource);

        // reads run in autocommit mode
        assertEquals(List.of("setAutoCommit(true)"), connection.getCalls());

        // the handed-out connection is a decorator that does not close the real connection
        assertNotSame(connection, handed);
        handed.close();
        assertFalse(connection.isClosed());
    }

    @Test
    public void commitClosesWithoutCommittingOrRollingBack() throws Exception {
        TestConnection connection = new TestConnection().autoCommit(true);
        TestDataSource dataSource = new TestDataSource();
        dataSource.setupConnection(connection);

        ReadOnlyTransaction tx = new ReadOnlyTransaction();
        tx.getOrCreateConnection("node", dataSource);
        tx.commit();

        // a read-only transaction never commits, rolls back, or disables autocommit; it just
        // returns the connection to the pool by closing it
        assertEquals(List.of("close"), connection.getCalls());
    }
}
