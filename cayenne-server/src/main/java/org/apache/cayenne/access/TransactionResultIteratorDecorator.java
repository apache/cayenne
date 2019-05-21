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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.tx.Transaction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Decorates ResultIterator to close active transaction when the iterator is
 * closed.
 * 
 * @since 1.2
 */
final class TransactionResultIteratorDecorator<T> implements ResultIterator<T> {

    private ResultIterator<T> result;
    private Transaction tx;

    public TransactionResultIteratorDecorator(ResultIterator<T> result, Transaction tx) {

        // make sure it is still valid before proceeding with the iterator
        if(tx.isRollbackOnly()) {
            throw new CayenneRuntimeException("Transaction passed should be rolled back");
        }

        this.result = result;
        this.tx = tx;
    }

    @Override
    public Iterator<T> iterator() {
        return result.iterator();
    }

    /**
     * Closes the result and commits the transaction.
     */
    @Override
    public void close() {

        try {
            result.close();

            // we can safely commit here as the transaction is internal to this decorator, and we already checked
            // that it hasn't been rolled back in constructor.
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Exception rollbackEx) {
            }

            throw new CayenneRuntimeException(e);
        }
    }

    /**
     * @since 3.0
     */
    @Override
    public List<T> allRows() {
        List<T> list = new ArrayList<>();

        while (hasNextRow()) {
            list.add(nextRow());
        }

        return list;
    }

    @Override
    public boolean hasNextRow() {
        return result.hasNextRow();
    }

    /**
     * @since 3.0
     */
    @Override
    public T nextRow() {
        return result.nextRow();
    }

    /**
     * @since 3.0
     */
    @Override
    public void skipRow() {
        result.skipRow();
    }
}
