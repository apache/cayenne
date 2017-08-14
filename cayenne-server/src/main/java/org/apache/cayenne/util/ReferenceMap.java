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

package org.apache.cayenne.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Map that transparently stores values as references and resolves them as needed.
 * Though this implementation try to follow general {@link Map} contract (including equals() and hashCode())
 * it is not intended for general usage.
 * <p>
 * There is no HardReferenceMap as simple HashMap can be used for that.
 * <p>
 * This map doesn't guaranties that value will be there even right after put(), as GC can remove it at any time.
 * <p>
 * This implementation supports proper serialization, concrete classes should use {@link #writeObjectInternal(ObjectOutputStream)}
 * and {@link #readObjectInternal(ObjectInputStream)} methods to support it too.
 * <p>
 *
 * @param <K> key type
 * @param <V> value type
 * @param <R> reference type that will be used to store values
 *
 * @see WeakValueMap implementation that uses WeakReference to store values
 * @see SoftValueMap implementation that uses SoftReference to store values
 *
 * @since 4.1
 */
abstract class ReferenceMap<K, V, R extends Reference<V>> extends AbstractMap<K, V> implements Serializable {

    /*
     * Implementation notes:
     *  - internally data stored in HashMap thus this class and all implementations are not thread safe;
     *  - to track references that were cleared ReferenceQueue is used;
     *  - this map stores not only direct key => ref map but also a reverse ref => key to be able
     *  effectively clear data that was removed by GC;
     *  - this map is abstract, all that required for the concrete implementation is
     *  to define newReference(Object) method;
     *  - all accessors/modifiers should call checkReferenceQueue() to clear all stale data
     */

    private static final long serialVersionUID = -3365744592038165092L;

    /**
     * This is a main data storage used for most operations
     */
    protected transient Map<K, R> map;

    protected transient ReferenceQueue<V> referenceQueue;

    /**
     * This is a lazily created set of entries that is essentially a view to actual data
     */
    protected transient Set<Entry<K, V>> entrySet;

    public ReferenceMap() {
        map = new HashMap<>();
        referenceQueue = new ReferenceQueue<>();
    }

    public ReferenceMap(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
        referenceQueue = new ReferenceQueue<>();
    }

    public ReferenceMap(Map<? extends K, ? extends V> m) {
        this(m.size());
        putAll(m);
    }

    @Override
    public int size() {
        checkReferenceQueue();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        checkReferenceQueue();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        checkReferenceQueue();
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        checkReferenceQueue();
        for(R ref : map.values()) {
            if(ref != null) {
                V v = ref.get();
                if(v != null) {
                    if(v.equals(value)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        checkReferenceQueue();
        R ref = map.get(key);
        if(ref == null) {
            return null;
        }
        return ref.get();
    }

    @Override
    public V put(K key, V value) {
        checkReferenceQueue();
        R refValue = newReference(value);
        R oldValue = map.put(key, refValue);
        if(oldValue == null) {
            return null;
        }
        return oldValue.get();
    }

    @Override
    public V remove(Object key) {
        checkReferenceQueue();
        R oldValue = map.remove(key);
        if(oldValue == null) {
            return null;
        }
        return oldValue.get();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkReferenceQueue();
        for(Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            R value = newReference(entry.getValue());
            map.put(entry.getKey(), value);
        }
    }

    @Override
    public void clear() {
        map.clear();
        resetReferenceQueue();
    }

    @Override
    public Set<K> keySet() {
        checkReferenceQueue();
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        checkReferenceQueue();
        // this can be optimized by creating view instead of new heavyweight collection
        Collection<R> referenceValues = map.values();
        Collection<V> values = new ArrayList<>(referenceValues.size());
        for(R v : referenceValues) {
            if(v != null) {
                values.add(v.get());
            }
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        checkReferenceQueue();
        // lazily create entry set view
        Set<Entry<K, V>> es = entrySet;
        if(es == null) {
            entrySet = es = new ReferenceEntrySet();
        }
        return es;
    }

    /**
     * Cleanup all references collected by GC so far
     */
    protected void checkReferenceQueue() {
        Collection<Reference<? extends V>> valuesToRemove = null;
        Reference<? extends V> reference;
        while((reference = referenceQueue.poll()) != null) {
            if(valuesToRemove == null) {
                valuesToRemove = new HashSet<>();
            }
            valuesToRemove.add(reference);
        }

        if(valuesToRemove == null) {
            return;
        }

        Collection<K> keysToRemove = new ArrayList<>(valuesToRemove.size());
        for(Map.Entry<K, R> entry : map.entrySet()) {
            if(valuesToRemove.contains(entry.getValue())) {
                keysToRemove.add(entry.getKey());
            }
        }

        for(K keyToRemove : keysToRemove) {
            map.remove(keyToRemove);
        }
    }

    private void resetReferenceQueue() {
        while(referenceQueue.poll() != null) {
            // just purge this queue
        }
    }

    /**
     * This method should be implemented by concrete implementations of this abstract class.
     *
     * @param value to be wrapped into reference
     * @return new reference to the value
     */
    abstract R newReference(V value);

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeObjectInternal(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readObjectInternal(in);
    }

    protected void writeObjectInternal(ObjectOutputStream out) throws IOException {
        checkReferenceQueue();
        Map<K, V> replacementMap = new HashMap<>();
        for(Entry<K, R> entry : map.entrySet()) {
            if(entry.getValue() != null) {
                replacementMap.put(entry.getKey(), entry.getValue().get());
            }
        }
        out.writeObject(replacementMap);
    }

    protected void readObjectInternal(ObjectInputStream in) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<K, V> replacement = (Map<K, V>) in.readObject();
        map = new HashMap<>(replacement.size());
        referenceQueue = new ReferenceQueue<>();
        putAll(replacement);
    }

    /**
     * View over {@link #map} entry set
     */
    class ReferenceEntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new ReferenceEntryIterator();
        }

        @Override
        public int size() {
            return map.size();
        }
    }

    /**
     * Iterator used by entrySet. Wrapper around {@link #map} iterator
     */
    class ReferenceEntryIterator implements Iterator<Entry<K, V>> {

        Iterator<Entry<K, R>> internalIterator;

        ReferenceEntryIterator() {
            internalIterator = map.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return internalIterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            return new ReferenceEntry(internalIterator.next());
        }
    }

    /**
     * View over {@link Map.Entry} that transparently resolves Reference
     */
    class ReferenceEntry extends SimpleEntry<K, V> {

        private static final long serialVersionUID = -1795136249842496011L;

        Entry<K, R> refEntry;

        public ReferenceEntry(Entry<K, R> refEntry) {
            super(refEntry.getKey(), refEntry.getValue() != null ? refEntry.getValue().get() : null);
            this.refEntry = refEntry;
        }

        @Override
        public V setValue(V value) {
            R newRef = newReference(value);
            R oldRef = refEntry.setValue(newRef);
            if(oldRef != null) {
                return oldRef.get();
            }
            return null;
        }
    }
}
