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

package org.apache.cayenne.access.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A ResultIterator that does in-memory filtering of rows to return only distinct rows.
 * Distinct comparison is done by comparing ObjectIds created from each row. Internally
 * DistinctResultIterator wraps another ResultIterator that provides the actual rows. The
 * current limitation is that once switched to reading ids instead of rows (i.e. when
 * "nextObjectId()" is called for the first time), it can't be used to read data rows
 * again. This is pretty sensible for most things in Cayenne.
 * 
 * @since 1.2
 */
public class DistinctResultIterator implements ResultIterator {

    protected ResultIterator wrappedIterator;
    protected Set<Map<String, Object>> fetchedIds;
    protected Object nextDataRow;
    protected DbEntity defaultEntity;
    protected boolean compareFullRows;

    protected boolean readingIds;

    /**
     * Creates new DistinctResultIterator wrapping another ResultIterator.
     * 
     * @param wrappedIterator
     * @param defaultEntity an entity needed to build ObjectIds for distinct comparison.
     */
    public DistinctResultIterator(ResultIterator wrappedIterator, DbEntity defaultEntity,
            boolean compareFullRows) throws CayenneException {
        if (wrappedIterator == null) {
            throw new CayenneException("Null wrapped iterator.");
        }

        if (defaultEntity == null) {
            throw new CayenneException("Null defaultEntity.");
        }

        this.wrappedIterator = wrappedIterator;
        this.defaultEntity = defaultEntity;
        this.fetchedIds = new HashSet<Map<String, Object>>();
        this.compareFullRows = compareFullRows;

        checkNextRow();
    }

    /**
     * CLoses underlying ResultIterator.
     */
    public void close() throws CayenneException {
        wrappedIterator.close();
    }

    /**
     * Returns all data rows.
     * 
     * @deprecated since 3.0
     */
    public List dataRows(boolean close) throws CayenneException {
        return allRows(close);
    }

    /**
     * @since 3.0
     */
    public List allRows(boolean close) throws CayenneException {
        List<Object> list = new ArrayList<Object>();

        try {
            while (this.hasNextRow()) {
                list.add(nextRow());
            }
            return list;
        }
        finally {
            if (close) {
                this.close();
            }
        }
    }

    /**
     * @deprecated since 3.0
     */
    public int getDataRowWidth() {
        return wrappedIterator.getDataRowWidth();
    }
    
    public int getResultSetWidth() {
        return wrappedIterator.getResultSetWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return nextDataRow != null;
    }

    /**
     * @deprecated since 3.0
     */
    public Map nextDataRow() throws CayenneException {
        return (DataRow) nextRow();
    }

    public Object nextRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Object row = nextDataRow;
        checkNextRow();
        return row;
    }

    /**
     * Returns a Map for the next ObjectId. After calling this method, calls to
     * "nextDataRow()" will result in exceptions.
     * 
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        DataRow row = (DataRow) nextDataRow;

        // if we were previously reading full rows, we need to strip extra keys...
        if (!readingIds) {
            Iterator<Map.Entry<String, Object>> it = row.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                String name = entry.getKey();
                DbAttribute attribute = (DbAttribute) entity.getAttribute(name);
                if (attribute == null || !attribute.isPrimaryKey()) {
                    it.remove();
                }
            }
        }

        checkNextId(entity);
        return row;
    }

    /**
     * @since 3.0
     */
    public Object nextId() throws CayenneException {

        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        if (!(nextDataRow instanceof DataRow)) {
            throw new IllegalStateException(
                    "A query is not fetching DataRows. Can't call 'nextId'");
        }

        DataRow row = (DataRow) nextDataRow;

        checkNextId(defaultEntity);

        // TODO: andrus 3/6/2008: not very efficient ... a better algorithm would've
        // relied on wrapped iterator 'nextId' method.
        return row.get(defaultEntity.getPrimaryKeys().iterator().next().getName());
    }

    /**
     * @deprecated since 3.0
     */
    public void skipDataRow() throws CayenneException {
        skipRow();
    }

    /**
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        if (readingIds) {
            checkNextId(defaultEntity);
        }
        else {
            checkNextRow();
        }
    }

    void checkNextRow() throws CayenneException {
        if (readingIds) {
            throw new CayenneException(
                    "Can't go back from reading ObjectIds to reading rows.");
        }

        if (this.compareFullRows) {
            checkNextUniqueRow();
        }
        else {
            checkNextRowWithUniqueId();
        }
    }

    void checkNextUniqueRow() throws CayenneException {

        nextDataRow = null;
        while (wrappedIterator.hasNextRow()) {
            DataRow next = (DataRow) wrappedIterator.nextRow();

            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    void checkNextRowWithUniqueId() throws CayenneException {

        nextDataRow = null;
        while (wrappedIterator.hasNextRow()) {
            DataRow next = (DataRow) wrappedIterator.nextRow();

            // create id map...
            // TODO: this can be optimized by creating an array with id keys
            // to avoid iterating over default entity attributes...

            Map<String, Object> id = new HashMap<String, Object>();
            for (final DbAttribute pk : defaultEntity.getPrimaryKeys()) {
                id.put(pk.getName(), next.get(pk.getName()));
            }

            if (fetchedIds.add(id)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    /**
     * @deprecated since 3.0
     */
    void checkNextId(DbEntity entity) throws CayenneException {
        if (entity == null) {
            throw new CayenneException("Null DbEntity, can't create id.");
        }

        this.readingIds = true;
        this.nextDataRow = null;

        while (wrappedIterator.hasNextRow()) {
            Map<String, Object> next = wrappedIterator.nextObjectId(entity);

            // if we are reading ids, we ignore "compareFullRows" setting
            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }
}
