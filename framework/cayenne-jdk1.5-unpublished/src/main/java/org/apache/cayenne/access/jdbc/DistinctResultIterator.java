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
 * DistinctResultIterator wraps another ResultIterator that provides the actual rows.
 * 
 * @since 3.0
 */
public class DistinctResultIterator implements ResultIterator {

    protected ResultIterator wrappedIterator;
    protected Set<Map<String, Object>> fetchedIds;
    protected Object nextDataRow;
    protected DbEntity defaultEntity;
    protected boolean compareFullRows;

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
     * @since 3.0
     */
    public List<?> allRows() throws CayenneException {
        List<Object> list = new ArrayList<Object>();

        while (this.hasNextRow()) {
            list.add(nextRow());
        }
        return list;
    }

    public boolean hasNextRow() throws CayenneException {
        return nextDataRow != null;
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
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        checkNextRow();
    }

    void checkNextRow() throws CayenneException {

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

}
