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
     * @since 3.0
     */
    public List<?> allRows() throws CayenneException {

        List<Object> list = new ArrayList<Object>();

        while (this.hasNextRow()) {
            list.add(this.nextRow());
        }

        return list;
    }

    public boolean hasNextRow() throws CayenneException {
        return nextRow;
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

    /**
     * @since 3.0
     */
    public void skipRow() throws CayenneException {
        wrappedIterator.skipRow();
    }
}
