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

        // join existing tx if it is in progress... in such case do not try to
        // commit or roll it back
        Transaction currentTx = BaseTransaction.getThreadTransaction();
        if (currentTx != null) {
            return op.perform();
        }

        // start a new tx and manage it till the end
        Transaction tx = txFactory.createTransaction();
        BaseTransaction.bindThreadTransaction(tx);
        try {

            T result = op.perform();

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

}
