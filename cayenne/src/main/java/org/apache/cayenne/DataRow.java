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

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * DataRow is a precursor of a persistent object that holds its database values, but models no relationships. A row is
 * immutable and the mutating Map methods throw {@link UnsupportedOperationException}. A row is serializable when its
 * values are. Rows serialized in one stream keep sharing their layout.
 *
 * @since 1.1
 */
public final class DataRow implements Map<String, Object>, Serializable {

    private final String entityName;
    private final long version;
    final DataRowLayout layout;
    final Object[] values;

    private DataRow(String entityName, DataRowLayout layout, Object[] values, long version) {
        this.entityName = entityName;
        this.layout = layout;
        this.values = values;
        this.version = version;
    }

    /**
     * Creates a row over the given values laid out by the given layout, with the given entity name and snapshot
     * version. The array is not copied: the row takes ownership of it, and the caller must not use the array after
     * this call.
     *
     * @since 5.0
     */
    public static DataRow of(String entityName, DataRowLayout layout, Object[] values, long version) {
        if (values.length != layout.size()) {
            throw new IllegalArgumentException(
                    "Values length " + values.length + " does not match the layout size " + layout.size());
        }
        return new DataRow(entityName, layout, values, version);
    }

    /**
     * Returns the name of the ObjEntity this row is a snapshot of, or null if the row is not mapped to an entity.
     *
     * @since 3.0
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Returns the {@link org.apache.cayenne.access.SnapshotVersion snapshot version} of this row: the version of the
     * operation (a SQL statement result, a commit) that produced it.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Returns a row with the given entity name and this row's values and version. The values are shared: rows are
     * immutable, so the copy is free.
     *
     * @since 5.0
     */
    public DataRow withEntityName(String entityName) {
        return new DataRow(entityName, layout, values, version);
    }

    /**
     * Returns a row with this row's entity name and the given version, holding this row's values overlaid with the
     * given ones. The row keeps its layout when every changed key is a column of it, and gets its own wider layout
     * otherwise.
     *
     * @since 5.0
     */
    public DataRow merged(Map<String, ?> changes, long version) {
        Object[] merged = values.clone();
        List<Map.Entry<String, ?>> added = null;

        for (Map.Entry<String, ?> entry : changes.entrySet()) {
            int index = layout.indexOf(entry.getKey());
            if (index >= 0) {
                merged[index] = entry.getValue();
            } else {
                if (added == null) {
                    added = new ArrayList<>();
                }
                added.add(entry);
            }
        }

        if (added == null) {
            return new DataRow(entityName, layout, merged, version);
        }

        int size = values.length;
        String[] names = new String[size + added.size()];
        Object[] widened = Arrays.copyOf(merged, names.length);
        for (int i = 0; i < size; i++) {
            names[i] = layout.nameAt(i);
        }
        for (int i = 0; i < added.size(); i++) {
            names[size + i] = added.get(i).getKey();
            widened[size + i] = added.get(i).getValue();
        }
        return new DataRow(entityName, DataRowLayout.of(names), widened, version);
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
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
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("DataRow is immutable");
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
                return values.length;
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
                return values.length;
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
                return values.length;
            }
        };
    }

    /**
     * Visits the entries in column order. Unlike the entry set, allocates nothing per entry.
     */
    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        for (int i = 0; i < values.length; i++) {
            action.accept(layout.nameAt(i), values[i]);
        }
    }

    /**
     * Compares the rows as maps, i.e. by their keys and values and regardless of their entity names, versions or the
     * order of their columns.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof DataRow other && other.layout == layout) {
            return Arrays.equals(values, other.values);
        }
        if (!(o instanceof Map<?, ?> map) || map.size() != values.length) {
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
        StringBuilder buffer = new StringBuilder("DataRow@")
                .append(Integer.toHexString(System.identityHashCode(this))).append("[values={");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                buffer.append(", ");
            }
            buffer.append(layout.nameAt(i)).append('=').append(values[i]);
        }
        return buffer.append("}, version=").append(version).append(']').toString();
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
