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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataContextObjectStore;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.EffectiveOpId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.util.SingleEntryMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DB operation planner that orders the operations by the dependencies between their rows, replacing the operations
 * of the dependency cycles by the operations that can be ordered, when possible.
 *
 * @since 6.0
 */
public class DefaultDbRowOpPlanner implements DbRowOpPlanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDbRowOpPlanner.class);

    private final Provider<DataDomain> dataDomainProvider;

    // the index of the mapping it was built for; rebuilt when the domain mapping is replaced
    private volatile DefaultDbRowOpPlannerIndex index;

    public DefaultDbRowOpPlanner(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    private DefaultDbRowOpPlannerIndex index() {
        CompiledMapping mapping = dataDomainProvider.get().getMapping();
        DefaultDbRowOpPlannerIndex localIndex = index;
        if (localIndex == null || localIndex.mapping() != mapping) {
            synchronized (this) {
                localIndex = index;
                if (localIndex == null || localIndex.mapping() != mapping) {
                    localIndex = DefaultDbRowOpPlannerIndex.of(mapping);
                    index = localIndex;
                }
            }
        }
        return localIndex;
    }

    /**
     * Orders the operations. The operations with dependency cycles, i.e. with no order satisfying all the dependencies,
     * are replaced by the operations that can be ordered when possible: an insert of a cycle is split into an insert
     * with its nullable FKs referencing the cycle set to NULL, and an update setting them after the rows they reference
     * are inserted, and a delete of a cycle gets an update setting its nullable FKs referencing the cycle to NULL
     * before the deletes. So the result may have more operations than the input. The input operations are not changed.
     * The cycles that can't be resolved this way are broken by sorting one of their operations ahead of its
     * dependencies, and run as such, as the DB may still accept them, e.g. with deferred FK constraints.
     */
    @Override
    public List<DbRowOp> plan(List<DbRowOp> dbRows) {
        DefaultDbRowOpPlannerIndex index = index();

        List<DbRowOp> ops = dbRows;

        // the FKs of the deleted rows set to NULL before the deletes, by an update added to break a cycle, so that the
        // deletes no longer depend on each other by these FKs. By the identity of the delete operations
        Map<DbRowOp, Set<CompiledJoin>> nulledFks = new IdentityHashMap<>();

        while (true) {
            SortPass pass = sortPass(index, ops, nulledFks);
            if (pass.cycles().isEmpty()) {
                return pass.sorted();
            }

            // each pass splits an operation of each of the cycles it found, unless it already split an operation of
            // the same cycle, and the next pass finds the cycles left
            List<DbRowOp> split = CycleSplitter.split(index, ops, pass, nulledFks);
            if (split == null) {
                LOGGER.warn("""
                        Dependency cycles of the operations that can't be broken. Sending the operations in an order \
                        the DB may reject: {}""", pass.cycleOps());
                return pass.sorted();
            }
            ops = split;
        }
    }

    private SortPass sortPass(
            DefaultDbRowOpPlannerIndex index,
            List<DbRowOp> dbRows,
            Map<DbRowOp, Set<CompiledJoin>> nulledFks) {

        // ops are addressed by their index in the list from here on
        int size = dbRows.size();
        DbRowOpType[] types = new DbRowOpType[size];
        EffectiveOpId[] ids = new EffectiveOpId[size];

        // the tables whose committed rows the FKs of the other operations may reference, so that the committed FKs
        // need to be read
        Set<String> tablesWithDeletes = new HashSet<>();
        Set<String> tablesWithReferencedUpdates = new HashSet<>();

        // build op index by ID. Multiple ops per ID are only possible with meaningful IDs, so the value arrays are
        // almost always of size 1
        Map<EffectiveOpId, int[]> indexById = HashMap.newHashMap(size);
        for (int i = 0; i < size; i++) {
            DbRowOp op = dbRows.get(i);
            types[i] = op.getType();
            ids[i] = effectiveIdFor(op);

            int[] sameIdOps = indexById.putIfAbsent(ids[i], new int[]{i});
            if (sameIdOps != null) {
                int[] expanded = Arrays.copyOf(sameIdOps, sameIdOps.length + 1);
                expanded[sameIdOps.length] = i;
                indexById.put(ids[i], expanded);
            }

            switch (types[i]) {
                case DELETE -> tablesWithDeletes.add(op.getEntity().name());
                case UPDATE -> {
                    if (updatesReferencedColumn((UpdateDbRowOp) op, index.referencedColumns())) {
                        tablesWithReferencedUpdates.add(op.getEntity().name());
                    }
                }
                case INSERT -> {
                }
            }
        }
        boolean hasMeaningfulIds = indexById.size() != size;

        // build ops dependency graph
        Edges edges = new Edges(
                dbRows,
                types,
                indexById,
                new DbRowOpGraph(dbRows),
                tablesWithDeletes,
                tablesWithReferencedUpdates,
                index.toOnesByEntity(),
                nulledFks);

        for (int i = 0; i < size; i++) {
            edges.addRelationshipEdges(index.byEntity(), i);
            if (hasMeaningfulIds) {
                edges.addMeaningfulIdEdges(ids, i);
            }
            edges.graph.add(i);
        }
        edges.addPendingEdges();

        List<DbRowOp> sorted = edges.graph.topSort(index.batchOrder());
        return new SortPass(
                dbRows,
                sorted,
                types,
                ids,
                indexById,
                edges.graph.brokenCycles(),
                edges.deleteDependencies);
    }

    private static boolean updatesReferencedColumn(UpdateDbRowOp op, Set<CompiledColumn> referencedColumns) {
        for (CompiledColumn column : op.getValues().getUpdatedAttributes()) {
            if (referencedColumns.contains(column)) {
                return true;
            }
        }
        return false;
    }

    static boolean fkReferenceUpdated(UpdateDbRowOp parentOp, CompiledJoin join) {
        List<CompiledColumn> updatedColumns = parentOp.getValues().getUpdatedAttributes();
        for (CompiledColumnPair pair : join.columnPairs()) {
            if (updatedColumns.contains(pair.target())) {
                return true;
            }
        }
        return false;
    }

    private static EffectiveOpId effectiveIdFor(DbRowOp op) {
        return new EffectiveOpId(op.getEntity().name(), op.getChangeId());
    }

    static EffectiveOpId effectiveIdFor(CompiledJoin relationship, Map<String, Object> opSnapshot) {
        int len = relationship.columnPairs().size();
        Map<String, Object> idMap = len == 1
                ? new SingleEntryMap<>(relationship.columnPairs().get(0).target().name())
                : HashMap.newHashMap(len);
        relationship.columnPairs().forEach(join -> {
            Object value = opSnapshot.get(join.source().name());
            if(value == null) {
                return;
            }
            idMap.put(join.target().name(), value);
        });
        if(idMap.size() != len) {
            return null;
        }
        return new EffectiveOpId(relationship.target().name(), idMap);
    }

    /**
     * The result of sorting a list of operations, with the facts of the operations the sort used, addressed by their
     * index in the list.
     *
     * @param ops       the operations sorted
     * @param sorted    the sorted operations
     * @param cycles    the dependency cycles broken by the sort, each as the indices of the operations depending on
     *                  each other
     * @param deleteDependencies the dependencies of the deletes on the deletes of the rows they reference
     */
    record SortPass(
            List<DbRowOp> ops,
            List<DbRowOp> sorted,
            DbRowOpType[] types,
            EffectiveOpId[] ids,
            Map<EffectiveOpId, int[]> indexById,
            List<int[]> cycles,
            List<DeleteDependency> deleteDependencies) {

        List<List<DbRowOp>> cycleOps() {
            return cycles.stream().map(c -> Arrays.stream(c).mapToObj(ops::get).toList()).toList();
        }
    }

    /**
     * The delete of a row referenced by the FK of another deleted row, waiting for the delete of that row.
     *
     * @param delete       the index of the delete of the referencing row
     * @param parentDelete the index of the delete of the referenced row
     * @param join         the FK
     */
    record DeleteDependency(int delete, int parentDelete, CompiledJoin join) {
    }

    /**
     * An FK of an updated or deleted row, whose value before the commit is not known to the object diff, so the
     * committed row has to be read. For an update, the FK referencing a new parent.
     */
    private record PendingOldParent(int opIndex, CompiledJoin join, EffectiveOpId newParentId) {
    }

    /**
     * Adds the edges between the operations of one sort to the graph.
     */
    private static final class Edges {

        private final List<DbRowOp> dbRows;
        private final DbRowOpType[] types;
        private final Map<EffectiveOpId, int[]> indexById;
        private final DbRowOpGraph graph;
        private final Set<String> tablesWithDeletes;
        private final Set<String> tablesWithReferencedUpdates;
        private final Map<String, Map<CompiledJoin, String>> toOnesByEntity;
        private final Map<DbRowOp, Set<CompiledJoin>> nulledFks;

        // the FKs that need the committed rows, read all together once all the operations are processed
        private final List<PendingOldParent> pendingOldParents;

        // the dependencies of the deletes on the deletes of the rows they reference, to break their cycles
        private final List<DeleteDependency> deleteDependencies;

        Edges(
                List<DbRowOp> dbRows,
                DbRowOpType[] types,
                Map<EffectiveOpId, int[]> indexById,
                DbRowOpGraph graph,
                Set<String> tablesWithDeletes,
                Set<String> tablesWithReferencedUpdates,
                Map<String, Map<CompiledJoin, String>> toOnesByEntity,
                Map<DbRowOp, Set<CompiledJoin>> nulledFks) {

            this.dbRows = dbRows;
            this.types = types;
            this.indexById = indexById;
            this.graph = graph;
            this.tablesWithDeletes = tablesWithDeletes;
            this.tablesWithReferencedUpdates = tablesWithReferencedUpdates;
            this.toOnesByEntity = toOnesByEntity;
            this.nulledFks = nulledFks;
            this.pendingOldParents = new ArrayList<>();
            this.deleteDependencies = new ArrayList<>();
        }

        void addRelationshipEdges(Map<CompiledTable, List<CompiledJoin>> relationships, int opIndex) {
            DbRowOp op = dbRows.get(opIndex);
            List<CompiledJoin> joins = relationships.get(op.getEntity());
            if (joins == null) {
                return;
            }

            switch (types[opIndex]) {
                case INSERT -> {
                    Map<String, Object> values = ((InsertDbRowOp) op).getValues().getSnapshot();
                    for (CompiledJoin join : joins) {
                        addParentEdges(join, opIndex, effectiveIdFor(join, values));
                    }
                }
                case UPDATE -> addUpdateEdges((UpdateDbRowOp) op, joins, opIndex);
                case DELETE -> addDeleteEdges((DeleteDbRowOp) op, joins, opIndex);
            }
        }

        private void addUpdateEdges(UpdateDbRowOp op, List<CompiledJoin> joins, int opIndex) {
            Map<String, Object> values = op.getValues().getSnapshot();
            boolean joinTableRow = op.getChangeId().getEntityName().startsWith(DbPathExp.DB_PREFIX);

            for (CompiledJoin join : joins) {
                EffectiveOpId newParentId = effectiveIdFor(join, values);
                addParentEdges(join, opIndex, newParentId);

                // the parent the FK referenced before the update matters only if the update changes the FK, and the
                // parent table has the operations that depend on the update: deletes, or updates of the referenced
                // columns
                if (joinTableRow || !changesFk(values, join) || !referencesChangedTable(join, true)) {
                    continue;
                }

                Map<String, Object> committedFks = committedFksFromDiff(op, join);
                if (committedFks != null) {
                    addOldParentEdges(join, opIndex, effectiveIdFor(join, committedFks), newParentId);
                } else {
                    pendingOldParents.add(new PendingOldParent(opIndex, join, newParentId));
                }
            }
        }

        private void addDeleteEdges(DeleteDbRowOp op, List<CompiledJoin> joins, int opIndex) {

            // a join table row of a flattened relationship: its id has the FKs, while the object of the op is one
            // of the related objects, and its snapshot is of another table
            Map<String, Object> joinRowFks = op.getChangeId().getEntityName().startsWith(DbPathExp.DB_PREFIX)
                    ? op.getChangeId().getIdSnapshot()
                    : null;
            Set<CompiledJoin> nulled = nulledFks.isEmpty() ? null : nulledFks.get(op);

            for (CompiledJoin join : joins) {

                // a deleted row matters only to the deleted rows it references. Not by an FK set to NULL before
                if (!referencesChangedTable(join, false) || nulled != null && nulled.contains(join)) {
                    continue;
                }

                if (joinRowFks != null) {
                    addParentEdges(join, opIndex, effectiveIdFor(join, joinRowFks));
                    continue;
                }

                Map<String, Object> committedFks = committedFksFromDiff(op, join);
                if (committedFks != null) {
                    addParentEdges(join, opIndex, effectiveIdFor(join, committedFks));
                } else {
                    pendingOldParents.add(new PendingOldParent(opIndex, join, null));
                }
            }
        }

        void addPendingEdges() {
            if (pendingOldParents.isEmpty()) {
                return;
            }

            // flush only ever handles DataContext objects of a single context
            DataContextObjectStore store = ((DataContext) dbRows
                    .get(pendingOldParents.getFirst().opIndex())
                    .getObject()
                    .getObjectContext())
                    .getObjectStore();

            Set<ObjectId> ids = new HashSet<>();
            pendingOldParents.forEach(p -> ids.add(dbRows.get(p.opIndex()).getObject().getObjectId()));

            // the committed state is in the snapshot cache (or the DB), never in the object itself
            Map<ObjectId, DataRow> committedRows = store.getSnapshots(ids);

            for (PendingOldParent pending : pendingOldParents) {
                ObjectId id = dbRows.get(pending.opIndex()).getObject().getObjectId();
                Map<String, Object> fks = committedFks(
                        committedRows.get(id),
                        store.getFlattenedIds(id),
                        pending.join());
                addOldParentEdges(pending.join(), pending.opIndex(), effectiveIdFor(pending.join(), fks),
                        pending.newParentId());
            }
        }

        private void addOldParentEdges(
                CompiledJoin join,
                int opIndex,
                EffectiveOpId oldParentId,
                EffectiveOpId newParentId) {

            if (oldParentId != null && !oldParentId.equals(newParentId)) {
                addParentEdges(join, opIndex, oldParentId);
            }
        }

        private Map<String, Object> committedFksFromDiff(DbRowOp op, CompiledJoin join) {

            // null means the diff doesn't know the committed target of the FK without a DB query
            ObjectId id = op.getObject().getObjectId();

            Map<CompiledJoin, String> toOnes = toOnesByEntity.get(id.getEntityName());
            String relationship = toOnes != null ? toOnes.get(join) : null;
            if (relationship == null) {
                return null;
            }

            ObjectDiff diff = op.getObject().getObjectContext().getObjectStore().getChanges(id);
            if (diff == null || !diff.isArcSnapshotResolved(relationship)) {
                return null;
            }

            ObjectId target = diff.getArcSnapshotValue(relationship);
            if (target == null) {
                return Map.of();
            }

            // only a target id with the referenced columns tells the FK values
            Map<String, Object> targetId = target.getIdSnapshot();
            Map<String, Object> fks = HashMap.newHashMap(join.columnPairs().size());
            for (CompiledColumnPair pair : join.columnPairs()) {
                Object value = targetId.get(pair.target().name());
                if (value == null) {
                    return null;
                }
                fks.put(pair.source().name(), value);
            }
            return fks;
        }

        private static Map<String, Object> committedFks(
                DataRow committedRow,
                Collection<ObjectId> flattenedIds,
                CompiledJoin join) {

            if (committedRow == null) {
                return Map.of();
            }

            int joinSize = join.columnPairs().size();
            Map<String, Object> fks = joinSize == 1
                    ? new SingleEntryMap<>(join.columnPairs().get(0).source().name())
                    : HashMap.newHashMap(joinSize);

            join.columnPairs().forEach(pair -> {
                Object value = committedRow.get(pair.source().name());
                if (value != null) {
                    fks.put(pair.source().name(), value);
                }
            });

            // the ids of the rows of the flattened attributes, mapped from target to source
            flattenedIds.forEach(flattenedId -> {
                Map<String, Object> idSnapshot = flattenedId.getIdSnapshot();
                join.columnPairs().forEach(pair -> {
                    Object value = idSnapshot.get(pair.target().name());
                    if (value != null) {
                        fks.put(pair.source().name(), value);
                    }
                });
            });

            return fks;
        }

        private boolean referencesChangedTable(CompiledJoin join, boolean includeReferencedUpdates) {
            String target = join.target().name();
            return tablesWithDeletes.contains(target)
                    || includeReferencedUpdates && tablesWithReferencedUpdates.contains(target);
        }

        private static boolean changesFk(Map<String, Object> values, CompiledJoin join) {
            for (CompiledColumnPair pair : join.columnPairs()) {
                if (!values.containsKey(pair.source().name())) {
                    return false;
                }
            }
            return true;
        }

        private void addParentEdges(CompiledJoin join, int opIndex, EffectiveOpId parentId) {
            if (parentId == null) {
                return;
            }

            int[] parentOps = indexById.get(parentId);
            if (parentOps == null) {
                return;
            }

            for (int parentIndex : parentOps) {
                if (parentIndex != opIndex) {
                    addRelationshipEdge(join, opIndex, parentIndex);
                }
            }
        }

        private void addRelationshipEdge(CompiledJoin relationship, int opIndex, int parentIndex) {

            DbRowOpType parentOpType = types[parentIndex];

            // 1. Our insert can depend on others insert or update
            // 2. Our update can depend on others insert or update, or others delete can depend on our update
            // 3. Others delete can depend on our delete
            switch (types[opIndex]) {
                case INSERT -> {
                    switch (parentOpType) {
                        case INSERT -> graph.add(opIndex, parentIndex);

                        // the referenced row exists, so the insert waits only for an update of the referenced columns
                        case UPDATE -> {
                            if (fkReferenceUpdated((UpdateDbRowOp) dbRows.get(parentIndex), relationship)) {
                                graph.add(opIndex, parentIndex);
                            }
                        }
                        case DELETE -> {
                        }
                    }
                }
                case UPDATE -> {
                    switch (parentOpType) {
                        case INSERT -> graph.add(opIndex, parentIndex);
                        case DELETE -> graph.add(parentIndex, opIndex);
                        case UPDATE -> {
                            // the update of the referenced columns of the parent must wait for the update moving the
                            // FK away from them. Note that a meaningful PK change is only supported this way, i.e. with
                            // the FKs referencing it set to null or to another parent: no update is generated for the
                            // FKs to follow the PK
                            if (fkReferenceUpdated((UpdateDbRowOp) dbRows.get(parentIndex), relationship)) {
                                graph.add(parentIndex, opIndex);
                            }
                        }
                    }
                }
                case DELETE -> {
                    if (parentOpType == DbRowOpType.DELETE) {
                        graph.add(parentIndex, opIndex);
                        deleteDependencies.add(new DeleteDependency(opIndex, parentIndex, relationship));
                    }
                }
            }
        }

        void addMeaningfulIdEdges(EffectiveOpId[] ids, int opIndex) {

            // an insert or an update of a row runs after the delete of the row with the same id. Two deletes of the
            // same row (e.g. of an object and of the row of its flattened attributes) don't depend on each other
            if (types[opIndex] == DbRowOpType.DELETE) {
                return;
            }

            for (int sameIdIndex : indexById.get(ids[opIndex])) {
                if (sameIdIndex == opIndex) {
                    continue;
                }

                switch (types[sameIdIndex]) {

                    // an update of a deleted row, i.e. setting its FK to NULL to break a cycle of the deletes, runs
                    // before the delete
                    case DELETE -> {
                        if (types[opIndex] == DbRowOpType.UPDATE && sameRow(opIndex, sameIdIndex)) {
                            graph.add(sameIdIndex, opIndex);
                        } else {
                            graph.add(opIndex, sameIdIndex);
                        }
                    }

                    // an insert also runs after the update changing the PK of the row away from the id, as the id of
                    // the update is the one before the change
                    case UPDATE -> {
                        if (types[opIndex] == DbRowOpType.INSERT
                                && changesPk((UpdateDbRowOp) dbRows.get(sameIdIndex))) {
                            graph.add(opIndex, sameIdIndex);
                        }
                    }
                    // an update of a row inserted in the same commit, i.e. of an insert split to break a cycle, runs
                    // after the insert. Not the update of another row, whose PK change frees the id of the insert
                    case INSERT -> {
                        if (types[opIndex] == DbRowOpType.UPDATE && sameRow(opIndex, sameIdIndex)) {
                            graph.add(opIndex, sameIdIndex);
                        }
                    }
                }
            }
        }

        private boolean sameRow(int opIndex1, int opIndex2) {
            return dbRows.get(opIndex1).getChangeId().equals(dbRows.get(opIndex2).getChangeId());
        }

        private static boolean changesPk(UpdateDbRowOp op) {
            List<CompiledColumn> pk = op.getEntity().primaryKey();
            for (CompiledColumn column : op.getValues().getUpdatedAttributes()) {
                if (pk.contains(column)) {
                    return true;
                }
            }
            return false;
        }
    }
}
