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

/**
 * An optional utility service that simplifies wrapping multiple operations in
 * transactions. Users only rarely need to invoke it directly, as all standard
 * Cayenne operations are managing their own transactions internally.
 *
 * @since 4.0
 */
public interface TransactionManager {

    /**
     * Starts a new transaction (or joins an existing one) calling
     * {@link org.apache.cayenne.tx.TransactionalOperation#perform()}, and then committing or rolling back the
     * transaction.
     *
     * @param op an operation to perform within the transaction.
     * @param <T> returned value type
     * @return a value returned by the "op" operation.
     */
    <T> T performInTransaction(TransactionalOperation<T> op);

    /**
     * Starts a new transaction (or joins an existing one) calling
     * {@link org.apache.cayenne.tx.TransactionalOperation#perform()}, and then committing or rolling back the
     * transaction. As transaction goes through stages, callback methods are invoked allowing the caller to customize
     * transaction parameters.
     *
     * @param op       an operation to perform within the transaction.
     * @param callback a callback to notify as transaction progresses through stages.
     * @param <T> returned value type
     * @return a value returned by the "op" operation.
     */
    <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback);


    /**
     * Performs operation in a transaction which parameters described by descriptor.
     *
     * @param op         an operation to perform within the transaction.
     * @param descriptor transaction descriptor
     * @param <T> result type
     * @return a value returned by the "op" operation.
     *
     * @since 4.1
     */
    <T> T performInTransaction(TransactionalOperation<T> op, TransactionDescriptor descriptor);

    /**
     * Performs operation in a transaction which parameters described by descriptor.
     * As transaction goes through stages, callback methods are invoked allowing the caller to customize
     * transaction parameters.
     *
     * @param op         an operation to perform within the transaction.
     * @param callback   a callback to notify as transaction progresses through stages.
     * @param descriptor transaction descriptor
     * @param <T> returned value type
     * @return a value returned by the "op" operation.
     *
     * @since 4.1
     */
    <T> T performInTransaction(TransactionalOperation<T> op, TransactionListener callback, TransactionDescriptor descriptor);
}
