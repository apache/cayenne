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

import java.sql.Connection;

/**
 * Defines callback methods for tracking and customizing Transactions execution.
 * 
 * @since 1.1
 */
public interface TransactionDelegate {

    /**
     * Called within a context of a Transaction before the transaction is committed.
     * Delegate can do its own processing, and optionally suppress further commit
     * processing by Cayenne by returning <code>false</code>.
     */
    public boolean willCommit(Transaction transaction);

    /**
     * Called within a context of a Transaction before transaction is marked as "rollback
     * only", meaning that further commit is not possible. Delegate can do its own
     * processing, and optionally suppress setting transaction status by returning
     * <code>false</code>.
     */
    public boolean willMarkAsRollbackOnly(Transaction transaction);

    /**
     * Called within a context of a Transaction before the transaction is rolledback.
     * Delegate can do its own processing, and optionally suppress further rollback
     * processing by Cayenne by returning <code>false</code>.
     */
    public boolean willRollback(Transaction transaction);

    /**
     * Called after a Transaction commit.
     */
    public void didCommit(Transaction transaction);

    /**
     * Called after a Transaction is rolledback.
     */
    public void didRollback(Transaction transaction);

    /**
     * Called within a context of a Transaction when a new JDBC onnection is added to the
     * the transaction. Delegate can do its own processing, and optionally suppress
     * connection registration with the transaction by returning <code>false</code>.
     */
    public boolean willAddConnection(Transaction transaction, Connection connection);
}
