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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ResultIterator;

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
        this.result = result;
        this.tx = tx;
    }

    public Iterator<T> iterator() {
        return result.iterator();
    }

    /**
     * Closes the result and commits the transaction.
     */
    public void close() {

        try {
            result.close();
            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Exception rollbackEx) {
            }

            throw new CayenneRuntimeException(e);
        } finally {
            if (Transaction.getThreadTransaction() == tx) {
                Transaction.bindThreadTransaction(null);
            }
        }
    }

    /**
     * @since 3.0
     */
    public List<T> allRows() {
        List<T> list = new ArrayList<T>();

        while (hasNextRow()) {
            list.add(nextRow());
        }

        return list;
    }

    public boolean hasNextRow() {
        return result.hasNextRow();
    }

    /**
     * @since 3.0
     */
    public T nextRow() {
        return result.nextRow();
    }

    /**
     * @since 3.0
     */
    public void skipRow() {
        result.skipRow();
    }
}
