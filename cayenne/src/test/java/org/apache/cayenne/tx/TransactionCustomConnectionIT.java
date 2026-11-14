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
import org.apache.cayenne.log.NoopSQLLogger;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.RecordingTransactionListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Check if connection can be decorated with listeners and that default connection of TransactionDescriptor
 * has major priority
 *
 * @see BaseTransaction,TransactionDescriptor
 */
public class TransactionCustomConnectionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    DataContext context;
    CayenneRuntime runtime;

    TransactionManager manager;
    private static boolean firstReadonlyCondition;

    @BeforeEach
    public void initTransactionManager() {
        context = env.context();
        runtime = env.runtime();
        // no binding in test container, get it from runtime
        manager = runtime.getInjector().getInstance(TransactionManager.class);
    }

    /**
     * Test depends on decoration of readonly property of the connection, but not every driver supports readonly setting.
     * So this test calculated for drivers with current support, but if it is not supported, this test mustn't fail
     * because it checks if readonly wasn't changed by setter and changes firstReadonlyCondition flag value
     * if it is true to avoid fails. In that case test is useless, but it's behavior in other cases can submit
     * right behavior of methods
     */
    @Test
    public void connectionDecorationWithListeners() {
        Transaction t = new CayenneTransaction(NoopSQLLogger.getInstance());
        //add listeners which will check if connection object will be changed after every decorate call
        List<RecordingTransactionListener> listeners = addAndGetListenersWithCustomReadonlyTo(t);
        BaseTransaction.bindThreadTransaction(t);
        try {
            ObjectSelect.query(Artist.class).select(context);

            //check if the last listener set readonly property to false
            t.getConnections().forEach((key, connection) -> {
                try {
                    assertEquals(connection.isReadOnly(), firstReadonlyCondition);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            });

            //check if every decoration from listener was called
            for (RecordingTransactionListener transactionListener : listeners) {
                assertEquals(1, transactionListener.decorateConnectionCalls);
            }
        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }

    private List<RecordingTransactionListener> addAndGetListenersWithCustomReadonlyTo(Transaction t) {
        List<RecordingTransactionListener> listeners =
                List.of(new ListenerWithFirstReadonlyDecorator(), new ListenerWithSecondReadonlyDecorator());
        listeners.forEach(t::addListener);
        return listeners;
    }

    //listener, which will check if readonly property of connection is false and set it to true
    class ListenerWithFirstReadonlyDecorator extends RecordingTransactionListener {

        @Override
        protected Connection decorate(Transaction tx, Connection connection) {
            try {
                firstReadonlyCondition = connection.isReadOnly();
                connection.setReadOnly(!firstReadonlyCondition);

                if (connection.isReadOnly() == firstReadonlyCondition) {
                    firstReadonlyCondition = !connection.isReadOnly();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return connection;
        }
    }

    //listener, which will check if readonly property of connection is true and set it to false
    class ListenerWithSecondReadonlyDecorator extends RecordingTransactionListener {

        @Override
        protected Connection decorate(Transaction tx, Connection connection) {
            try {
                assertEquals(!firstReadonlyCondition, connection.isReadOnly());
                connection.setReadOnly(!connection.isReadOnly());
                if (connection.isReadOnly() == !firstReadonlyCondition) {
                    firstReadonlyCondition = !firstReadonlyCondition;
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return connection;
        }
    }

    @Test
    public void defaultConnectionInDescriptor() {
        Transaction t = new CayenneTransaction(NoopSQLLogger.getInstance());
        BaseTransaction.bindThreadTransaction(t);
        try {
            ObjectSelect.query(Artist.class).select(context);
            Connection connection = t.getConnections().values().stream().findFirst().get();

            try {
                connection.setAutoCommit(true);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

            AtomicInteger supplied = new AtomicInteger();
            TransactionDescriptor descriptor = TransactionDescriptor.builder()
                    .connectionSupplier(() -> {
                        supplied.incrementAndGet();
                        return connection;
                    })
                    .propagation(TransactionPropagation.REQUIRES_NEW)
                    .isolation(Connection.TRANSACTION_SERIALIZABLE)
                    .build();

            performInTransaction(descriptor);

            // the connection of the descriptor is used in place of a new one
            assertTrue(supplied.get() > 0);
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
