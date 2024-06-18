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

import java.sql.Connection;
import java.util.function.Supplier;

/**
 * Descriptor that allows to customize transaction logic.
 * It provides following options:
 * <ul>
 *  <li> transaction isolation level
 *  <li> transaction propagation logic.
 *  <li> custom connection to use in a transaction
 * </ul>
 * @see TransactionManager#performInTransaction(TransactionalOperation, TransactionDescriptor)
 * @see org.apache.cayenne.runtime.CayenneRuntime#performInTransaction(TransactionalOperation, TransactionDescriptor)
 * @since 4.1
 */
public class TransactionDescriptor {

    /**
     * Keep database default isolation level
     */
    public static final int ISOLATION_DEFAULT = -1;

    private static final TransactionDescriptor DEFAULT_DESCRIPTOR = builder()
            .propagation(TransactionPropagation.NESTED)
            .isolation(TransactionDescriptor.ISOLATION_DEFAULT)
            .build();

    private int isolation;

    private TransactionPropagation propagation;

    private Supplier<Connection> connectionSupplier;

    protected TransactionDescriptor() {
    }

    /**
     * @return required isolation level
     */
    public int getIsolation() {
        return isolation;
    }

    /**
     * @return required propagation behaviour
     */
    public TransactionPropagation getPropagation() {
        return propagation;
    }

    /**
     * @return custom connection supplier, passed by user
     * @since 4.2
     */
    public Supplier<Connection> getConnectionSupplier() {
        return connectionSupplier;
    }

    /**
     * @return TransactionDescriptor Builder
     * @since 4.2
     */
    public static Builder builder(){
        return new Builder();
    }

    /**
     * Returns descriptor with the {@link TransactionPropagation#NESTED} propagation
     * and the {@link #ISOLATION_DEFAULT} isolation level
     * @return default descriptor
     * @since 4.2
     */
    public static TransactionDescriptor defaultDescriptor() {
        return DEFAULT_DESCRIPTOR;
    }

    /**
     * Builder class for the TransactionDescriptor.
     * @since 4.2
     */
    public static class Builder {
        private final TransactionDescriptor transactionDescriptor = new TransactionDescriptor();

        private Builder(){
        }

        /**
         * @param isolation one of the following <code>Connection</code> constants:
         *                  <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
         *                  <code>Connection.TRANSACTION_READ_COMMITTED</code>,
         *                  <code>Connection.TRANSACTION_REPEATABLE_READ</code>,
         *                  <code>Connection.TRANSACTION_SERIALIZABLE</code>, or
         *                  <code>TransactionDescriptor.ISOLATION_DEFAULT</code>
         */
        public Builder isolation(int isolation) {
            transactionDescriptor.isolation = isolation;
            return this;
        }

        /**
         * A custom connection provided by the TransactionDescriptor will be used
         * instead of any other connection provided by tbe connection pool.
         *
         * @param connection custom connection
         * @see #connectionSupplier(Supplier)
         */
        public Builder connection(Connection connection) {
            transactionDescriptor.connectionSupplier = () -> connection;
            return this;
        }

        /**
         * A custom connection provided by the TransactionDescriptor will be used
         * instead of any other connection provided by tbe connection pool.
         *
         * @param connectionSupplier custom connection supplier
         * @see #connection(Connection)
         */
        public Builder connectionSupplier(Supplier<Connection> connectionSupplier){
            transactionDescriptor.connectionSupplier = connectionSupplier;
            return this;
        }

        /**
         * @param propagation transaction propagation behaviour
         * @see TransactionPropagation
         */
        public Builder propagation(TransactionPropagation propagation) {
            transactionDescriptor.propagation = propagation;
            return this;
        }

        public TransactionDescriptor build() {
            return transactionDescriptor;
        }
    }

}
