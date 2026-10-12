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

import java.util.HashMap;
import java.util.Map;

/**
 * The shape of a {@link DataRow} stored as an array: the column names in column order, with a lookup of a name to its
 * position. One layout is shared by all the rows of one result set. Layouts are compared by identity, so two rows
 * share a layout only when they came from the same reader.
 *
 * @since 5.0
 */
public final class RowLayout {

    private final String[] names;
    private final Map<String, Integer> indexes;

    /**
     * Creates a layout over the given column names. The array is not copied unless a name is repeated in it: the
     * layout takes ownership of it, and the caller must not change it after this call. A repeated name is
     * deduplicated: the last occurrence keeps the name, the way the last put wins in a map, and each earlier one is
     * renamed with an underscore appended until it is unique.
     */
    public RowLayout(String[] names) {
        this.indexes = new HashMap<>((int) Math.ceil(names.length / 0.75));

        String[] deduplicated = names;
        for (int i = names.length - 1; i >= 0; i--) {
            String name = names[i];
            if (indexes.containsKey(name)) {
                if (deduplicated == names) {
                    deduplicated = names.clone();
                }
                do {
                    name = name + "_";
                } while (indexes.containsKey(name));
                deduplicated[i] = name;
            }
            indexes.put(name, i);
        }
        this.names = deduplicated;
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

    @Override
    public String toString() {
        return "RowLayout[" + String.join(",", names) + "]";
    }
}
