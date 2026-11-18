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

import org.apache.cayenne.util.IdValues;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A map comparing array keys (e.g. binary PKs) by content, otherwise like HashMap. Used by to-many Map relationships.
 *
 * @since 5.0
 */
final class ContentKeyMap<K, V> extends AbstractMap<K, V> {

    private final Map<Object, Entry<K, V>> entries;
    private transient Set<Entry<K, V>> entrySet;

    ContentKeyMap() {
        this.entries = new HashMap<>();
    }

    ContentKeyMap(int expectedSize) {
        this.entries = HashMap.newHashMap(expectedSize);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(IdValues.valueKey(key));
    }

    @Override
    public V get(Object key) {
        Entry<K, V> entry = entries.get(IdValues.valueKey(key));
        return entry != null ? entry.getValue() : null;
    }

    @Override
    public V put(K key, V value) {
        Entry<K, V> entry = entries.putIfAbsent(IdValues.valueKey(key), new SimpleEntry<>(key, value));
        return entry != null ? entry.setValue(value) : null;
    }

    @Override
    public V remove(Object key) {
        Entry<K, V> entry = entries.remove(IdValues.valueKey(key));
        return entry != null ? entry.getValue() : null;
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = entrySet;
        if (es == null) {
            entrySet = es = new EntrySet();
        }
        return es;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> entry : entries.values()) {
            h += IdValues.valueHashCode(entry.getKey()) ^ Objects.hashCode(entry.getValue());
        }
        return h;
    }

    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return entries.values().iterator();
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public void clear() {
            entries.clear();
        }
    }
}
