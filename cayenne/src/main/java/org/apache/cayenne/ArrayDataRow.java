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

import java.io.Serial;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A {@link DataRow} stored as an array of values over a {@link RowLayout}.
 *
 * @since 5.0
 */
final class ArrayDataRow extends DataRow {

    private final RowLayout layout;
    private final Object[] values;

    ArrayDataRow(String entityName, RowLayout layout, Object[] values, long version) {
        super(entityName, version);
        if (values.length != layout.size()) {
            throw new IllegalArgumentException(
                    "Values length " + values.length + " does not match the layout size " + layout.size());
        }
        this.layout = layout;
        this.values = values;
    }

    @Override
    public RowLayout getLayout() {
        return layout;
    }

    @Override
    public Object valueAt(int index) {
        return values[index];
    }

    @Override
    public DataRow withEntityName(String entityName) {
        return new ArrayDataRow(entityName, layout, values, getVersion());
    }

    @Override
    public DataRow merged(Map<String, ?> changes, long version) {
        Object[] merged = values.clone();
        for (Map.Entry<String, ?> entry : changes.entrySet()) {
            int index = layout.indexOf(entry.getKey());
            if (index < 0) {
                // a key outside the layout: the result has to be a map
                HashMap<String, Object> map = new HashMap<>(this);
                map.putAll(changes);
                return new MapDataRow(getEntityName(), map, version);
            }
            merged[index] = entry.getValue();
        }
        return new ArrayDataRow(getEntityName(), layout, merged, version);
    }

    @Override
    public int size() {
        return layout.size();
    }

    @Override
    public boolean isEmpty() {
        return layout.size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String name && layout.indexOf(name) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Object v : values) {
            if (Objects.equals(v, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object get(Object key) {
        if (!(key instanceof String name)) {
            return null;
        }
        int index = layout.indexOf(name);
        return index < 0 ? null : values[index];
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        if (!(key instanceof String name)) {
            return defaultValue;
        }
        int index = layout.indexOf(name);
        return index < 0 ? defaultValue : values[index];
    }

    @Override
    public Set<String> keySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<String> iterator() {
                return new PositionIterator<>() {
                    @Override
                    String at(int index) {
                        return layout.nameAt(index);
                    }
                };
            }

            @Override
            public int size() {
                return layout.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }
        };
    }

    @Override
    public Collection<Object> values() {
        return new AbstractCollection<>() {
            @Override
            public Iterator<Object> iterator() {
                return new PositionIterator<>() {
                    @Override
                    Object at(int index) {
                        return values[index];
                    }
                };
            }

            @Override
            public int size() {
                return layout.size();
            }
        };
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new PositionIterator<>() {
                    @Override
                    Entry<String, Object> at(int index) {
                        return new AbstractMap.SimpleImmutableEntry<>(layout.nameAt(index), values[index]);
                    }
                };
            }

            @Override
            public int size() {
                return layout.size();
            }
        };
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        for (int i = 0; i < values.length; i++) {
            action.accept(layout.nameAt(i), values[i]);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof ArrayDataRow other && other.layout == layout) {
            return Arrays.equals(values, other.values);
        }

        if (!(o instanceof Map<?, ?> map) || map.size() != layout.size()) {
            return false;
        }

        for (int i = 0; i < values.length; i++) {
            String name = layout.nameAt(i);
            Object value = values[i];
            if (value == null) {
                if (map.get(name) != null || !map.containsKey(name)) {
                    return false;
                }
            } else if (!value.equals(map.get(name))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        // the Map contract: the sum of the entry hashes, each being the key hash xor the value hash
        int h = 0;
        for (int i = 0; i < values.length; i++) {
            h += layout.nameAt(i).hashCode() ^ Objects.hashCode(values[i]);
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(getClass().getSimpleName()).append("@")
                .append(Integer.toHexString(System.identityHashCode(this))).append("[values={");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(layout.nameAt(i)).append('=').append(values[i]);
        }
        return buffer.append("}, version=").append(getVersion()).append(']').toString();
    }

    /**
     * Serializes as a map row: the layout is not shared across the wire.
     */
    @Serial
    private Object writeReplace() {
        return new MapDataRow(getEntityName(), new HashMap<>(this), getVersion());
    }

    private abstract class PositionIterator<T> implements Iterator<T> {

        private int next;

        abstract T at(int index);

        @Override
        public boolean hasNext() {
            return next < values.length;
        }

        @Override
        public T next() {
            if (next >= values.length) {
                throw new NoSuchElementException();
            }
            return at(next++);
        }
    }
}
