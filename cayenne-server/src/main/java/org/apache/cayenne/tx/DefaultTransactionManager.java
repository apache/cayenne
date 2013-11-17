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
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.Transaction;
import org.apache.cayenne.di.Inject;

/**
 * @since 3.2
 */
public class DefaultTransactionManager implements TransactionManager {

    private DataDomain dataDomain;

    public DefaultTransactionManager(@Inject DataDomain dataDomain) {
        this.dataDomain = dataDomain;
    }

    public <T> T performInTransaction(TransactionalOperation<T> op) {

        // join existing tx if it is in progress... in such case do not try to
        // commit or roll it back
        Transaction currentTx = Transaction.getThreadTransaction();
        if (currentTx != null) {
            return op.perform();
        }

        // start a new tx and manage it till the end
        Transaction tx = dataDomain.createTransaction();
        Transaction.bindThreadTransaction(tx);
        try {

            T result = op.perform();

            tx.commit();

            return result;

        } catch (Exception ex) {
            tx.setRollbackOnly();
            throw new CayenneRuntimeException(ex);
        } finally {
            Transaction.bindThreadTransaction(null);

            if (tx.getStatus() == Transaction.STATUS_MARKED_ROLLEDBACK) {
                try {
                    tx.rollback();
                } catch (Exception e) {
                }
            }
        }
    }

}
