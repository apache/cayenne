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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.CayenneTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionListener;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TransactionThreadIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private JdbcEventLogger logger;

    @Test
    public void testThreadConnectionReuseOnSelect() throws Exception {

        ConnectionCounterTx t = new ConnectionCounterTx(new CayenneTransaction(logger));
        BaseTransaction.bindThreadTransaction(t);

        try {

            SelectQuery q1 = new SelectQuery(Artist.class);
            context.performQuery(q1);
            assertEquals(1, t.connectionCount);

            // delegate will fail if the second query opens a new connection
            SelectQuery q2 = new SelectQuery(Artist.class);
            context.performQuery(q2);

        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    class ConnectionCounterTx implements Transaction {

        private Transaction delegate;
        int connectionCount;

        ConnectionCounterTx(Transaction delegate) {
            this.delegate = delegate;
        }

        public void begin() {
            delegate.begin();
        }

        public void commit() {
            delegate.commit();
        }

        public void rollback() {
            delegate.rollback();
        }

        public void setRollbackOnly() {
            delegate.setRollbackOnly();
        }

        public boolean isRollbackOnly() {
            return delegate.isRollbackOnly();
        }

        public Connection getConnection(String name) {
            return delegate.getConnection(name);
        }

        public void addConnection(String name, Connection connection) {
            if (connectionCount++ > 0) {
                fail("Invalid attempt to add connection");
            }

            delegate.addConnection(name, connection);
        }

        @Override
        public void addListener(TransactionListener listener) {
            delegate.addListener(listener);
        }
    }
}
