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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class UserTransactionTest extends ServerCase {

    @Inject
    private ObjectContext context;
    
    @Inject
    private JdbcEventLogger logger;

    public void testCommit() throws Exception {

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");

        TransactionDelegate delegate = mock(TransactionDelegate.class);
        Transaction t = Transaction.internalTransaction(delegate);
        t.setJdbcEventLogger(logger);

        when(delegate.willAddConnection(eq(t), any(Connection.class))).thenReturn(true);
        when(delegate.willCommit(t)).thenReturn(true);

        Transaction.bindThreadTransaction(t);

        try {
            context.commitChanges();

            verify(delegate, atLeast(1)).willAddConnection(eq(t), any(Connection.class));
            verify(delegate, never()).willCommit(eq(t));
            verify(delegate, never()).didCommit(eq(t));
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
