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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.access.flush.EffectiveOpId;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;

/**
 * Breaks the dependency cycles of the operations, e.g. of the rows referencing each other, by moving the writes of the
 * nullable FKs of one operation of a cycle that reference the other operations of the cycle into an update:
 * <ul>
 *     <li>An insert is split into an insert with the FKs set to NULL, and an update setting them. The update depends
 *     on the insert and on the rows the FKs reference, so it can run after them all, while nothing in the cycle
 *     depends on the insert without the FKs.</li>
 *     <li>A delete gets an update setting the FKs to NULL before it, so that the deletes of the rows the FKs reference
 *     depend on the update instead of the delete.</li>
 * </ul>
 * An insert of a row referencing itself by the PK the insert generates is split the same way, as a cycle of one
 * operation.
 */
final class CycleSplitter {

    private final DefaultDbRowOpPlannerIndex index;
    private final List<DbRowOp> ops;
    private final DefaultDbRowOpPlanner.SortPass pass;
    private final Map<DbRowOp, Set<CompiledJoin>> nulledFks;
    private final boolean[] inCycle;

    // the operations with the split ones replaced and the updates added, created on the first split
    private List<DbRowOp> result;

    private CycleSplitter(
            DefaultDbRowOpPlannerIndex index,
            List<DbRowOp> ops,
            DefaultDbRowOpPlanner.SortPass pass,
            Map<DbRowOp, Set<CompiledJoin>> nulledFks) {

        this.index = index;
        this.ops = ops;
        this.pass = pass;
        this.nulledFks = nulledFks;
        this.inCycle = new boolean[ops.size()];
    }

    /**
     * Returns the operations with an operation of each cycle of the sort pass split: a split insert is replaced, and
     * the update is appended to the list. For a delete, the FKs set to NULL are added to "nulledFks", for the next
     * sort pass to drop the dependencies on them. Skips a cycle with an operation split already, as splitting it may
     * have broken the cycle, and the next sort pass finds it if it didn't. Returns null if nothing could be split.
     */
    static List<DbRowOp> split(
            DefaultDbRowOpPlannerIndex index,
            List<DbRowOp> ops,
            DefaultDbRowOpPlanner.SortPass pass,
            Map<DbRowOp, Set<CompiledJoin>> nulledFks) {

        return new CycleSplitter(index, ops, pass, nulledFks).split();
    }

    private List<DbRowOp> split() {
        boolean[] split = new boolean[ops.size()];

        for (int[] cycle : pass.cycles()) {
            if (Arrays.stream(cycle).anyMatch(v -> split[v])) {
                continue;
            }

            for (int v : cycle) {
                inCycle[v] = true;
            }

            for (int v : candidates(cycle)) {
                boolean done = switch (pass.types()[v]) {
                    case INSERT -> {
                        List<CompiledColumn> fks = insertFks(v);
                        if (fks != null) {
                            splitInsert(v, fks);
                        }
                        yield fks != null;
                    }
                    case DELETE -> nullFksBeforeDelete(v);
                    case UPDATE -> false;
                };

                if (done) {
                    split[v] = true;
                    break;
                }
            }

            for (int v : cycle) {
                inCycle[v] = false;
            }
        }

        // the inserts referencing their own rows by the generated PKs are the cycles of one operation
        Map<Integer, List<CompiledJoin>> selfReferences = new LinkedHashMap<>();
        for (DefaultDbRowOpPlanner.SelfReference reference : pass.selfReferences()) {
            selfReferences.computeIfAbsent(reference.insert(), i -> new ArrayList<>()).add(reference.join());
        }

        selfReferences.forEach((v, joins) -> {
            List<CompiledColumn> fks = split[v] ? null : nullableFks(joins);
            if (fks != null) {
                splitInsert(v, fks);
                split[v] = true;
            }
        });

        return result;
    }

    // The operations of a cycle in the table order, for the choice not to depend on the order of the operations,
    // except for the operations of the same table. So the row of the table first in the table order gets its FKs set
    // later for an insert, and earlier for a delete, while the inserts and the deletes keep their usual order
    private Integer[] candidates(int[] cycle) {
        Comparator<DbRowOp> batchOrder = index.batchOrder();
        Integer[] candidates = Arrays.stream(cycle).boxed().toArray(Integer[]::new);
        Arrays.sort(candidates, (v1, v2) -> {
            DbRowOp op1 = ops.get(v1);
            DbRowOp op2 = ops.get(v2);
            int c = batchOrder.compare(op1, op2);

            // the batch order of the deletes is the reverse table order
            if (op1.getType() == DbRowOpType.DELETE && op2.getType() == DbRowOpType.DELETE) {
                c = -c;
            }
            return c != 0 ? c : Integer.compare(v1, v2);
        });
        return candidates;
    }

