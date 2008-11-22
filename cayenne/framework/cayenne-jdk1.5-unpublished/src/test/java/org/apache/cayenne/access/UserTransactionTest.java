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
import org.apache.cayenne.unit.CayenneCase;

public class UserTransactionTest extends CayenneCase {

    public void testCommit() throws Exception {
        DataContext context = createDataContext();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");

        final boolean[] willAddConnectionCalled = new boolean[1];
        final boolean[] willCommitCalled = new boolean[1];
        final boolean[] didCommitCalled = new boolean[1];

        TransactionDelegate delegate = new MockTransactionDelegate() {

            @Override
            public boolean willAddConnection(
                    Transaction transaction,
                    Connection connection) {
                willAddConnectionCalled[0] = true;
                return true;
            }

            @Override
            public boolean willCommit(Transaction transaction) {
                willCommitCalled[0] = true;
                return true;
            }

            @Override
            public void didCommit(Transaction transaction) {
                didCommitCalled[0] = true;
            }
        };

        Transaction t = Transaction.internalTransaction(delegate);
        Transaction.bindThreadTransaction(t);

        try {
            context.commitChanges();

            assertTrue("User transaction was ignored", willAddConnectionCalled[0]);
            assertFalse(
                    "User transaction was unexpectedly committed",
                    willCommitCalled[0]);
            assertFalse("User transaction was unexpectedly committed", didCommitCalled[0]);
        }
        finally {

            try {
                t.rollback();
            }
            catch (Throwable th) {
                // ignore
            }
            Transaction.bindThreadTransaction(null);
        }

    }
}
