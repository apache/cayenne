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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.Collections.*;

/**
 * Optimized mutable single-entry map.
 * <p>
 * This implementation is compatible with general {@link Map} contract, including {@link Map#equals(Object)},
 * {@link Map#hashCode()} and {@link java.util.AbstractMap#toString()} implementations.
 * <p>
 * This Map can store only one key that is defined at creation time and can't be changed.
 * This map will throw {@link IllegalArgumentException} on any put operation with the wrong key
 * and return {@code null} on get.
 * <p>
 * This map will be effectively empty after putting null value.
 *
 * @since 4.2
 */
public class SingleEntryMap<K, V> implements Map<K, V>, Map.Entry<K, V>, Serializable {

    private static final long serialVersionUID = -3848347748971431847L;

    private final K key;
    private V value;

    /**
     * Create empty map
     *
     * @param key that can be stored in this map, can't be null
     */
    public SingleEntryMap(K key) {
        this(key, null);
    }

    /**
     * Create map with single key-value entry
     *
     * @param key that can be stored in this map, can't be null
     * @param value to store, if null map will be empty.
     */
    public SingleEntryMap(K key, V value) {
        this.key = Objects.requireNonNull(key);
        this.value = value;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return this.value == null ? emptySet() : singleton(this);
    }

    @Override
    public boolean containsKey(Object key) {
        return this.value != null && this.key.equals(key);
    }

    @Override
    public int size() {
        return this.value == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return this.value == null;
    }

    @Override
    public boolean containsValue(Object value) {
        return value != null && value.equals(this.value);
    }

    @Override
    public V get(Object key) {
        return this.key.equals(key) ? this.value : null;
    }

    @Override
    public V put(K key, V value) {
        if(this.key.equals(key)) {
            return setValue(value);
        }
        throw new IllegalArgumentException("This map supports only key '" + this.key + "'");
    }

    @Override
    public V remove(Object key) {
        return this.key.equals(key) ? setValue(null) : null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        value = null;
    }

    @Override
    public Set<K> keySet() {
        return value == null ? emptySet() : singleton(key);
    }

    @Override
    public Collection<V> values() {
        return value == null ? emptySet() : singleton(value);
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V oldValue = this.value;
        this.value = value;
        return oldValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        Map<?,?> m = (Map<?,?>) o;
        return m.size() == size() && (value == null || value.equals(m.get(key)));
    }

    @Override
    public int hashCode() {
        return value == null ? 0 : key.hashCode() ^ value.hashCode();
    }

    @Override
    public String toString() {
        return value == null ? "{}" : "{" + key + "=" + value + "}";
    }

    /* below is a set of methods with default implementation in Map interface */

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if(value != null) {
            action.accept(key, value);
        }
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return this.key.equals(key) && value != null ? value : defaultValue;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        if(this.key.equals(key)) {
            if (this.value == null) {
                this.value = value;
                return null;
            }
            return this.value;
        }
        throw new IllegalArgumentException("This map supports only key '" + this.key + "'");
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if(this.key.equals(key)) {
            if(value == null) {
                value = mappingFunction.apply(key);
            }
            return value;
        }
        throw new IllegalArgumentException("This map supports only key '" + this.key + "'");
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (value != null && this.key.equals(key)) {
            return value = remappingFunction.apply(key, value);
        }
        return null;
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if(this.key.equals(key)) {
            return value = remappingFunction.apply(key, value);
        }
        throw new IllegalArgumentException("This map supports only key '" + this.key + "'");
    }


    @Override
    public V merge(K key, V newValue, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if(this.key.equals(key)) {
            return value = value == null ? newValue : remappingFunction.apply(value, newValue);
        }
        throw new IllegalArgumentException("This map supports only key '" + this.key + "'");
    }

    @Override
    public V replace(K key, V value) {
        if(this.key.equals(key) && this.value != null) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        return null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if(this.key.equals(key) && value != null && value.equals(oldValue)) {
            value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if(this.key.equals(key) && this.value != null && this.value.equals(value)) {
            this.value = null;
            return true;
        }
        return false;
    }
}
