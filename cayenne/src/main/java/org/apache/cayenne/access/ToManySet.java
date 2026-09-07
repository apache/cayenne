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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.cayenne.Persistent;

/**
 * A set that holds objects for to-many relationships, lazily resolved on first access.
 *
 * @since 3.0
 */
public class ToManySet<E> extends ToManyHolder<E> implements Set<E> {

    protected Set<E> objectSet;

    public ToManySet(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
    }

    // ====================================================
    // RelationshipCollection methods
    // ====================================================

    @Override
    public boolean isFault() {
        if (objectSet != null) {
            return false;
        }

        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        if (isTransientParent()) {
            objectSet = new HashSet<>();
            return false;
        }

        return true;
    }

    @Override
    public void invalidate() {
        objectSet = null;
    }

    /**
     * Resolves the set with the given objects. A non-set collection (e.g. a prefetch result) is copied into a new
     * set.
     */
    @Override
    public void resolveWith(Collection<E> objects) {
        objectSet = objects instanceof Set<E> set ? set : new HashSet<>(objects);
    }

    /**
     * Returns internal objects set resolving it if needed.
     */
    protected Set<E> resolvedObjectSet() {
        if (isFault()) {
            synchronized (this) {
                // now that we obtained the lock, check if another thread just resolved the set
                if (isFault()) {
                    objectSet = new HashSet<>(resolveFromDB());
                }
            }
        }

        return objectSet;
    }

    // ====================================================
    // Standard Set Methods.
    // ====================================================

    @Override
    public boolean add(E o) {
        if (isFault()) {
            addLocal(o);
            return true;
        }
        return objectSet.add(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return resolvedObjectSet().addAll(c);
    }

    @Override
    public void clear() {
        resolvedObjectSet().clear();
    }

    @Override
    public boolean contains(Object o) {
        return resolvedObjectSet().contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return resolvedObjectSet().containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ToManySet<?> other)) {
            return false;
        }
        return resolvedObjectSet().equals(other.resolvedObjectSet());
    }

    @Override
    public int hashCode() {
        return 53 + resolvedObjectSet().hashCode();
    }

    @Override
    public boolean isEmpty() {
        return resolvedObjectSet().isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return resolvedObjectSet().iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        if (isFault()) {
            removeLocal((E) o);
            return true;
        }
        return objectSet.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return resolvedObjectSet().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return resolvedObjectSet().retainAll(c);
    }

    @Override
    public int size() {
        return resolvedObjectSet().size();
    }

    @Override
    public Object[] toArray() {
        return resolvedObjectSet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return resolvedObjectSet().toArray(a);
    }

    @Override
    public String toString() {
        return objectSet != null ? objectSet.toString() : "[<unresolved>]";
    }
}
