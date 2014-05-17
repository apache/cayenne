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
package org.apache.cayenne.access;

import org.apache.cayenne.tx.BaseTransaction;

/**
 * @deprecated since 3.2 use {@link BaseTransaction} static methods. Note that
 *             since 3.2 {@link org.apache.cayenne.tx.Transaction} is an
 *             interface located in a different package.
 */
@Deprecated
public abstract class Transaction {

    /**
     * Binds a Transaction to the current thread.
     * 
     * @since 1.2
     */
    @Deprecated
    public static void bindThreadTransaction(org.apache.cayenne.tx.Transaction transaction) {
        BaseTransaction.bindThreadTransaction(transaction);
    }

    /**
     * Returns a Transaction associated with the current thread, or null if
     * there is no such Transaction.
     * 
     * @since 1.2
     */
    @Deprecated
    public static org.apache.cayenne.tx.Transaction getThreadTransaction() {
        return BaseTransaction.getThreadTransaction();
    }

}
