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
package org.apache.cayenne.access.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.ResultIteratorIterator;

/**
 * A ResultIterator that does in-memory filtering of rows to return only
 * distinct rows. Distinct comparison is done by comparing ObjectIds created
 * from each row. Internally DistinctResultIterator wraps another ResultIterator
 * that provides the actual rows.
 * 
 * @since 3.0
 */
public class DistinctResultIterator<T> implements ResultIterator<T> {

    protected ResultIterator<T> delegate;
    protected Set<Map<String, Object>> fetchedIds;
    protected DataRow nextDataRow;
    protected DbEntity defaultEntity;
    protected boolean compareFullRows;

    /**
     * Creates new DistinctResultIterator wrapping another ResultIterator.
     * 
     * @param wrappedIterator
     * @param defaultEntity
     *            an entity needed to build ObjectIds for distinct comparison.
     */
    public DistinctResultIterator(ResultIterator<T> delegate, DbEntity defaultEntity, boolean compareFullRows) {
        if (delegate == null) {
            throw new NullPointerException("Null wrapped iterator.");
        }

        if (defaultEntity == null) {
            throw new NullPointerException("Null defaultEntity.");
        }

        this.delegate = delegate;
        this.defaultEntity = defaultEntity;
        this.fetchedIds = new HashSet<Map<String, Object>>();
        this.compareFullRows = compareFullRows;

        checkNextRow();
    }

    /**
     * @since 3.2
     */
    @Override
    public Iterator<T> iterator() {
        return new ResultIteratorIterator<T>(this);
    }

    /**
     * Closes underlying ResultIterator.
     */
    @Override
    public void close() {
        delegate.close();
    }

    /**
     * @since 3.0
     */
    @Override
    public List<T> allRows() {
        List<T> list = new ArrayList<T>();

        while (this.hasNextRow()) {
            list.add(nextRow());
        }
        return list;
    }

    @Override
    public boolean hasNextRow() {
        return nextDataRow != null;
    }

    @Override
    public T nextRow() {
        if (!hasNextRow()) {
            throw new NoSuchElementException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        // TODO: 
        @SuppressWarnings("unchecked")
        T row = (T) nextDataRow;
        checkNextRow();
        return row;
    }

    /**
     * @since 3.0
     */
    @Override
    public void skipRow() {
        if (!hasNextRow()) {
            throw new NoSuchElementException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        checkNextRow();
    }

    void checkNextRow() {

        if (this.compareFullRows) {
            checkNextUniqueRow();
        } else {
            checkNextRowWithUniqueId();
        }
    }

    void checkNextUniqueRow() {

        nextDataRow = null;
        while (delegate.hasNextRow()) {
            DataRow next = (DataRow) delegate.nextRow();

            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    void checkNextRowWithUniqueId() {

        nextDataRow = null;
        while (delegate.hasNextRow()) {
            DataRow next = (DataRow) delegate.nextRow();

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

}
