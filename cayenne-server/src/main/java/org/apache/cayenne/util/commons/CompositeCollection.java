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

package org.apache.cayenne.util.commons;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Decorates a collection of other collections to provide a single unified view.
 * <p>
 * Changes made to this collection will actually be made on the decorated collection.
 * Add and remove operations require the use of a pluggable strategy. If no
 * strategy is provided then add and remove are unsupported.
 *
 * @author Brian McCallister
 * @author Stephen Colebourne
 * @author Phil Steitz
 *
 * @since 4.1
 *
 * NOTE: this is a simplified and type-safe version of CompositeCollection found in commons-collections v3.2.1
 */
public class CompositeCollection<E> implements Collection<E> {

    /** Collections in the composite */
    protected ArrayList<Collection<E>> all;

    /**
     * Create an empty CompositeCollection.
     */
    public CompositeCollection() {
        super();
        this.all = new ArrayList<>();
    }

    //-----------------------------------------------------------------------
    /**
     * Gets the size of this composite collection.
     * <p>
     * This implementation calls <code>size()</code> on each collection.
     *
     * @return total number of elements in all contained containers
     */
    @Override
    public int size() {
        int size = 0;
        for(Collection<E> collection : this.all) {
            size += collection.size();
        }
        return size;
    }

    /**
     * Checks whether this composite collection is empty.
     * <p>
     * This implementation calls <code>isEmpty()</code> on each collection.
     *
     * @return true if all of the contained collections are empty
     */
    @Override
    public boolean isEmpty() {
        for(Collection<E> collection : this.all) {
            if(!collection.isEmpty()){
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether this composite collection contains the object.
     * <p>
     * This implementation calls <code>contains()</code> on each collection.
     *
     * @param obj  the object to search for
     * @return true if obj is contained in any of the contained collections
     */
    @Override
    public boolean contains(Object obj) {
        for(Collection<E> collection : this.all) {
            if(!collection.contains(obj)){
                return true;
            }
        }
        return false;
    }

    /**
     * Gets an iterator over all the collections in this composite.
     * <p>
     * This implementation uses an <code>IteratorChain</code>.
     *
     * @return an <code>IteratorChain</code> instance which supports
     *  <code>remove()</code>. Iteration occurs over contained collections in
     *  the order they were added, but this behavior should not be relied upon.
     * @see IteratorChain
     */
    @Override
    public Iterator<E> iterator() {
        if (this.all.isEmpty()) {
            return Collections.emptyIterator();
        }
        IteratorChain<E> chain = new IteratorChain<>();
        for (Collection<E> collection : this.all) {
            chain.addIterator(collection.iterator());
        }
        return chain;
    }

    /**
     * Returns an array containing all of the elements in this composite.
     *
     * @return an object array of all the elements in the collection
     */
    @Override
    public Object[] toArray() {
        final Object[] result = new Object[this.size()];
        int i = 0;
        for (Collection<E> collection : this.all) {
            for(E o : collection) {
                result[i++] = o;
            }
        }
        return result;
    }

    /**
     * Returns an object array, populating the supplied array if possible.
     * See <code>Collection</code> interface for full details.
     *
     * @param array  the array to use, populating if possible
     * @return an array of all the elements in the collection
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] array) {
        int size = this.size();
        Object[] result;
        if (array.length >= size) {
            result = array;
        } else {
            result = (T[]) Array.newInstance(array.getClass().getComponentType(), size);
        }

        int offset = 0;
        for (Collection<E> collection : this.all) {
            for (E e : collection) {
                result[offset++] = e;
            }
        }
        if (result.length > size) {
            result[size] = null;
        }
        return (T[])result;
    }

    /**
     * Adds an object to the collection, throwing UnsupportedOperationException
     * unless a CollectionMutator strategy is specified.
     *
     * @param obj  the object to add
     * @return true if the collection was modified
     * @throws UnsupportedOperationException if CollectionMutator hasn't been set
     * @throws UnsupportedOperationException if add is unsupported
     * @throws ClassCastException if the object cannot be added due to its type
     * @throws NullPointerException if the object cannot be added because its null
     * @throws IllegalArgumentException if the object cannot be added
     */
    @Override
    public boolean add(Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes an object from the collection, throwing UnsupportedOperationException
     * unless a CollectionMutator strategy is specified.
     *
     * @param obj  the object being removed
     * @return true if the collection is changed
     * @throws UnsupportedOperationException if removed is unsupported
     * @throws ClassCastException if the object cannot be removed due to its type
     * @throws NullPointerException if the object cannot be removed because its null
     * @throws IllegalArgumentException if the object cannot be removed
     */
    @Override
    public boolean remove(Object obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether this composite contains all the elements in the specified collection.
     * <p>
     * This implementation calls <code>contains()</code> for each element in the
     * specified collection.
     *
     * @param coll  the collection to check for
     * @return true if all elements contained
     */
    @Override
    public boolean containsAll(Collection<?> coll) {
        for (Object o : coll) {
            if (!this.contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds a collection of elements to this collection, throwing
     * UnsupportedOperationException unless a CollectionMutator strategy is specified.
     *
     * @param coll  the collection to add
     * @return true if the collection was modified
     * @throws UnsupportedOperationException if CollectionMutator hasn't been set
     * @throws UnsupportedOperationException if add is unsupported
     * @throws ClassCastException if the object cannot be added due to its type
     * @throws NullPointerException if the object cannot be added because its null
     * @throws IllegalArgumentException if the object cannot be added
     */
    @Override
    public boolean addAll(Collection<? extends E> coll) {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes the elements in the specified collection from this composite collection.
     * <p>
     * This implementation calls <code>removeAll</code> on each collection.
     *
     * @param coll  the collection to remove
     * @return true if the collection was modified
     * @throws UnsupportedOperationException if removeAll is unsupported
     */
    @Override
    public boolean removeAll(Collection<?> coll) {
        if (coll.size() == 0) {
            return false;
        }
        boolean changed = false;
        for(Collection<E> collection : this.all) {
            changed = (collection.removeAll(coll) || changed);
        }
        return changed;
    }

    /**
     * Retains all the elements in the specified collection in this composite collection,
     * removing all others.
     * <p>
     * This implementation calls <code>retainAll()</code> on each collection.
     *
     * @param coll  the collection to remove
     * @return true if the collection was modified
     * @throws UnsupportedOperationException if retainAll is unsupported
     */
    @Override
    public boolean retainAll(final Collection<?> coll) {
        boolean changed = false;
        for(Collection<E> collection : this.all) {
            changed = (collection.retainAll(coll) || changed);
        }
        return changed;
    }

    /**
     * Removes all of the elements from this collection .
     * <p>
     * This implementation calls <code>clear()</code> on each collection.
     *
     * @throws UnsupportedOperationException if clear is unsupported
     */
    @Override
    public void clear() {
        for (Collection<E> collection : this.all) {
            collection.clear();
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Add an additional collection to this composite.
     *
     * @param c  the collection to add
     */
    public void addComposited(Collection<E> c) {
        all.add(c);
    }

    /**
     * Removes a collection from the those being decorated in this composite.
     *
     * @param coll  collection to be removed
     */
    public void removeComposited(Collection<E> coll) {
        all.remove(coll);
    }

    /**
     * Gets the collections being decorated.
     *
     * @return Unmodifiable collection of all collections in this composite.
     */
    public Collection<Collection<E>> getCollections() {
        return Collections.unmodifiableCollection(this.all);
    }

}
