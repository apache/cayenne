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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.map.DbEntity;

/**
 * Decorates ResultIterator to close active transaction when the iterator is closed.
 * 
 * @since 1.2
 */
final class TransactionResultIteratorDecorator implements ResultIterator {

    private ResultIterator result;
    private Transaction tx;

    public TransactionResultIteratorDecorator(ResultIterator result, Transaction tx) {
        this.result = result;
        this.tx = tx;
    }

    /**
     * Closes the result and commits the transaction.
     */
    public void close() throws CayenneException {

        try {
            result.close();
            tx.commit();
        }
        catch (Exception e) {
            try {
                tx.rollback();
            }
            catch (Exception rollbackEx) {
            }

            throw new CayenneException(e);
        }
        finally {
            if (Transaction.getThreadTransaction() == tx) {
                Transaction.bindThreadTransaction(null);
            }
        }
    }

    /**
     * @deprecated since 3.0 in favor of {@link #allRows(boolean)}.
     */
    public List dataRows(boolean close) throws CayenneException {
        return allRows(close);
    }

    /**
     * @since 3.0
     */
    public List allRows(boolean close) throws CayenneException {
        List list = new ArrayList<Object>();

        try {
            while (hasNextRow()) {
                list.add(nextRow());
            }
        }
        finally {
            if (close) {
                close();
            }
        }

        return list;
    }

    /**
     * @deprecated since 3.0
     */
    public int getDataRowWidth() {
        return result.getDataRowWidth();
    }
    
    /**
     * @since 3.0
     */
    public int getResultSetWidth() {
        return result.getResultSetWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return result.hasNextRow();
    }

    /**
     * @deprecated since 3.0 in favor of {@link #nextRow()}.
     */
    public Map<String, Object> nextDataRow() throws CayenneException {
        return result.nextDataRow();
    }

    /**
     * @since 3.0
     */
    public Object nextRow() throws CayenneException {
        return result.nextRow();
    }

    /**
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException {
        return result.nextObjectId(entity);
    }

    /**
     * @since 3.0
     */
    public Object nextId() throws CayenneException {
        return result.nextId();
    }

    /**
     * @deprecated since 3.0 in favor of {@link #skipRow()}.
     */
    public void skipDataRow() throws CayenneException {
        result.skipDataRow();
    }

    /**
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
        result.skipRow();
    }
}
