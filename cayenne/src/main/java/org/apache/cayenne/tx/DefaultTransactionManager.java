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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

/**
 * @since 4.0
 */
public class DefaultTransactionManager implements TransactionManager {

    private TransactionFactory txFactory;
    private JdbcEventLogger jdbcEventLogger;

    public DefaultTransactionManager(@Inject TransactionFactory txFactory, @Inject JdbcEventLogger jdbcEventLogger) {
        this.txFactory = txFactory;
        this.jdbcEventLogger = jdbcEventLogger;
    }

    @Override
    public <T> T performInTransaction(TransactionalOperation<T> op) {
        return performInTransaction(op, DoNothingTransactionListener.getInstance(), TransactionDescriptor.defaultDescriptor());
    }

    @Override
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback) {
        return performInTransaction(op, callback, TransactionDescriptor.defaultDescriptor());
    }

    /**
     * @since 4.1
     */
    @Override
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionDescriptor descriptor) {
        return performInTransaction(op, DoNothingTransactionListener.getInstance(), descriptor);
    }

    /**
     * @since 4.1
     */
    @Override
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
        BaseTransactionHandler handler = getHandler(descriptor);
        return handler.handle(op, callback, descriptor);
    }

    protected BaseTransactionHandler getHandler(TransactionDescriptor descriptor) {
        switch (descriptor.getPropagation()) {
            // MANDATORY requires transaction to exists
            case MANDATORY:
                return new MandatoryTransactionHandler(txFactory, jdbcEventLogger);

            // NESTED can join existing or create new
            case NESTED:
                return new NestedTransactionHandler(txFactory, jdbcEventLogger);

            // REQUIRES_NEW should always create new transaction
            case REQUIRES_NEW:
                return new RequiresNewTransactionHandler(txFactory, jdbcEventLogger);
        }

        throw new CayenneRuntimeException("Unsupported transaction propagation: " + descriptor.getPropagation());
    }

    private static class NestedTransactionHandler extends BaseTransactionHandler {

        private NestedTransactionHandler(TransactionFactory txFactory, JdbcEventLogger jdbcEventLogger) {
            super(txFactory, jdbcEventLogger);
        }

        @Override
        protected <T> T handle(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
            Transaction currentTx = BaseTransaction.getThreadTransaction();
            if(currentTx != null) {
                return performInTransaction(currentTx, op, callback);
            } else {
                return performInNewTransaction(op, callback, descriptor);
            }
        }
    }

    private static class MandatoryTransactionHandler extends BaseTransactionHandler {

        private MandatoryTransactionHandler(TransactionFactory txFactory, JdbcEventLogger jdbcEventLogger) {
            super(txFactory, jdbcEventLogger);
        }

        @Override
        protected <T> T handle(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
            Transaction currentTx = BaseTransaction.getThreadTransaction();
            if(currentTx == null) {
                throw new CayenneRuntimeException("Transaction operation should join to existing transaction but none found.");
            }
            return performInTransaction(currentTx, op, callback);
        }
    }

    private static class RequiresNewTransactionHandler extends BaseTransactionHandler {

        private RequiresNewTransactionHandler(TransactionFactory txFactory, JdbcEventLogger jdbcEventLogger) {
            super(txFactory, jdbcEventLogger);
        }

        @Override
        protected <T> T handle(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
            Transaction currentTx = BaseTransaction.getThreadTransaction();
            try {
                return performInNewTransaction(op, callback, descriptor);
            } finally {
                if(currentTx != null) {
                    // restore old transaction, if where set
                    BaseTransaction.bindThreadTransaction(currentTx);
                }
            }
        }
    }

    protected static abstract class BaseTransactionHandler {

        private TransactionFactory txFactory;
        private JdbcEventLogger jdbcEventLogger;

        private BaseTransactionHandler(TransactionFactory txFactory, JdbcEventLogger jdbcEventLogger) {
            this.txFactory = txFactory;
            this.jdbcEventLogger = jdbcEventLogger;
        }

        protected abstract <T> T handle(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor);

        protected <T> T performInNewTransaction(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor) {
            Transaction tx = txFactory.createTransaction(descriptor);
            BaseTransaction.bindThreadTransaction(tx);
            try {
                T result = performInTransaction(tx, op, callback);
                tx.commit();
                return result;

            } catch (CayenneRuntimeException ex) {
                tx.setRollbackOnly();
                throw ex;
            } catch (Exception ex) {
                tx.setRollbackOnly();
                throw new CayenneRuntimeException(ex);
            } finally {
                BaseTransaction.bindThreadTransaction(null);

                if (tx.isRollbackOnly()) {
                    try {
                        tx.rollback();
                    } catch (Exception e) {
                        // although we don't expect an exception here, print the
                        // stack, as there have been some Cayenne bugs already
                        // (CAY-557) that were masked by this 'catch' clause.
                        jdbcEventLogger.logQueryError(e);
                    }
                }
            }
        }

        protected <T> T performInTransaction(Transaction tx, TransactionalOperation<T> op, TransactionListener callback) {
            tx.addListener(callback);
            return op.perform();
        }

    }

}
