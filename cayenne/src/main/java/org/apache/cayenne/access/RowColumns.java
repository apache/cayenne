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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.RowLayout;

/**
 * Reads a fixed set of named columns from rows, by position when a row has a {@link RowLayout}. The positions are
 * resolved when a row of a new layout comes along, so a loop over the rows of one result set looks the names up
 * once. Not thread-safe.
 */
final class RowColumns {

    private final String[] names;
    private RowLayout layout;
    private int[] indexes;

    RowColumns(String[] names) {
        this.names = names;
    }

    /**
     * Returns the value of the i-th column of the row, or null if the row has no such column.
     */
    Object get(DataRow row, int i) {
        RowLayout rowLayout = row.getLayout();
        if (rowLayout == null) {
            return row.get(names[i]);
        }

        if (rowLayout != layout) {
            int[] resolved = new int[names.length];
            for (int j = 0; j < names.length; j++) {
                resolved[j] = rowLayout.indexOf(names[j]);
            }
            indexes = resolved;
            layout = rowLayout;
        }

        int index = indexes[i];
        return index < 0 ? null : row.valueAt(index);
    }
}
