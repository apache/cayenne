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
 * @author Andrus Adamchik
 */
public class DistinctResultIterator implements ResultIterator {

    protected ResultIterator wrappedIterator;
    protected Set fetchedIds;
    protected Map nextDataRow;
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
        this.fetchedIds = new HashSet();
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
     */
    public List dataRows(boolean close) throws CayenneException {
        List list = new ArrayList();

        try {
            while (this.hasNextRow()) {
                list.add(this.nextDataRow());
            }
            return list;
        }
        finally {
            if (close) {
                this.close();
            }
        }
    }

    public int getDataRowWidth() {
        return wrappedIterator.getDataRowWidth();
    }

    public boolean hasNextRow() throws CayenneException {
        return nextDataRow != null;
    }

    public Map nextDataRow() throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Map row = nextDataRow;
        checkNextRow();
        return row;
    }

    /**
     * Returns a Map for the next ObjectId. After calling this method, calls to
     * "nextDataRow()" will result in exceptions.
     */
    public Map nextObjectId(DbEntity entity) throws CayenneException {
        if (!hasNextRow()) {
            throw new CayenneException(
                    "An attempt to read uninitialized row or past the end of the iterator.");
        }

        Map row = nextDataRow;

        // if we were previously reading full rows, we need to strip extra keys...
        if (!readingIds) {
            Iterator it = row.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String name = (String) entry.getKey();
                DbAttribute attribute = (DbAttribute) entity.getAttribute(name);
                if (attribute == null || !attribute.isPrimaryKey()) {
                    it.remove();
                }
            }
        }

        checkNextId(entity);
        return row;
    }

    public void skipDataRow() throws CayenneException {
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
            Map next = wrappedIterator.nextDataRow();

            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    void checkNextRowWithUniqueId() throws CayenneException {

        nextDataRow = null;
        while (wrappedIterator.hasNextRow()) {
            Map next = wrappedIterator.nextDataRow();

            // create id map...
            // TODO: this can be optimized by creating an array with id keys
            // to avoid iterating over default entity attributes...

            Map id = new HashMap();
            for (final DbAttribute pk : defaultEntity.getPrimaryKeys()) {
                id.put(pk.getName(), next.get(pk.getName()));
            }

            if (fetchedIds.add(id)) {
                this.nextDataRow = next;
                break;
            }
        }
    }

    void checkNextId(DbEntity entity) throws CayenneException {
        if (entity == null) {
            throw new CayenneException("Null DbEntity, can't create id.");
        }

        this.readingIds = true;
        this.nextDataRow = null;

        while (wrappedIterator.hasNextRow()) {
            Map next = wrappedIterator.nextObjectId(entity);

            // if we are reading ids, we ignore "compareFullRows" setting
            if (fetchedIds.add(next)) {
                this.nextDataRow = next;
                break;
            }
        }
    }
}
