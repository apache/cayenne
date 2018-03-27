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

/**
 *
 * Descriptor that provide desired transaction isolation level and propagation logic.
 *
 * @since 4.1
 */
public class TransactionDescriptor {

    /**
     * Keep database default isolation level
     */
    public static final int ISOLATION_DEFAULT = -1;

    private final int isolation;

    private final TransactionPropagation propagation;

    /**
     * @param isolation one of the following <code>Connection</code> constants:
     *        <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     *        <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     *        <code>Connection.TRANSACTION_REPEATABLE_READ</code>,
     *        <code>Connection.TRANSACTION_SERIALIZABLE</code>, or
     *        <code>TransactionDescriptor.ISOLATION_DEFAULT</code>
     *
     * @param propagation transaction propagation behaviour
     *
     * @see TransactionPropagation
     */
    public TransactionDescriptor(int isolation, TransactionPropagation propagation) {
        this.isolation = isolation;
        this.propagation = propagation;
    }

    /**
     *
     * Create transaction descriptor with desired isolation level and <code>NESTED</code> propagation
     *
     * @param isolation one of the following <code>Connection</code> constants:
     *        <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     *        <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     *        <code>Connection.TRANSACTION_REPEATABLE_READ</code>,
     *        <code>Connection.TRANSACTION_SERIALIZABLE</code>, or
     *        <code>TransactionDescriptor.ISOLATION_DEFAULT</code>
     */
    public TransactionDescriptor(int isolation) {
        this(isolation, TransactionPropagation.NESTED);
    }

    /**
     *
     * @param propagation transaction propagation behaviour
     * @see TransactionPropagation
     */
    public TransactionDescriptor(TransactionPropagation propagation) {
        this(ISOLATION_DEFAULT, propagation);
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
}
