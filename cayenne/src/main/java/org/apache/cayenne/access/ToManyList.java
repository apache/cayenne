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

import org.apache.cayenne.Persistent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list that holds objects for to-many relationships, lazily resolved on first access. All operations, except for
 * resolving the list from DB, are not synchronized. The safest way to implement custom synchronization is to
 * synchronize on parent ObjectStore.
 */
public class ToManyList<E> extends ToManyHolder<E> implements List<E> {

    protected List<E> objectList;

    /**
     * Creates ToManyList.
     *
     * @since 1.1
     */
    public ToManyList(Persistent source, String relationship) {
        super(source, relationship);

        // if source is new, set object list right away
        if (isTransientParent()) {
            objectList = new ArrayList<>();
        }
    }

    @Override
    public boolean isFault() {
        if (objectList != null) {
            return false;
        }

        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        if (isTransientParent()) {
            objectList = new ArrayList<>();
            return false;
        }

        return true;
    }

    @Override
    public void invalidate() {
        objectList = null;
    }

    /**
     * Resolves the list with the given objects. A non-list collection is copied into a new list.
     */
    @Override
    public void resolveWith(Collection<E> objects) {
        objectList = objects instanceof List<E> list ? list : new ArrayList<>(objects);
    }

    /**
     * Returns internal objects list resolving it if needed.
     */
    protected List<E> resolvedObjectList() {
        if (isFault()) {
            synchronized (this) {
                // now that we obtained the lock, check if another thread just resolved the list
                if (isFault()) {
                    objectList = resolveRelationship();
                }
            }
        }

        return objectList;
    }

    // ====================================================
    // Standard List Methods.
    // ====================================================

    @Override
    public boolean add(E o) {
        if (isFault()) {
            addLocal(o);
            return true;
        }
        return objectList.add(o);
    }

    @Override
    public void add(int index, E o) {
        resolvedObjectList().add(index, o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return resolvedObjectList().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return resolvedObjectList().addAll(index, c);
    }

    @Override
    public void clear() {
        resolvedObjectList().clear();
    }

    @Override
    public boolean contains(Object o) {
        return resolvedObjectList().contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return resolvedObjectList().containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ToManyList<?> other)) {
            return false;
        }
        return resolvedObjectList().equals(other.resolvedObjectList());
    }

    @Override
    public int hashCode() {
        return 37 + resolvedObjectList().hashCode();
    }

    @Override
    public E get(int index) {
        return resolvedObjectList().get(index);
    }

    @Override
    public int indexOf(Object o) {
        return resolvedObjectList().indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return resolvedObjectList().isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return resolvedObjectList().iterator();
    }

    @Override
    public int lastIndexOf(Object o) {
        return resolvedObjectList().lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return resolvedObjectList().listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return resolvedObjectList().listIterator(index);
    }

    @Override
    public E remove(int index) {
        return resolvedObjectList().remove(index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        if (isFault()) {
            removeLocal((E) o);
            return true;
        }
        return objectList.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return resolvedObjectList().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return resolvedObjectList().retainAll(c);
    }

    @Override
    public E set(int index, E o) {
        return resolvedObjectList().set(index, o);
    }

    @Override
    public int size() {
        return resolvedObjectList().size();
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return resolvedObjectList().subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return resolvedObjectList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return resolvedObjectList().toArray(a);
    }

    @Override
    public String toString() {
        return objectList != null ? objectList.toString() : "[<unresolved>]";
    }
}