    private void splitInsert(int v, List<CompiledColumn> fks) {
        InsertDbRowOp insert = (InsertDbRowOp) ops.get(v);
        InsertDbRowOp insertWithoutFks = new InsertDbRowOp(
                insert.getObject(),
                insert.getEntity(),
                insert.getChangeId());
        UpdateDbRowOp fkUpdate = new UpdateDbRowOp(insert.getObject(), insert.getEntity(), insert.getChangeId());
        insert.getValues().splitTo(insertWithoutFks.getValues(), fkUpdate.getValues(), fks);

        result().set(v, insertWithoutFks);
        result().add(fkUpdate);
    }

    private boolean nullFksBeforeDelete(int v) {
        DbRowOp delete = ops.get(v);

        // the row of a soft delete stays, so its FKs must stay too
        if (delete instanceof SoftDeleteDbRowOp || dependsOnSameIdInCycle(v)) {
            return false;
        }

        Set<CompiledJoin> joins = new HashSet<>();
        for (DefaultDbRowOpPlanner.DeleteDependency dependency : pass.deleteDependencies()) {
            if (dependency.delete() == v && inCycle[dependency.parentDelete()]) {
                joins.add(dependency.join());
            }
        }

        List<CompiledColumn> fks = nullableFks(joins);
        if (fks == null) {
            return false;
        }

        UpdateDbRowOp fkUpdate = new UpdateDbRowOp(delete.getObject(), delete.getEntity(), delete.getChangeId());
        for (CompiledColumn fk : fks) {
            fkUpdate.getValues().addValue(fk, null, true);
        }

        result().add(fkUpdate);
        nulledFks.computeIfAbsent(delete, d -> new HashSet<>()).addAll(joins);
        return true;
    }

    /**
     * Returns the FK columns of an insert that reference the other operations of its cycle, if the insert depends on
     * the cycle only by these FKs, and all of them are nullable. Otherwise, returns null.
     */
    private List<CompiledColumn> insertFks(int v) {
        if (dependsOnSameIdInCycle(v)) {
            return null;
        }

        InsertDbRowOp insert = (InsertDbRowOp) ops.get(v);
        List<CompiledJoin> joins = index.byEntity().get(insert.getEntity());
        if (joins == null) {
            return null;
        }

        Map<String, Object> values = insert.getValues().getSnapshot();
        List<CompiledJoin> cycleJoins = new ArrayList<>();
        for (CompiledJoin join : joins) {
            if (referencesCycle(v, join, values)) {
                cycleJoins.add(join);
            }
        }

        return nullableFks(cycleJoins);
    }

    // the source columns of the joins, if all of them are nullable, otherwise, or if there are no joins, null
    private static List<CompiledColumn> nullableFks(Iterable<CompiledJoin> joins) {
        List<CompiledColumn> fks = new ArrayList<>();
        for (CompiledJoin join : joins) {
            for (CompiledColumnPair pair : join.columnPairs()) {
                if (pair.source().isMandatory()) {
                    return null;
                }
                if (!fks.contains(pair.source())) {
                    fks.add(pair.source());
                }
            }
        }
        return fks.isEmpty() ? null : fks;
    }

    // a dependency on the operations of the same id (e.g. the delete of the row with the same meaningful PK) is not an
    // FK
    private boolean dependsOnSameIdInCycle(int v) {
        for (int sameId : pass.indexById().get(pass.ids()[v])) {
            if (sameId != v && inCycle[sameId]) {
                return true;
            }
        }
        return false;
    }

    // the FK of the insert makes it depend on another operation of the cycle, the same way DefaultDbRowOpPlanner adds
    // the dependencies of an insert
    private boolean referencesCycle(int v, CompiledJoin join, Map<String, Object> values) {
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

    private List<DbRowOp> result() {
        if (result == null) {
            result = new ArrayList<>(ops);
        }
        return result;
    }
}
