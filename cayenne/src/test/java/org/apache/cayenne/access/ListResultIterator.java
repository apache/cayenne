/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access;

import org.apache.cayenne.ResultIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A ResultIterator over an in-memory list of rows, that records whether it was closed.
 */
class ListResultIterator<T> implements ResultIterator<T> {

    private final List<T> rows;
    private int position;
    private boolean closed;

    @SafeVarargs
    static <T> ListResultIterator<T> of(T... rows) {
        return new ListResultIterator<>(List.of(rows));
    }

    ListResultIterator(List<T> rows) {
        this.rows = rows;
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public List<T> allRows() {
        List<T> remaining = new ArrayList<>(rows.subList(position, rows.size()));
        position = rows.size();
        return remaining;
    }

    @Override
    public boolean hasNextRow() {
        return position < rows.size();
    }

    @Override
    public T nextRow() {
        return rows.get(position++);
    }

    @Override
    public void skipRow() {
        position++;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public Iterator<T> iterator() {
        return rows.subList(position, rows.size()).iterator();
    }
}
