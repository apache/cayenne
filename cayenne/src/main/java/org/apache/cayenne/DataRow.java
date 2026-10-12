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
import java.util.Map;

/**
 * DataRow is a read-only map that holds values retrieved from the database for a given query row. DataRows are used to
 * cache raw database data and as a reference point for tracking PersistentObject changes.
 * <p>
 * Every DataRow carries a {@link org.apache.cayenne.access.SnapshotVersion snapshot version}, the version of the
 * operation that produced the row. All rows read by one SQL statement share a version, as do all snapshots of one
 * commit, and an operation that runs later has a greater version. Two rows of the same ObjectId with the same version
 * therefore hold the same data. A cached row keeps its version though while later statements read the same data.
 * <p>
 * A row is immutable: the mutating Map methods throw {@link UnsupportedOperationException}, and the key, value and
 * entry views of a row stored as a map are the views of that map, which the caller is not to change either. A row
 * with changed values is derived with {@link #merged(Map, long)}. Rows come in two forms. A row read from a result set is stored as an
 * array of values over a {@link RowLayout} shared by all the rows of the result set, and exposes its
 * {@link #getLayout() layout} so that the values can be read by position. Any other row is stored as a map and has no
 * layout.
 *
 * @since 1.1
 */
public abstract sealed class DataRow implements Map<String, Object>, Serializable permits MapDataRow, ArrayDataRow {

    private final long version;
    private final String entityName;

    DataRow(String entityName, long version) {
        this.entityName = entityName;
        this.version = version;
    }

    /**
     * Creates a row over the given values, with the given entity name and snapshot version. The map is not copied:
     * the row takes ownership of it, and the caller must not change the map after this call. To create a row from a
     * map that stays in use, pass a copy. The map should be serializable for the row to be.
     *
     * @since 5.0
     */
    public static DataRow of(String entityName, Map<String, Object> values, long version) {
        return new MapDataRow(entityName, values, version);
    }

    /**
     * Creates a row over the given values laid out by the given layout, with the given entity name and snapshot
     * version. The array is not copied: the row takes ownership of it, and the caller must not use the array after
     * this call.
     *
     * @since 5.0
     */
    public static DataRow of(String entityName, RowLayout layout, Object[] values, long version) {
        return new ArrayDataRow(entityName, layout, values, version);
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
     * Returns the layout of a row stored as an array, or null if the row is stored as a map. The values of a row with
     * a layout can be read by position with {@link #valueAt(int)}.
     *
     * @since 5.0
     */
    public abstract RowLayout getLayout();

    /**
     * Returns the value at the given position of the {@link #getLayout() layout} of this row.
     *
     * @throws UnsupportedOperationException if the row has no layout
     * @since 5.0
     */
    public abstract Object valueAt(int index);

    /**
     * Returns a row with the given entity name and this row's values and version. The values are shared: rows are
     * immutable, so the copy is free.
     *
     * @since 5.0
     */
    public abstract DataRow withEntityName(String entityName);

    /**
     * Returns a row with this row's entity name and the given version, holding this row's values overlaid with the
     * given ones. A row with a layout keeps it when every changed key is a column of the layout.
     *
     * @since 5.0
     */
    public abstract DataRow merged(Map<String, ?> changes, long version);

    @Override
    public final Object put(String key, Object value) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public final Object remove(Object key) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public final void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    @Override
    public final void clear() {
        throw new UnsupportedOperationException("DataRow is immutable");
    }

    /**
     * Rows are equal to any Map with the same entries, regardless of the version and the entity name.
     */
    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
