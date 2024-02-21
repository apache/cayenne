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

import org.apache.cayenne.exp.path.CayennePath;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A java.util.List wrapper that stores objects prefetched together with the main list.
 * 
 * @since 1.2
 */
// TODO, andrus, 4/11/2006 - this object doesn't have to be a list. It is just a question
// of changing DataRowStore result caching API. Since we are doing it when already in 1.2
// beta, I am choosing the least invasive way that doesn't affect public API.
//
// Future alternatives may include caching the entire QueryResponse... or maybe leaving
// everything the way it is.
class ListWithPrefetches implements List<Object>, Serializable {

    private final List<Object> list;
    private final Map<CayennePath, List<?>> prefetchResultsByPath;

    @SuppressWarnings("unchecked")
    ListWithPrefetches(List<?> mainList, Map<CayennePath, List<?>> prefetchResultsByPath) {
        if (mainList == null) {
            throw new IllegalArgumentException("Main list is null");
        }

        this.list = (List<Object>)mainList;
        this.prefetchResultsByPath = prefetchResultsByPath != null
                ? Collections.unmodifiableMap(prefetchResultsByPath)
                : null;
    }

    Map<CayennePath, List<?>> getPrefetchResultsByPath() {
        return prefetchResultsByPath;
    }

    public void add(int index, Object element) {
        list.add(index, element);
    }

    public boolean add(Object o) {
        return list.add(o);
    }

    public boolean addAll(Collection c) {
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection c) {
        return list.addAll(index, c);
    }

    public void clear() {
        list.clear();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public boolean containsAll(Collection c) {
        return list.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return list.equals(o);
    }

    public Object get(int index) {
        return list.get(index);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }

    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public Iterator<Object> iterator() {
        return list.iterator();
    }

    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    public ListIterator<Object> listIterator() {
        return list.listIterator();
    }

    public ListIterator<Object> listIterator(int index) {
        return list.listIterator(index);
    }

    public Object remove(int index) {
        return list.remove(index);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean removeAll(Collection c) {
        return list.removeAll(c);
    }

    public boolean retainAll(Collection c) {
        return list.retainAll(c);
    }

    public Object set(int index, Object element) {
        return list.set(index, element);
    }

    public int size() {
        return list.size();
    }

    public List<Object> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }
}
