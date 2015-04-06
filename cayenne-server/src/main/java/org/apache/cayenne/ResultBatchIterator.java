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

package org.apache.cayenne;

import java.io.Closeable;
import java.io.IOException;
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
public class ResultBatchIterator<T> implements Iterable<T>, Closeable {

    private ResultIterator delegate;
    private int size;

    public ResultBatchIterator(ResultIterator delegate, int size) {
        this.delegate = delegate;
        this.size = size;
    }

    /**
     * Returns the next batch of result rows, depending on the query and batch size, may be a
     * List of scalar values, DataRows, or Object[] arrays containing a mix of scalars and DataRows.
     *
     * @since 4.0
     */
    public List<T> nextBatch() {
        List<T> objects = new ArrayList<T>(size);
        int i = 0;

        while (i < size) {
            if (delegate.hasNextRow()) {
                objects.add((T) delegate.nextRow());
                i++;
            } else {
                break;
            }
        }

        return objects;
    };

    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public int getBatchSize() {
        return size;
    }

}
