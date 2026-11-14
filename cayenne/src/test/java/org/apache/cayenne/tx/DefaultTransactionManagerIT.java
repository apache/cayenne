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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultTransactionManagerIT {

    @Test
    public void performInTransaction_Local() {

        final Transaction tx = new ReadOnlyTransaction();

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);

        final Object expectedResult = new Object();
        Object result = txManager.performInTransaction(() -> {
            assertSame(tx, BaseTransaction.getThreadTransaction());
            return expectedResult;
        });

        assertSame(expectedResult, result);
    }

    @Test
    public void performInTransaction_ExistingTx() {

        final Transaction tx1 = new ReadOnlyTransaction();

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx1);

        final Transaction tx2 = new ReadOnlyTransaction();
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
    public void nestedPropagation() {
        final Transaction tx = new ReadOnlyTransaction();

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

    @Test
    public void mandatoryPropagationNotStarted() {
        final Transaction tx = new ReadOnlyTransaction();

        assertNull(BaseTransaction.getThreadTransaction());

        DefaultTransactionManager txManager = createDefaultTxManager(() -> tx);

        try {
            assertThrows(CayenneRuntimeException.class, () -> {
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
            });
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }

    }

    @Test
    public void mandatoryPropagation() {
        final Transaction tx = new ReadOnlyTransaction();

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
    public void requiresNewPropagation() {
        final Transaction tx1 = new ReadOnlyTransaction();
        final Transaction tx2 = new ReadOnlyTransaction();
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
        return new DefaultTransactionManager(descriptor -> txSupplier.get());
    }

}
