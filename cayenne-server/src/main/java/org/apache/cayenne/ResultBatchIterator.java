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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Defines API of a batch iterator over the ResultIterator returned as a result of
 * Select queries execution. Usually a ResultBatchIterator is supported by an open
 * java.sql.ResultSet, therefore ResultBatchIterator must be explicitly closed when
 * the user is done working with them.
 *
 * @since 4.0
 */
public class ResultBatchIterator<T> implements Iterable<List<T>>, Iterator<List<T>>, AutoCloseable {

    private final ResultIterator<T> delegate;
    private final int size;

    public ResultBatchIterator(ResultIterator<T> delegate, int size) {
        this.delegate = delegate;
        this.size = size;
    }

    @Override
    public Iterator<List<T>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return delegate.hasNextRow();
    }

    @Override
    public List<T> next() {
        List<T> objects = new ArrayList<>(size);

        for (int i = 0; i < size && hasNext(); i++) {
            objects.add((T) delegate.nextRow());
        }

        return objects;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported.");
    }

    @Override
    public void close() {
        delegate.close();
    }

    public int getBatchSize() {
        return size;
    }
}
