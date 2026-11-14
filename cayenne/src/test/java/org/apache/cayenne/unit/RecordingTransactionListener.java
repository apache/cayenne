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
package org.apache.cayenne.unit;

import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionListener;

import java.sql.Connection;

/**
 * A transaction listener that counts the callbacks it receives. Subclasses can decorate the connections via
 * {@link #decorate(Transaction, Connection)}.
 */
public class RecordingTransactionListener implements TransactionListener {

    public int willCommitCalls;
    public int willRollbackCalls;
    public int willAddConnectionCalls;
    public int decorateConnectionCalls;

    @Override
    public void willCommit(Transaction tx) {
        willCommitCalls++;
    }

    @Override
    public void willRollback(Transaction tx) {
        willRollbackCalls++;
    }

    @Override
    public void willAddConnection(Transaction tx, String connectionName, Connection connection) {
        willAddConnectionCalls++;
    }

    @Override
    public final Connection decorateConnection(Transaction tx, Connection connection) {
        decorateConnectionCalls++;
        return decorate(tx, connection);
    }

    protected Connection decorate(Transaction tx, Connection connection) {
        return connection;
    }
}
