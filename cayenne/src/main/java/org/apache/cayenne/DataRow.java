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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * DataRow a map that holds values retrieved from the database for a given query row.
 * DataRows are used to cache raw database data and as a reference point for tracking
 * PersistentObject changes.
 * <p>
 * Every DataRow carries a {@link org.apache.cayenne.access.SnapshotVersion snapshot version}, the version of the
 * operation that produced the row. All rows read by one SQL statement share a version, as do all snapshots of one
 * commit, and an operation that runs later has a greater version. Two rows of the same ObjectId with the same version
 * therefore hold the same data. A cached row keeps its version though while later statements read the same data.
 *
 * @since 1.1
 */
public class DataRow implements Map<String, Object>, Serializable {

    private final HashMap<String, Object> values;
    private final long version;
    private final String entityName;

    /**
     * Creates a row over the given values, with the given entity name and snapshot version. The map is not copied:
     * the row takes ownership of it, and the caller must not use the map after this call. To create a row from a map
     * that stays in use, including another row, pass a copy.
     *
     * @since 5.0
     */
    public DataRow(String entityName, HashMap<String, Object> values, long version) {
        this.values = values;
        this.entityName = entityName;
        this.version = version;
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
    public Object put(String key, Object value) {
        return values.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return values.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        values.putAll(m);
    }

    @Override
    public void clear() {
        values.clear();
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

    /**
     * Rows are equal to any Map with the same entries, regardless of the version and the entity name.
     */
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
                .append(" version", version).toString();
    }

}
