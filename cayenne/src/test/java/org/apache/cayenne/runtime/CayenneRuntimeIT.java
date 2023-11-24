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
package org.apache.cayenne.runtime;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionListener;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.validation.ValidationException;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CayenneRuntimeIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private ObjectContext context;

    static class DefaultListenerImpl implements TransactionListener {

        @Override
        public void willCommit(Transaction tx) {
        }

        @Override
        public void willRollback(Transaction tx) {
        }

        @Override
        public void willAddConnection(Transaction tx, String connectionName, Connection connection) {
        }
    }

    @Test
    public void testPerformInTransaction_Local_Callback() {

        TransactionListener callback = mock(DefaultListenerImpl.class);
        when(callback.decorateConnection(any(Transaction.class), any(Connection.class))).thenCallRealMethod();

        Artist a = runtime.performInTransaction(() -> {
            Artist localArtist = runtime.newContext().newObject(Artist.class);
            localArtist.setArtistName("A1");
            localArtist.getObjectContext().commitChanges();
            return localArtist;
        }, callback);

        assertEquals("A1", a.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        verify(callback).willCommit(any(Transaction.class));
        verify(callback).willAddConnection(any(Transaction.class), any(String.class), any(Connection.class));
        verify(callback, times(0)).willRollback(any(Transaction.class));
        verify(callback).decorateConnection(any(Transaction.class), any(Connection.class));
    }

    @Test
    public void testPerformInTransaction_Local_Callback_Rollback() {

        TransactionListener callback = mock(TransactionListener.class);
        when(callback.decorateConnection(any(Transaction.class), any(Connection.class))).thenCallRealMethod();

        try {
            runtime.performInTransaction(() -> {
                Artist localArtist = runtime.newContext().newObject(Artist.class);
                localArtist.getObjectContext().commitChanges();
                return localArtist;
            }, callback);

            fail("Exception expected");
        } catch (ValidationException v) {
            verify(callback).willRollback(any(Transaction.class));
            verify(callback, times(0)).willAddConnection(any(Transaction.class), any(String.class), any(Connection.class));
            verify(callback, times(0)).willCommit(any(Transaction.class));
        }
    }

    @Test
    public void testRollbackTransaction() {
        assertEquals(0, ObjectSelect.query(Artist.class).selectCount(context));

        try {
            runtime.performInTransaction(() -> {
                // Default PK batch size is 20
                for (int i = 0; i < 30; i++) {
                    Artist artist = context.newObject(Artist.class);
                    artist.setArtistName("test" + i);
                    context.commitChanges();
                }

                // this should fail with validation error
                context.newObject(Artist.class);
                context.commitChanges();
                return null;
            });
        } catch (Exception ignored) {
        }

        assertEquals(0, ObjectSelect.query(Artist.class).selectCount(context));
    }
}
