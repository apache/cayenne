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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The layout of a {@link DataRow}: the column names in column order, with a lookup of a name to its position. A row
 * holds its values by position in its layout, and all the rows of one result set share a layout. Layouts are
 * immutable and compared by identity, so two rows share a layout only when they came from the same reader. A layout
 * is serializable, and the rows serialized in one stream keep sharing theirs.
 * <p>
 * A consumer that needs a fixed set of columns from rows of any layouts builds a layout of those names and reads
 * them through a {@link Reader} of it.
 *
 * @since 5.0
 */
public final class DataRowLayout implements Serializable {

    private final String[] names;
    private final transient Map<String, Integer> indexes;

    private DataRowLayout(String[] names) {
        this.indexes = new HashMap<>((int) Math.ceil(names.length / 0.75));

        // the array is owned, so the duplicates are renamed in place
        for (int i = names.length - 1; i >= 0; i--) {
            String name = names[i];
            if (indexes.containsKey(name)) {
                do {
                    name = name + "_";
                } while (indexes.containsKey(name));
                names[i] = name;
            }
            indexes.put(name, i);
        }
        this.names = names;
    }

    /**
     * Returns a layout of the given column names, in the given order. A repeated name is deduplicated: the last
     * occurrence keeps the name, the way the last put wins in a map, and each earlier one is renamed with an
     * underscore appended until it is unique.
     */
    public static DataRowLayout of(String... names) {
        return new DataRowLayout(names.clone());
    }

    /**
     * Returns the number of columns.
     */
    public int size() {
        return names.length;
    }

    /**
     * Returns the position of the named column, or -1 if there is no such column.
     */
    public int indexOf(String name) {
        Integer index = indexes.get(name);
        return index != null ? index : -1;
    }

    /**
     * Returns the name of the column at the given position.
     */
    public String nameAt(int index) {
        return names[index];
    }

    /**
     * Returns a reader of the columns of this layout from rows of any layouts.
     */
    public Reader reader() {
        return new Reader(this);
    }

    @Override
    public String toString() {
        return "DataRowLayout[" + String.join(",", names) + "]";
    }

    /**
     * Rebuilds the index of the names, which is not serialized.
     */
    @Serial
    private Object readResolve() {
        return new DataRowLayout(names);
    }

    /**
     * Reads the columns of a layout from rows of any layouts. A row of the reader's own layout is read by position.
     * A row of another layout has the reader's column names resolved to positions in it, which the reader keeps
     * until it reads a row of yet another layout, so a loop over a result set resolves the positions once.
     * <p>
     * A reader is thus a per-execution object: one is created per query execution, or per other loop over the rows
     * of one result set, and not shared across them. It may still be shared between threads: the resolved positions
     * are kept as one immutable pair, and the race of two threads resolving them costs a repeated resolution only.
     */
    public static final class Reader {

        private final DataRowLayout layout;
        private Resolved resolved;

        private record Resolved(DataRowLayout rowLayout, int[] indexes) {
        }

        private Reader(DataRowLayout layout) {
            this.layout = layout;
        }

        /**
         * Returns the value of the i-th column of the reader's layout in the row, or null if the row has no such
         * column. A null is told from a missing column with {@link #has(DataRow, int)}.
         */
        public Object get(DataRow row, int i) {
            DataRowLayout rowLayout = row.layout;
            if (rowLayout == layout) {
                return row.values[i];
            }
            int index = indexesIn(rowLayout)[i];
            return index < 0 ? null : row.values[index];
        }

        /**
         * Returns whether the row has the i-th column of the reader's layout.
         */
        public boolean has(DataRow row, int i) {
            DataRowLayout rowLayout = row.layout;
            return rowLayout == layout || indexesIn(rowLayout)[i] >= 0;
        }

        private int[] indexesIn(DataRowLayout rowLayout) {
            Resolved r = resolved;
            if (r == null || r.rowLayout != rowLayout) {
                int[] indexes = new int[layout.names.length];
                for (int j = 0; j < indexes.length; j++) {
                    indexes[j] = rowLayout.indexOf(layout.names[j]);
                }
                r = new Resolved(rowLayout, indexes);
                resolved = r;
            }
            return r.indexes;
        }

        @Override
        public String toString() {
            return "Reader" + layout;
        }
    }
}
