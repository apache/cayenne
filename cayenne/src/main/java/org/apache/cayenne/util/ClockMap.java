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
package org.apache.cayenne.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/**
 * A bounded concurrent map that evicts the entries least recently read. It is a {@link ConcurrentHashMap} with the
 * CLOCK ("second chance") replacement policy on top: each entry carries a "referenced" bit that a read sets, and once
 * the map outgrows its maximum size, a sweep walks the entries, clearing the bit of those that were read since its last
 * pass and removing those that were not. The policy approximates LRU, with a read costing one hash lookup and, at most
 * once per sweep revolution, one bit write: no allocation, no queue and no linked list is touched on a read.
 * <p>
 * The sweep runs on the thread that inserts past the limit and is budgeted, so that no single insert pays for a whole
 * revolution. The map may therefore briefly hold a few entries more than its maximum size. A single sweep runs at a
 * time; inserts that find it running skip it, and the next insert catches up.
 * <p>
 * Iteration views are weakly consistent, like those of {@link ConcurrentHashMap}, and do not count as reads.
 *
 * @since 5.0
 */
public class ClockMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

    private static final class Entry<V> {

        final V value;

        // a new entry starts referenced, so that it survives the sweep that may pass it right after the insert
        volatile boolean referenced = true;

        Entry(V value) {
            this.value = value;
        }
    }

    private final ConcurrentHashMap<K, Entry<V>> map;
    private final ReentrantLock sweepLock;
    private final int maxSize;

    // the position of the last sweep, only touched under the sweep lock
    private Iterator<Map.Entry<K, Entry<V>>> hand;

    /**
     * @param maxSize the number of entries past which inserts evict. Zero or less keeps the map empty.
     */
    public ClockMap(int maxSize) {
        this.map = new ConcurrentHashMap<>();
        this.sweepLock = new ReentrantLock();
        this.maxSize = maxSize;
    }

    private static <V> V unwrap(Entry<V> entry) {
        return entry == null ? null : entry.value;
    }

    private static <V> void touch(Entry<V> entry) {
        // the write is rare: a hot entry keeps its bit set until the sweep passes it
        if (!entry.referenced) {
            entry.referenced = true;
        }
    }

    @Override
    public V get(Object key) {
        Entry<V> entry = map.get(key);
        if (entry == null) {
            return null;
        }
        touch(entry);
        return entry.value;
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public V put(K key, V value) {
        Entry<V> previous = map.put(key, new Entry<>(Objects.requireNonNull(value)));
        if (previous == null) {
            evictIfFull();
        }
        return unwrap(previous);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Entry<V> previous = map.putIfAbsent(key, new Entry<>(Objects.requireNonNull(value)));
        if (previous == null) {
            evictIfFull();
            return null;
        }
        touch(previous);
        return previous.value;
    }

    @Override
    public V remove(Object key) {
        return unwrap(map.remove(key));
    }

    @Override
    public boolean remove(Object key, Object value) {
        Entry<V> entry = map.get(key);
        // an Entry has identity equality, so the removal is atomic with the value check
        return entry != null && Objects.equals(entry.value, value) && map.remove(key, entry);
    }

    @Override
    public V replace(K key, V value) {
        return unwrap(map.replace(key, new Entry<>(Objects.requireNonNull(value))));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Entry<V> entry = map.get(key);
        return entry != null
                && Objects.equals(entry.value, oldValue)
                && map.replace(key, entry, new Entry<>(Objects.requireNonNull(newValue)));
    }

    /**
     * Atomic, like {@link ConcurrentHashMap#merge(Object, Object, BiFunction)}: the remapping function runs under the
     * lock of the key's hash bin and must be short and free of side effects on this map.
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Entry<V> candidate = new Entry<>(Objects.requireNonNull(value));
        Entry<V> stored = map.merge(key, candidate, (current, offered) -> {
            V merged = remappingFunction.apply(current.value, offered.value);
            if (merged == null) {
                return null;
            }
            if (merged == current.value) {
                touch(current);
                return current;
            }
            return merged == offered.value ? offered : new Entry<>(merged);
        });

        if (stored == candidate) {
            evictIfFull();
        }
        return unwrap(stored);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                Iterator<Map.Entry<K, Entry<V>>> it = map.entrySet().iterator();
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        Map.Entry<K, Entry<V>> e = it.next();
                        return new SimpleImmutableEntry<>(e.getKey(), e.getValue().value);
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public void clear() {
                map.clear();
            }
        };
    }

    private void evictIfFull() {
        int over = map.size() - maxSize;
        if (over <= 0 || !sweepLock.tryLock()) {
            return;
        }

        try {
            // the hand clears the bit of every entry it passes and removes the entries it finds cleared. The budget
            // grows with the overflow, so inserts can not outrun the sweeps, while a single insert never walks more
            // than a slice of the map; the next insert resumes where this one stopped
            int budget = 2 * over + 32;
            while (over > 0 && budget-- > 0) {
                if (hand == null || !hand.hasNext()) {
                    hand = map.entrySet().iterator();
                    if (!hand.hasNext()) {
                        return;
                    }
                }

                Map.Entry<K, Entry<V>> next = hand.next();
                Entry<V> entry = next.getValue();
                if (entry.referenced) {
                    entry.referenced = false;
                } else if (map.remove(next.getKey(), entry)) {
                    // a two-argument remove never evicts an entry that was concurrently replaced
                    over--;
                }
            }
        } finally {
            sweepLock.unlock();
        }
    }
}
