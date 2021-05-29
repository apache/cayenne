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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Check if connection can be decorated with listeners and that default connection of TransactionDescriptor
 * has major priority
 * @see BaseTransaction,TransactionDescriptor
 * @since 4.2.M4
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TransactionCustomConnectionIT extends ServerCase {

    private final Logger logger = LoggerFactory.getLogger(TransactionIsolationIT.class);

    @Inject
    DataContext context;

    @Inject
    ServerRuntime runtime;

    @Inject
    private JdbcEventLogger jdbcEventLogger;

    TransactionManager manager;

    @Before
    public void initTransactionManager() {
        // no binding in test container, get it from runtime
        manager = runtime.getInjector().getInstance(TransactionManager.class);
    }

    @Test
    public void testConnectionDecorationWithListeners() throws Exception {
        Transaction t = new CayenneTransaction(jdbcEventLogger);
        //add listeners which will check if connection object will be changed after every decorate call
        List<TransactionListener> listeners = addAndGetListenersWithCustomReadonlyToTransaction(t);
        BaseTransaction.bindThreadTransaction(t);

        try {
            ObjectSelect.query(Artist.class).select(context);
            assertEquals(1, t.getConnections().size());

            //check if the last listener set readonly property to false
            t.getConnections().forEach((key, connection) -> {
                try {
                    assertFalse(connection.isReadOnly());
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            });

            //check if every decoration from listener was called
            for (TransactionListener transactionListener : listeners) {
                verify(transactionListener).decorateConnection(any(), any());
            }
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    private List<TransactionListener> addAndGetListenersWithCustomReadonlyToTransaction(Transaction t) {
        Class<?>[] classes = new Class[]{ListenerWithFalseReadonlyDecorator.class, ListenerWithTrueReadonlyDecorator.class};
        List<TransactionListener> listeners = new ArrayList<>();
        for (Class<?> aClass : classes) {
            TransactionListener listener = (TransactionListener) mock(aClass);
            listeners.add(listener);
            when(listener.decorateConnection(any(), any())).thenCallRealMethod();
            t.addListener(listener);
        }
        return listeners;
    }

    //listener, which will check if readonly property of connection is false and set it to true
    class ListenerWithFalseReadonlyDecorator implements TransactionListener {

        @Override
        public void willCommit(Transaction tx) {

        }

        @Override
        public void willRollback(Transaction tx) {

        }

        @Override
        public void willAddConnection(Transaction tx, String connectionName, Connection connection) {

        }

        @Override
        public Connection decorateConnection(Transaction tx, Connection connection) {
            try {
                assertFalse(connection.isReadOnly());
                connection.setReadOnly(true);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return connection;
        }
    }

    //listener, which will check if readonly property of connection is true and set it to false
    class ListenerWithTrueReadonlyDecorator implements TransactionListener {

        @Override
        public void willCommit(Transaction tx) {

        }

        @Override
        public void willRollback(Transaction tx) {

        }

        @Override
        public void willAddConnection(Transaction tx, String connectionName, Connection connection) {

        }

        @Override
        public Connection decorateConnection(Transaction tx, Connection connection) {
            try {
                assertTrue(connection.isReadOnly());
                connection.setReadOnly(false);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return connection;
        }
    }

    @Test
    public void testDefaultConnectionInDescriptor() {
        Transaction t = new CayenneTransaction(jdbcEventLogger);
        BaseTransaction.bindThreadTransaction(t);
        try {
            ObjectSelect.query(Artist.class).select(context);
            Connection connection = t.getConnections().values().stream().findFirst().get();

            TransactionDescriptor mockDescriptor = mock(TransactionDescriptor.class);
            when(mockDescriptor.getCustomConnectionSupplier()).thenReturn(() -> connection);
            when(mockDescriptor.getPropagation()).thenReturn(TransactionPropagation.REQUIRES_NEW);
            when(mockDescriptor.getIsolation()).thenReturn(Connection.TRANSACTION_SERIALIZABLE);

            performInTransaction(mockDescriptor);
            verify(mockDescriptor, times(2)).getCustomConnectionSupplier();
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }

    }

    private void performInTransaction(TransactionDescriptor descriptor) {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("test");
        manager.performInTransaction(() -> {
            artist.setArtistName("test3");
            context.commitChanges();
            return null;
        }, descriptor);
    }
}
