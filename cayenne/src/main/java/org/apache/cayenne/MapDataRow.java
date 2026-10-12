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

import org.apache.cayenne.util.ToStringBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A {@link DataRow} stored as a map.
 *
 * @since 5.0
 */
final class MapDataRow extends DataRow {

    private final Map<String, Object> values;

    MapDataRow(String entityName, Map<String, Object> values, long version) {
        super(entityName, version);
        this.values = values;
    }

    @Override
    public RowLayout getLayout() {
        return null;
    }

    @Override
    public Object valueAt(int index) {
        throw new UnsupportedOperationException("The row has no layout");
    }

    @Override
    public DataRow withEntityName(String entityName) {
        return new MapDataRow(entityName, values, getVersion());
    }

    @Override
    public DataRow merged(Map<String, ?> changes, long version) {
        HashMap<String, Object> merged = new HashMap<>(values);
        merged.putAll(changes);
        return new MapDataRow(getEntityName(), merged, version);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return values.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return values.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return values.get(key);
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    @Override
    public Collection<Object> values() {
        return values.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        values.forEach(action);
    }

    @Override
    public boolean equals(Object o) {
        return values.equals(o);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("values", values)
                .append(" version", getVersion()).toString();
    }
}
