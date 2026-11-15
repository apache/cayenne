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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.flush.EffectiveOpId;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;

/**
 * Breaks the dependency cycles of the inserts, e.g. of the rows referencing each other, by splitting an insert of a
 * cycle into an insert with its FKs referencing the other rows of the cycle set to NULL, and an update setting them.
 * The FKs must be nullable. The update depends on the insert and on the rows the FKs reference, so it can run after
 * them all, while nothing in the cycle depends on the insert without the FKs.
 */
final class InsertCycleSplitter {

    private InsertCycleSplitter() {
    }

    /**
     * Returns the operations with an insert of each cycle of the sort pass split, in place of the insert, and the
     * update appended to the list. Skips a cycle with an insert split already, as splitting it may have broken the
     * cycle, and the next sort pass finds it if it didn't. Returns null if nothing could be split.
     */
    static List<DbRowOp> split(
            DefaultDbRowOpPlannerIndex index,
            List<DbRowOp> ops,
            DefaultDbRowOpPlanner.SortPass pass) {

        List<DbRowOp> result = null;
        boolean[] split = new boolean[ops.size()];
        boolean[] inCycle = new boolean[ops.size()];

        for (int[] cycle : pass.cycles()) {
            if (Arrays.stream(cycle).anyMatch(v -> split[v])) {
                continue;
            }

            for (int v : cycle) {
                inCycle[v] = true;
            }

            // the inserts are tried in the batch order, for the choice not to depend on the order of the operations,
            // except for the operations of the same table
            Integer[] candidates = Arrays.stream(cycle).boxed().toArray(Integer[]::new);
            Comparator<DbRowOp> batchOrder = index.batchOrder();
            Arrays.sort(candidates, (v1, v2) -> {
                int c = batchOrder.compare(ops.get(v1), ops.get(v2));
                return c != 0 ? c : Integer.compare(v1, v2);
            });

            for (int v : candidates) {
                List<CompiledColumn> fks = deferrableFks(index, ops, pass, inCycle, v);
                if (fks != null) {
                    if (result == null) {
                        result = new ArrayList<>(ops);
                    }

                    InsertDbRowOp insert = (InsertDbRowOp) ops.get(v);
                    InsertDbRowOp insertWithoutFks = new InsertDbRowOp(
                            insert.getObject(),
                            insert.getEntity(),
                            insert.getChangeId());
                    UpdateDbRowOp fkUpdate = new UpdateDbRowOp(
                            insert.getObject(),
                            insert.getEntity(),
                            insert.getChangeId());
                    insert.getValues().splitTo(insertWithoutFks.getValues(), fkUpdate.getValues(), fks);

                    result.set(v, insertWithoutFks);
                    result.add(fkUpdate);
                    split[v] = true;
                    break;
                }
            }

            for (int v : cycle) {
                inCycle[v] = false;
            }
        }

        return result;
    }

    /**
     * Returns the FK columns of an insert that reference the other operations of its cycle, if the insert depends on
     * the cycle only by these FKs, and all of them are nullable. Otherwise, or for an operation that is not an insert,
     * returns null.
     */
    private static List<CompiledColumn> deferrableFks(
            DefaultDbRowOpPlannerIndex index,
            List<DbRowOp> ops,
            DefaultDbRowOpPlanner.SortPass pass,
            boolean[] inCycle,
            int v) {

        if (pass.types()[v] != DbRowOpType.INSERT) {
            return null;
        }

        // a dependency on the operations of the same id (e.g. the delete of the row with the same meaningful PK) is
        // not an FK
        for (int sameId : pass.indexById().get(pass.ids()[v])) {
            if (sameId != v && inCycle[sameId]) {
                return null;
            }
        }

        DbRowOp insert = ops.get(v);
        List<CompiledJoin> joins = index.byEntity().get(insert.getEntity());
        if (joins == null) {
            return null;
        }

        Map<String, Object> values = ((InsertDbRowOp) insert).getValues().getSnapshot();
        List<CompiledColumn> fks = new ArrayList<>();
        for (CompiledJoin join : joins) {
            if (referencesCycle(ops, pass, inCycle, v, join, values)) {
                for (CompiledColumnPair pair : join.columnPairs()) {
                    if (pair.source().isMandatory()) {
                        return null;
                    }
                    if (!fks.contains(pair.source())) {
                        fks.add(pair.source());
                    }
                }
            }
        }

        return fks.isEmpty() ? null : fks;
    }

    // the FK of the insert makes it depend on another operation of the cycle, the same way DefaultDbRowOpPlanner adds
    // the dependencies of an insert
    private static boolean referencesCycle(
            List<DbRowOp> ops,
            DefaultDbRowOpPlanner.SortPass pass,
            boolean[] inCycle,
            int v,
            CompiledJoin join,
            Map<String, Object> values) {

        EffectiveOpId parentId = DefaultDbRowOpPlanner.effectiveIdFor(join, values);
        if (parentId == null) {
            return false;
        }

        int[] parents = pass.indexById().get(parentId);
        if (parents == null) {
            return false;
        }

        for (int parent : parents) {
            if (parent == v || !inCycle[parent]) {
                continue;
            }

            switch (pass.types()[parent]) {
                case INSERT -> {
                    return true;
                }
                case UPDATE -> {
                    if (DefaultDbRowOpPlanner.fkReferenceUpdated((UpdateDbRowOp) ops.get(parent), join)) {
                        return true;
                    }
                }
                case DELETE -> {
                }
            }
        }
        return false;
    }
}
