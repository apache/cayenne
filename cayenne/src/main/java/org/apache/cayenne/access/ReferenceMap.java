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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Map of registered objects of a {@link DataContextObjectStore}, storing the values as weak or soft references and
 * resolving them as needed. Created with {@link #weakValues()} or {@link #softValues()}. Though this implementation
 * tries to follow general {@link Map} contract (including equals() and hashCode()) it is not intended for general
 * usage.
 * <p>
 * This map doesn't guarantee that value will be there even right after put(), as GC can remove it at any time.
 * <p>
 * The map is thread-safe. Its iterators are weakly consistent, and {@link #computeIfAbsent(Object, Function)} is
 * atomic, so it can serve as a registry shared by concurrent threads.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @since 4.1
 */
final class ReferenceMap<K, V> extends AbstractMap<K, V> {

    // Implementation notes:
    //  - internally data stored in a ConcurrentHashMap, so the map is thread-safe;
    //  - a reference carries its key, so a reference cleared by the GC is removed without scanning the map;
    //  - cleared references are tracked via a ReferenceQueue, and are expunged on modifications and on the
    //  operations that report or iterate the contents. Lookups do not touch the queue: a cleared reference reads as
    //  an absent value, and draining the queue takes its lock, which concurrent readers would contend for whenever
    //  the GC keeps the queue non-empty.

    /**
     * Returns a map that stores values as {@link WeakReference}s.
     */
    static <K, V> ReferenceMap<K, V> weakValues() {
        return new ReferenceMap<>(false);
    }

    /**
     * Returns a map that stores values as {@link SoftReference}s.
     */
    static <K, V> ReferenceMap<K, V> softValues() {
        return new ReferenceMap<>(true);
    }

    private final boolean softValues;
    private final ConcurrentMap<K, Reference<V>> map;
    private final ReferenceQueue<V> referenceQueue;

    // set while a thread drains the reference queue, so that others skip the drain instead of queueing on its lock
    private final AtomicBoolean expunging;

    // a view of the entries of "map" that resolves the references
    private final Set<Entry<K, V>> entrySet;

    private volatile Consumer<K> keyCleanupCallback;

    private ReferenceMap(boolean softValues) {
        this.softValues = softValues;
        this.map = new ConcurrentHashMap<>();
        this.referenceQueue = new ReferenceQueue<>();
        this.expunging = new AtomicBoolean();
        this.entrySet = new ReferenceEntrySet();
    }

    @Override
    public int size() {
        expungeStaleEntries();
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        expungeStaleEntries();
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        expungeStaleEntries();
        for (Reference<V> ref : map.values()) {
            V v = ref.get();
            if (v != null && v.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        Reference<V> ref = map.get(key);
        return ref != null ? ref.get() : null;
    }

    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException("ReferenceMap can't contain null values");
        }
        expungeStaleEntries();
        Reference<V> oldValue = map.put(key, newReference(key, value));
        return oldValue != null ? oldValue.get() : null;
    }

    /**
     * Atomically returns the value for the key, or, if there's none (or the value was cleared by the GC), computes
     * it with the function, stores and returns it. The function is called at most once, and is not called if a value
     * is present.
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        // a lock-free fast path for a present value
        Reference<V> ref = map.get(key);
        V present = ref != null ? ref.get() : null;
        if (present != null) {
            return present;
        }

        // a new value is likely to be stored, so this is the time to make room for it
        expungeStaleEntries();

        // the computed value is held strongly until it is returned, so the GC can't clear it in the meantime
        Object[] result = new Object[1];
        map.compute(key, (k, oldRef) -> {
            V existing = oldRef != null ? oldRef.get() : null;
            if (existing != null) {
                result[0] = existing;
                return oldRef;
            }

            V created = mappingFunction.apply(k);
            if (created == null) {
                // nothing is mapped, and a cleared reference, if any, is removed
                return null;
            }

            result[0] = created;
            return newReference(k, created);
        });

        @SuppressWarnings("unchecked")
        V value = (V) result[0];
        return value;
    }

    @Override
    public V remove(Object key) {
        expungeStaleEntries();
        Reference<V> oldValue = map.remove(key);
        return oldValue != null ? oldValue.get() : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        expungeStaleEntries();
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            if (entry.getValue() == null) {
                throw new NullPointerException("ReferenceMap can't contain null values");
            }
            map.put(entry.getKey(), newReference(entry.getKey(), entry.getValue()));
        }
    }

    @Override
    public void clear() {
        map.clear();
        resetReferenceQueue();
    }

    @Override
    public Set<K> keySet() {
        expungeStaleEntries();
        // should this check for cleared references? it can be invalid later anyway...
        return map.keySet();
    }

    @Override
    public Collection<V> values() {
        expungeStaleEntries();
        // this can be optimized by creating view instead of new heavyweight collection
        Collection<V> values = new ArrayList<>(map.size());
        for (Reference<V> ref : map.values()) {
            V value = ref.get();
            // check for null in case GC cleared some values after last queue check
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        expungeStaleEntries();
        return entrySet;
    }

    /**
     * Set callback that will be notified with a key on each value removal
     * due to the corresponding value reference cleaned up by the GC.
     *
     * @param keyCleanupCallback callback to set
     * @since 4.2.2
     */
    void setKeyCleanupCallback(Consumer<K> keyCleanupCallback) {
        this.keyCleanupCallback = keyCleanupCallback;
    }

    /**
     * Removes the entries whose values were collected by the GC so far.
     */
    private void expungeStaleEntries() {
        // one thread at a time is enough to keep up with the GC, and polling the queue takes a lock that concurrent
        // callers would otherwise contend for whenever the queue is not empty
        if (!expunging.compareAndSet(false, true)) {
            return;
        }

        try {
            Reference<? extends V> reference;
            while ((reference = referenceQueue.poll()) != null) {
                @SuppressWarnings("unchecked")
                K key = ((KeyedReference<K>) reference).key();

                // the key may have been remapped to a live value since, so remove the entry only if it still holds
                // the cleared reference
                if (map.remove(key, reference)) {
                    Consumer<K> callback = keyCleanupCallback;
                    if (callback != null) {
                        callback.accept(key);
                    }
                }
            }
        } finally {
            expunging.set(false);
        }
    }

    private void resetReferenceQueue() {
        //noinspection StatementWithEmptyBody
        while (referenceQueue.poll() != null) {
            // just purge this queue
        }
    }

    private Reference<V> newReference(K key, V value) {
        return softValues
                ? new KeyedSoftReference<>(key, value, referenceQueue)
                : new KeyedWeakReference<>(key, value, referenceQueue);
    }

    // a value reference that knows its key
    private interface KeyedReference<K> {
        K key();
    }

    private static final class KeyedWeakReference<K, V> extends WeakReference<V> implements KeyedReference<K> {

        private final K key;

        KeyedWeakReference(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        @Override
        public K key() {
            return key;
        }
    }

    private static final class KeyedSoftReference<K, V> extends SoftReference<V> implements KeyedReference<K> {

        private final K key;

        KeyedSoftReference(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }

        @Override
        public K key() {
            return key;
        }
    }

    // view over the entry set of "map"
    private class ReferenceEntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new ReferenceEntryIterator();
        }

        @Override
        public int size() {
            return map.size();
        }
    }

    // iterator used by entrySet, a wrapper around the "map" iterator. It fetches ahead to be sure we have a valid
    // value, or otherwise we can return a cleared reference
    private class ReferenceEntryIterator implements Iterator<Entry<K, V>> {

        final Iterator<Entry<K, Reference<V>>> internalIterator;

        Entry<K, V> next;

        // the internal iterator is already past the entry returned by next(), so its removal is done by key
        Entry<K, V> last;

        ReferenceEntryIterator() {
            internalIterator = map.entrySet().iterator();
            tryAdvance();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            last = next;
            tryAdvance();
            return last;
        }

        @Override
        public void remove() {
            if (last == null) {
                throw new IllegalStateException();
            }
            map.remove(last.getKey());
            last = null;
        }

        // moves the internal iterator ahead, to the first entry with a live value
        private void tryAdvance() {
            next = null;

            while (internalIterator.hasNext()) {
                Entry<K, Reference<V>> nextRefEntry = internalIterator.next();
                V value = nextRefEntry.getValue().get();
                if (value != null) {
                    next = new ReferenceEntry(nextRefEntry, value);
                    break;
                }
            }
        }
    }

    // view over a Map.Entry that transparently resolves the Reference
    private class ReferenceEntry extends SimpleEntry<K, V> {

        final Entry<K, Reference<V>> refEntry;

        ReferenceEntry(Entry<K, Reference<V>> refEntry, V value) {
            super(refEntry.getKey(), value);
            this.refEntry = refEntry;
        }

        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new NullPointerException("ReferenceMap can't contain null values");
            }
            Reference<V> oldRef = refEntry.setValue(newReference(getKey(), value));
            super.setValue(value);
            return oldRef != null ? oldRef.get() : null;
        }
    }
}
