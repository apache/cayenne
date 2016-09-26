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

import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DefaultTransactionManagerIT extends ServerCase {

    @Test
    public void testPerformInTransaction_Local() {

        final BaseTransaction tx = mock(BaseTransaction.class);
        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.createTransaction()).thenReturn(tx);

        DefaultTransactionManager txManager = new DefaultTransactionManager(txFactory, mock(JdbcEventLogger.class));

        final Object expectedResult = new Object();
        Object result = txManager.performInTransaction(new TransactionalOperation<Object>() {
            public Object perform() {
                assertNotNull(BaseTransaction.getThreadTransaction());
                return expectedResult;
            }
        });

        assertSame(expectedResult, result);
    }

    @Test
    public void testPerformInTransaction_ExistingTx() {

        final BaseTransaction tx1 = mock(BaseTransaction.class);
        TransactionFactory txFactory = mock(TransactionFactory.class);
        when(txFactory.createTransaction()).thenReturn(tx1);

        DefaultTransactionManager txManager = new DefaultTransactionManager(txFactory, mock(JdbcEventLogger.class));

        final BaseTransaction tx2 = mock(BaseTransaction.class);
        BaseTransaction.bindThreadTransaction(tx2);
        try {

            final Object expectedResult = new Object();
            Object result = txManager.performInTransaction(new TransactionalOperation<Object>() {
                public Object perform() {
                    assertSame(tx2, BaseTransaction.getThreadTransaction());
                    return expectedResult;
                }
            });

            assertSame(expectedResult, result);
        } finally {
            BaseTransaction.bindThreadTransaction(null);
        }
    }


}
