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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A ResultIterator over a collection of objects.
 * 
 * @since 4.0
 */
class CollectionResultIterator<T> implements ResultIterator<T> {

    protected Iterator<T> iterator;

    public CollectionResultIterator(Collection<T> c) {
        this.iterator = c.iterator();
    }

    @Override
    public Iterator<T> iterator() {
        checkIterator();
        return iterator;
    }

    @Override
    public List<T> allRows() {

        List<T> list = new ArrayList<>();
        for (T t : this) {
            list.add(t);
        }

        return list;
    }

    @Override
    public boolean hasNextRow() {
        checkIterator();
        return iterator.hasNext();
    }

    @Override
    public T nextRow() {
        checkIterator();
        return iterator.next();
    }

    @Override
    public void skipRow() {
        checkIterator();
        iterator.next();
    }

    @Override
    public void close() {
        iterator = null;
    }

    protected void checkIterator() {
        if (iterator == null) {
            throw new IllegalStateException("Iterator is closed");
        }
    }

}
