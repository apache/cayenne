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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;

import java.util.List;
import java.util.Map;

/**
 * The batch an operation can run in: the operations with equal keys run as one batch, i.e. one statement executed with
 * the parameters of each operation, when they come one after another. {@link DbRowOpPlanner} keeps them together as
 * much as their dependencies allow. A key is made of the facts of the statement of an operation, not of its values:
 * the table, the columns written, and the shape of the qualifier. The columns are compared as sets, as the values of
 * each operation of a batch are bound by the column names.
 *
 * <p>A key refers to the columns and the qualifier of its operation, instead of copying them, so it is taken once the
 * operation is complete.
 *
 * @since 6.0
 */
public final class BatchKey {

    enum Kind {
        INSERT, UPDATE, DELETE, SOFT_DELETE
    }

    private final Kind kind;
    private final CompiledTable table;

    // the columns written by an update, empty for the other operations, as an insert writes all the columns
    private final List<CompiledColumn> columns;

    // the qualifier columns besides the PK, e.g. for the optimistic locking, with their values, of which only the
    // nulls matter, compared to NULL by the statement
    private final Map<CompiledColumn, Object> qualifier;
    private final boolean optimisticLock;

    private final int hash;

    BatchKey(
            Kind kind,
            CompiledTable table,
            List<CompiledColumn> columns,
            Map<CompiledColumn, Object> qualifier,
            boolean optimisticLock) {

        this.kind = kind;
        this.table = table;
        this.columns = columns;
        this.qualifier = qualifier;
        this.optimisticLock = optimisticLock;

        // the hashes of the columns are summed, for the hash not to depend on their order
        int columnsHash = 0;
        for (CompiledColumn column : columns) {
            columnsHash += column.name().hashCode();
        }
        int qualifierHash = 0;
        for (Map.Entry<CompiledColumn, Object> e : qualifier.entrySet()) {
            qualifierHash += 31 * e.getKey().name().hashCode() + (e.getValue() == null ? 1 : 0);
        }

        int h = kind.ordinal();
        h = 31 * h + table.name().hashCode();
        h = 31 * h + columnsHash;
        h = 31 * h + qualifierHash;
        this.hash = 2 * h + (optimisticLock ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BatchKey other) || hash != other.hash) {
            return false;
        }
        return kind == other.kind
                && optimisticLock == other.optimisticLock
                && sameTable(table, other.table)
                && sameColumns(columns, other.columns)
                && sameQualifier(qualifier, other.qualifier);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder().append(kind).append(' ').append(table.name());
        columns.forEach(c -> buffer.append(' ').append(c.name()));
        if (!qualifier.isEmpty()) {
            buffer.append(" where");
            qualifier.keySet().forEach(c -> buffer.append(' ').append(c.name()));
        }
        return buffer.toString();
    }

    private static boolean sameTable(CompiledTable t1, CompiledTable t2) {
        return t1 == t2 || t1.name().equals(t2.name()) && t1.map().name().equals(t2.map().name());
    }

    // the same columns in any order, by name. The lists are short, and have no repeats, so a scan is the cheapest
    private static boolean sameColumns(List<CompiledColumn> c1, List<CompiledColumn> c2) {
        if (c1.size() != c2.size()) {
            return false;
        }
        for (CompiledColumn column : c1) {
            if (find(c2, column.name()) == null) {
                return false;
            }
        }
        return true;
    }

    // the same columns, by name, with the same values compared to NULL
    private static boolean sameQualifier(Map<CompiledColumn, Object> q1, Map<CompiledColumn, Object> q2) {
        if (q1.size() != q2.size()) {
            return false;
        }
        for (Map.Entry<CompiledColumn, Object> e1 : q1.entrySet()) {
            boolean found = false;
            for (Map.Entry<CompiledColumn, Object> e2 : q2.entrySet()) {
                if (e1.getKey().name().equals(e2.getKey().name())) {
                    found = (e1.getValue() == null) == (e2.getValue() == null);
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static CompiledColumn find(List<CompiledColumn> columns, String name) {
        for (CompiledColumn column : columns) {
            if (column.name().equals(name)) {
                return column;
            }
        }
        return null;
    }
}
