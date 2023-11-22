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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.log.JdbcEventLogger;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultTransactionManagerIT {

    @Test
    public void testPerformInTransaction_Local() {

        final BaseTransaction tx = mock(BaseTransaction.class);

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);

        final Object expectedResult = new Object();
        Object result = txManager.performInTransaction(() -> {
            assertSame(tx, BaseTransaction.getThreadTransaction());
            return expectedResult;
        });

        assertSame(expectedResult, result);
    }

    @Test
    public void testPerformInTransaction_ExistingTx() {

        final BaseTransaction tx1 = mock(BaseTransaction.class);

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx1);

        final BaseTransaction tx2 = mock(BaseTransaction.class);
        BaseTransaction.bindThreadTransaction(tx2);
        try {

            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(() -> {
                assertSame(tx2, BaseTransaction.getThreadTransaction());
                return expectedResult;
            });

            assertSame(expectedResult, result);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }
    }

    @Test
    public void testNestedPropagation() {
        final BaseTransaction tx = mock(BaseTransaction.class);

        assertNull(BaseTransaction.getThreadTransaction());

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);

        try {
            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(() -> {
                        assertSame(tx, BaseTransaction.getThreadTransaction());
                        return expectedResult;
                    },
                    TransactionDescriptor.builder()
                            .propagation(TransactionPropagation.NESTED)
                            .build()
            );
            assertSame(expectedResult, result);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }

    }

    @Test(expected = CayenneRuntimeException.class)
    public void testMandatoryPropagationNotStarted() {
        final BaseTransaction tx = mock(BaseTransaction.class);

        assertNull(BaseTransaction.getThreadTransaction());

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);

        try {
            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(() -> {
                        assertSame(tx, BaseTransaction.getThreadTransaction());
                        return expectedResult;
                    },
                    TransactionDescriptor.builder()
                            .propagation(TransactionPropagation.MANDATORY)
                            .build()
            );
            assertSame(expectedResult, result);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }

    }

    @Test
    public void testMandatoryPropagation() {
        final BaseTransaction tx = mock(BaseTransaction.class);

        assertNull(BaseTransaction.getThreadTransaction());

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);
        BaseTransaction.bindThreadTransaction(tx);

        try {
            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(() -> {
                        assertSame(tx, BaseTransaction.getThreadTransaction());
                        return expectedResult;
                    },
                    TransactionDescriptor.builder()
                            .propagation(TransactionPropagation.MANDATORY)
                            .build()
            );
            assertSame(expectedResult, result);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }

    }

    @Test
    public void testRequiresNewPropagation() {
        final BaseTransaction tx1 = mock(BaseTransaction.class);
        final BaseTransaction tx2 = mock(BaseTransaction.class);
        final AtomicInteger counter = new AtomicInteger(0);

        assertNull(BaseTransaction.getThreadTransaction());

        DefaultTransactionManager txManager = createDefaultTxManager(() -> {
            counter.incrementAndGet();
            return tx2;
        });

        BaseTransaction.bindThreadTransaction(tx1);

        try {
            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(() -> {
                        assertSame(tx2, BaseTransaction.getThreadTransaction());
                        return expectedResult;
                    },
                    TransactionDescriptor.builder()
                            .propagation(TransactionPropagation.REQUIRES_NEW)
                            .build()
            );
            assertSame(expectedResult, result);
            assertSame(tx1, BaseTransaction.getThreadTransaction());
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }

    }

    private DefaultTransactionManager createDefaultTxManager(final Supplier<Transaction> txSupplier) {
        return new DefaultTransactionManager(
                createMockFactory(txSupplier),
                mock(JdbcEventLogger.class)
        );
    }

    private TransactionFactory createMockFactory(final Supplier<Transaction> supplier) {
        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.createTransaction()).thenReturn(supplier.get());
        when(txFactory.createTransaction(any(TransactionDescriptor.class))).thenReturn(supplier.get());
        return txFactory;
    }

}
