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

import org.apache.art.Artist;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class TransactionThreadTest extends CayenneCase {

    public void testThreadConnectionReuseOnSelect() throws Exception {

        Delegate delegate = new Delegate();
        Transaction t = Transaction.internalTransaction(delegate);

        Transaction.bindThreadTransaction(t);

        try {

            SelectQuery q1 = new SelectQuery(Artist.class);
            createDataContext().performQuery(q1);
            assertEquals(1, delegate.connectionCount);

            // delegate will fail if the second query opens a new connection
            SelectQuery q2 = new SelectQuery(Artist.class);
            createDataContext().performQuery(q2);

        }
        finally {
            Transaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    public void testThreadConnectionReuseOnQueryFromWillCommit() throws Exception {
        deleteTestData();

        Artist a = createDataContext().newObject(Artist.class);
        a.setArtistName("aaa");

        Delegate delegate = new Delegate() {

            @Override
            public boolean willCommit(Transaction transaction) {

                // insert another artist directly
                SQLTemplate template = new SQLTemplate(
                        Artist.class,
                        "insert into ARTIST (ARTIST_ID, ARTIST_NAME) values (1, 'bbb')");
                createDataContext().performNonSelectingQuery(template);

                return true;
            }
        };

        getDomain().setTransactionDelegate(delegate);

        try {
            a.getObjectContext().commitChanges();
        }
        finally {
            getDomain().setTransactionDelegate(null);
        }

        assertEquals(2, createDataContext()
                .performQuery(new SelectQuery(Artist.class))
                .size());
    }

    class Delegate implements TransactionDelegate {

        int connectionCount;

        public boolean willAddConnection(Transaction transaction, Connection connection) {
            if (connectionCount++ > 0) {
                fail("Invalid attempt to add connection");
            }

            return true;
        }

        public void didCommit(Transaction transaction) {
        }

        public void didRollback(Transaction transaction) {
        }

        public boolean willCommit(Transaction transaction) {
            return true;
        }

        public boolean willMarkAsRollbackOnly(Transaction transaction) {
            return true;
        }

        public boolean willRollback(Transaction transaction) {
            return true;
        }
    }
}
