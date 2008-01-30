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
 * @author Andrus Adamchik
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
            if(Transaction.getThreadTransaction() == tx) {
                Transaction.bindThreadTransaction(null);
            }
        }
    }

    public List dataRows(boolean close) throws CayenneException {
        List<Map> list = new ArrayList<Map>();

        try {
            while (hasNextRow()) {
                list.add(nextDataRow());
            }
        }
        finally {
            if (close) {
                close();
            }
        }

        return list;
    }

    public int getDataRowWidth() {
        return result.getDataRowWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return result.hasNextRow();
    }

    public Map nextDataRow() throws CayenneException {
        return result.nextDataRow();
    }

    public Map nextObjectId(DbEntity entity) throws CayenneException {
        return result.nextObjectId(entity);
    }

    public void skipDataRow() throws CayenneException {
        result.skipDataRow();
    }
}
