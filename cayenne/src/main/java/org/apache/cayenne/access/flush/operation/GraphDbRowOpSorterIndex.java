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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

/**
 * The facts of a compiled mapping {@link GraphDbRowOpSorter} needs to sort the operations, built once per mapping.
 *
 * @param mapping           the mapping the index is built for
 * @param byEntity          the FK-holding joins by table
 * @param referencedColumns the columns referenced by the FKs
 * @param toOnesByEntity    the to-one relationships mapped to a single FK, by entity name and the FK join, whose
 *                          committed targets the object diffs may know
 * @param batchOrder        the order of the operations not depending on each other, by the operation type and the
 *                          table. Besides the order itself, it groups the operations that can run as one batch, and
 *                          the graph sort keeps each group together as much as the dependencies allow. This is not
 *                          cosmetic: the dependencies alone leave the independent operations in any order, and an
 *                          order mixing the tables breaks the batches up, running a statement per row instead of a
 *                          batch per table.
 */
record GraphDbRowOpSorterIndex(
        CompiledMapping mapping,
        Map<CompiledTable, List<CompiledJoin>> byEntity,
        Set<CompiledColumn> referencedColumns,
        Map<String, Map<CompiledJoin, String>> toOnesByEntity,
        Comparator<DbRowOp> batchOrder) {

    static GraphDbRowOpSorterIndex of(CompiledMapping mapping) {
        return new GraphDbRowOpSorterIndex(
                mapping,
                fkJoinsByTable(mapping),
                referencedColumns(mapping),
                toOnesByEntity(mapping),
                new BatchOrder(mapping));
    }

    private static Map<CompiledTable, List<CompiledJoin>> fkJoinsByTable(CompiledMapping mapping) {
        Map<CompiledTable, List<CompiledJoin>> byTable = new HashMap<>();
        for (CompiledTable table : mapping.schema().tables()) {
            for (CompiledJoin join : table.joins()) {

                // the dependencies are found from the FK side, every FK has its FK side join on its own table
                if (join.isForeignKey()) {
                    byTable.computeIfAbsent(table, t -> new ArrayList<>()).add(join);
                }
            }
        }
        return byTable;
    }

    private static Set<CompiledColumn> referencedColumns(CompiledMapping mapping) {
        Set<CompiledColumn> columns = new HashSet<>();
        for (CompiledTable table : mapping.schema().tables()) {
            for (CompiledJoin join : table.joins()) {
                if (join.isForeignKey()) {
                    join.columnPairs().forEach(pair -> columns.add(pair.target()));
                }
            }
        }
        return columns;
    }

    private static Map<String, Map<CompiledJoin, String>> toOnesByEntity(CompiledMapping mapping) {
        Map<String, Map<CompiledJoin, String>> byEntity = new HashMap<>();
        for (CompiledEntity entity : mapping.entities()) {
            for (CompiledRelationship relationship : entity.relationships()) {
                if (relationship instanceof CompiledToOneRelationship
                        && relationship.joins().size() == 1
                        && relationship.joins().getFirst().isForeignKey()) {
                    byEntity
                            .computeIfAbsent(entity.name(), e -> new HashMap<>())
                            .put(relationship.joins().getFirst(), relationship.name());
                }
            }
        }
        return byEntity;
    }

    // inserts, updates, deletes, each by the table order, reversed for the deletes
    private static final class BatchOrder implements Comparator<DbRowOp> {

        private final Comparator<CompiledTable> tableOrder;

        BatchOrder(CompiledMapping mapping) {
            this.tableOrder = new WeightedTableComparator(mapping);
        }

        @Override
        public int compare(DbRowOp o1, DbRowOp o2) {
            DbRowOpType type = o1.getType();
            int result = type.compareTo(o2.getType());
            if (result != 0) {
                return result;
            }

            result = tableOrder.compare(o1.getEntity(), o2.getEntity());
            return type == DbRowOpType.DELETE ? -result : result;
        }
    }
}
