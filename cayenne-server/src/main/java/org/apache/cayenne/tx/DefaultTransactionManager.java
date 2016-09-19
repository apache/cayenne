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
        return performInTransaction(op, DoNothingTransactionListener.getInstance());
    }

    @Override
    public <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback) {

        // Either join existing tx (in such case do not try to commit or rollback), or start a new tx and manage it
        // till the end

        Transaction currentTx = BaseTransaction.getThreadTransaction();
        return (currentTx != null)
                ? performInTransaction(currentTx, op, callback)
                : performInLocalTransaction(op, callback);
    }

    protected <T> T performInLocalTransaction(TransactionalOperation<T> op, TransactionListener callback) {
        Transaction tx = txFactory.createTransaction();
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
