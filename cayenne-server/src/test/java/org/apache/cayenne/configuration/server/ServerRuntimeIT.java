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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionListener;
import org.apache.cayenne.tx.TransactionalOperation;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.validation.ValidationException;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ServerRuntimeIT extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Test
    public void testPerformInTransaction_Local_Callback() {

        TransactionListener callback = mock(TransactionListener.class);

        Artist a = runtime.performInTransaction(new TransactionalOperation<Artist>() {

            @Override
            public Artist perform() {

                Artist localArtist = runtime.newContext().newObject(Artist.class);
                localArtist.setArtistName("A1");
                localArtist.getObjectContext().commitChanges();
                return localArtist;
            }
        }, callback);

        assertEquals("A1", a.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        verify(callback).willCommit(any(Transaction.class));
        verify(callback).willAddConnection(any(Transaction.class), any(String.class), any(Connection.class));
        verify(callback, times(0)).willRollback(any(Transaction.class));
    }

    @Test
    public void testPerformInTransaction_Local_Callback_Rollback() {

        TransactionListener callback = mock(TransactionListener.class);

        try {
            runtime.performInTransaction(new TransactionalOperation<Artist>() {

                @Override
                public Artist perform() {

                    Artist localArtist = runtime.newContext().newObject(Artist.class);
                    localArtist.getObjectContext().commitChanges();
                    return localArtist;
                }
            }, callback);

            fail("Exception expected");
        } catch (ValidationException v) {
            verify(callback).willRollback(any(Transaction.class));
            verify(callback, times(0)).willAddConnection(any(Transaction.class), any(String.class), any(Connection.class));
            verify(callback, times(0)).willCommit(any(Transaction.class));
        }
    }
}
