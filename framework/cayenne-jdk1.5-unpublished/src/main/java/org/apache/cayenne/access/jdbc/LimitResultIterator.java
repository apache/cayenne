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
            wrappedIterator.nextDataRow();
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

    protected Map readDataRow() throws CayenneException {
        Map<String, Object> next = wrappedIterator.nextDataRow();
        return next;
    }

    public void close() throws CayenneException {
        wrappedIterator.close();
    }

    public List dataRows(boolean close) throws CayenneException {
        List<Map> list = new ArrayList<Map>();

        try {
            while (this.hasNextRow()) {
                list.add(this.nextDataRow());
            }
            return list;
        }
        finally {
            if (close) {
                close();
            }
        }
    }

    public int getDataRowWidth() {
        return wrappedIterator.getDataRowWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return nextRow;
    }

    public Map nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }
        Map<String, Object> row = readDataRow();
        checkNextRow();

        return row;
    }

    public Object nextId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Object id = readId(entity);
        checkNextId(entity);
        return id;

    }

    public Map<String, Object> nextObjectId(DbEntity entity) throws CayenneException {

        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        checkNextObjectId(entity);
        return nextDataObjectIds;
    }

    public void skipDataRow() throws CayenneException {
        wrappedIterator.skipDataRow();
    }

    void checkNextId(DbEntity entity) throws CayenneException {
        if (entity == null) {
            throw new CayenneException("Null DbEntity, can't create id.");
        }
        nextRow = false;
        if (wrappedIterator.hasNextRow()) {
            nextRow = true;
        }
    }

    public Object readId(DbEntity entity) throws CayenneException {

        Object next = wrappedIterator.nextId(entity);
        return next;
    }

    void checkNextObjectId(DbEntity entity) throws CayenneException {
        if (entity == null) {
            throw new CayenneException("Null DbEntity, can't create id.");
        }

        nextRow = false;
        nextDataObjectIds = null;
        if (wrappedIterator.hasNextRow()) {
            nextRow = true;
            Map<String, Object> next = wrappedIterator.nextObjectId(entity);
            nextDataObjectIds = next;
        }
    }

}
