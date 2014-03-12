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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.util.ResultIteratorIterator;

/**
 * @since 3.0
 */
public class LimitResultIterator<T> implements ResultIterator<T> {

    protected ResultIterator<T> delegate;
    protected Map<String, Object> nextDataObjectIds;

    protected int fetchLimit;
    protected int offset;
    protected int fetchedSoFar;

    protected boolean nextRow;

    public LimitResultIterator(ResultIterator<T> delegate, int offset, int fetchLimit) {

        if (delegate == null) {
            throw new NullPointerException("Null delegate iterator.");
        }
        this.delegate = delegate;
        this.offset = offset;
        this.fetchLimit = fetchLimit;

        checkOffset();
        checkNextRow();

    }

    /**
     * @since 3.2
     */
    @Override
    public Iterator<T> iterator() {
        return new ResultIteratorIterator<T>(this);
    }

    private void checkOffset() {
        for (int i = 0; i < offset && delegate.hasNextRow(); i++) {
            delegate.nextRow();
        }
    }

    private void checkNextRow() {
        nextRow = false;

        if ((fetchLimit <= 0 || fetchedSoFar < fetchLimit) && this.delegate.hasNextRow()) {
            nextRow = true;
            fetchedSoFar++;
        }
    }

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

        while (hasNextRow()) {
            list.add(nextRow());
        }

        return list;
    }

    @Override
    public boolean hasNextRow() {
        return nextRow;
    }

    /**
     * @since 3.0
     */
    @Override
    public T nextRow() {
        if (!hasNextRow()) {
            throw new NoSuchElementException("An attempt to read uninitialized row or past the end of the iterator.");
        }

        T row = delegate.nextRow();
        checkNextRow();
        return row;
    }

    /**
     * @since 3.0
     */
    @Override
    public void skipRow() {
        delegate.skipRow();
    }
}
