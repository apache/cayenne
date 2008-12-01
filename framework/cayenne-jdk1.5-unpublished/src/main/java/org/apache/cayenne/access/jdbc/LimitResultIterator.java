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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.ResultIterator;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 3.0
 */
public class LimitResultIterator implements ResultIterator {

    protected ResultIterator wrappedIterator;
    protected Map<String, Object> nextDataObjectIds;

    protected int fetchLimit;
    protected int offset;
    protected int fetchedSoFar;

    protected boolean nextRow;

    public LimitResultIterator(ResultIterator wrappedIterator, int offset, int fetchLimit)
            throws CayenneException {

        if (wrappedIterator == null) {
            throw new CayenneException("Null wrapped iterator.");
        }
        this.wrappedIterator = wrappedIterator;
        this.offset = offset;
        this.fetchLimit = fetchLimit;

        checkOffset();
        checkNextRow();

    }

    private void checkOffset() throws CayenneException {
        for (int i = 0; i < offset && wrappedIterator.hasNextRow(); i++) {
            wrappedIterator.nextRow();
        }
    }

    private void checkNextRow() throws CayenneException {
        nextRow = false;

        if ((fetchLimit <= 0 || fetchedSoFar < fetchLimit)
                && this.wrappedIterator.hasNextRow()) {
            nextRow = true;
            fetchedSoFar++;
        }
    }

    public void close() throws CayenneException {
        wrappedIterator.close();
    }

    /**
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
                list.add(this.nextRow());
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
        return wrappedIterator.getDataRowWidth();
    }
    
    /**
     * @since 3.0
     */
    public int getResultSetWidth() {
        return wrappedIterator.getResultSetWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return nextRow;
    }

    /**
     * @deprecated since 3.0
     */
    public Map<String, Object> nextDataRow() throws CayenneException {
        return (DataRow) nextRow();
    }

    /**
     * @since 3.0
     */
    public Object nextRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Object row = wrappedIterator.nextRow();
        checkNextRow();
        return row;
    }

    public Object nextId() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Object id = readId();
        checkNextId();
        return id;

    }

    /**
     * @deprecated since 3.0 in favor of {@link #nextId(DbEntity)}.
     */
    public Map<String, Object> nextObjectId(DbEntity entity) throws CayenneException {

        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        checkNextObjectId(entity);
        return nextDataObjectIds;
    }

    /**
     * @deprecated since 3.0 in favor of {@link #skipRow()}.
     */
    public void skipDataRow() throws CayenneException {
        wrappedIterator.skipDataRow();
    }

    /**
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
        wrappedIterator.skipRow();
    }

    void checkNextId() throws CayenneException {
        nextRow = false;
        if (wrappedIterator.hasNextRow()) {
            nextRow = true;
        }
    }

    private Object readId() throws CayenneException {
        Object next = wrappedIterator.nextId();
        return next;
    }

    /**
     * @deprecated since 3.0
     */
    void checkNextObjectId(DbEntity entity) throws CayenneException {

        nextRow = false;
        nextDataObjectIds = null;
        if (wrappedIterator.hasNextRow()) {
            nextRow = true;
            Map<String, Object> next = wrappedIterator.nextObjectId(entity);
            nextDataObjectIds = next;
        }
    }
}
