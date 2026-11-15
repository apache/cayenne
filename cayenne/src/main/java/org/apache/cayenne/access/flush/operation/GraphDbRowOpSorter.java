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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;
import org.apache.cayenne.util.SingleEntryMap;

/**
 * DB operation sorter that orders the operations by the dependencies between their rows.
 *
 * @since 6.0
 */
public class GraphDbRowOpSorter implements DbRowOpSorter {

    private final Provider<DataDomain> dataDomainProvider;

    // the FK-holding relationships by table of the mapping they were indexed for;
    // rebuilt when the domain mapping is replaced
    private volatile RelationshipIndex relationships;

    public GraphDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    private RelationshipIndex initDataSync() {
        CompiledMapping mapping = dataDomainProvider.get().getMapping();
        RelationshipIndex localRelationships = relationships;
        if(localRelationships == null || localRelationships.mapping != mapping) {
            synchronized (this) {
                localRelationships = relationships;
                if(localRelationships == null || localRelationships.mapping != mapping) {
                    localRelationships = initDataNoSync(mapping);
                    relationships = localRelationships;
                }
            }
        }
        return localRelationships;
    }

    /**
     * Init all the data we need for faster processing actual rows.
     */
    private RelationshipIndex initDataNoSync(CompiledMapping mapping) {
        Map<CompiledTable, List<CompiledJoin>> byEntity = new HashMap<>();
        Set<CompiledColumn> referencedColumns = new HashSet<>();
        mapping.schema().tables().forEach(entity ->
            entity.joins().forEach(dbRelationship -> {
                if(!dbRelationship.isForeignKey()) {
                    // the dependencies are found from the FK side, every FK has its FK side join on its own table
                    return;
                }
                byEntity
                        .computeIfAbsent(entity, e -> new ArrayList<>())
                        .add(dbRelationship);
                dbRelationship.columnPairs().forEach(pair -> referencedColumns.add(pair.target()));
            })
        );

        // the to-one relationships mapped to a single FK, whose committed targets the object diffs may know
        Map<String, Map<CompiledJoin, String>> toOnesByEntity = new HashMap<>();
        for (CompiledEntity entity : mapping.entities()) {
            for (CompiledRelationship relationship : entity.relationships()) {
                if (relationship instanceof CompiledToOneRelationship
                        && relationship.joins().size() == 1
                        && relationship.joins().getFirst().isForeignKey()) {
                    toOnesByEntity
                            .computeIfAbsent(entity.name(), e -> new HashMap<>())
                            .put(relationship.joins().getFirst(), relationship.name());
                }
            }
        }

        return new RelationshipIndex(
                mapping,
                byEntity,
                referencedColumns,
                toOnesByEntity,
                new BatchOrder(mapping));
    }

    @Override
    public List<DbRowOp> sort(List<DbRowOp> dbRows) {
        // lazy init Cayenne model data
        RelationshipIndex relationships = initDataSync();

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
                    if (updatesReferencedColumn((UpdateDbRowOp) op, relationships.referencedColumns)) {
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
                relationships.toOnesByEntity);

        for (int i = 0; i < size; i++) {
            edges.addRelationshipEdges(relationships.byEntity, i);
            if (hasMeaningfulIds) {
                edges.addMeaningfulIdEdges(ids, i);
            }
            edges.graph.add(i);
        }
        edges.addPendingEdges();

        // sort
        return edges.graph.topSort(relationships.batchOrder);
    }

    private static boolean updatesReferencedColumn(UpdateDbRowOp op, Set<CompiledColumn> referencedColumns) {
        for (CompiledColumn column : op.getValues().getUpdatedAttributes()) {
            if (referencedColumns.contains(column)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fkReferenceUpdated(UpdateDbRowOp parentOp, CompiledJoin join) {
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

    private static EffectiveOpId effectiveIdFor(CompiledJoin relationship, Map<String, Object> opSnapshot) {
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

    private record RelationshipIndex(
            CompiledMapping mapping,
            Map<CompiledTable, List<CompiledJoin>> byEntity,
            Set<CompiledColumn> referencedColumns,
            Map<String, Map<CompiledJoin, String>> toOnesByEntity,

            // The order of the operations not depending on each other, by the operation type and the table. Besides
            // the order itself, it groups the operations that can run as one batch, and the graph sort keeps each
            // group together as much as the dependencies allow. This is not cosmetic: the dependencies alone leave
            // the independent operations in any order, and an order mixing the tables breaks the batches up, running
            // a statement per row instead of a batch per table.
            Comparator<DbRowOp> batchOrder) {
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

        // the FKs that need the committed rows, read all together once all the operations are processed
        private final List<PendingOldParent> pendingOldParents;

        Edges(
                List<DbRowOp> dbRows,
                DbRowOpType[] types,
                Map<EffectiveOpId, int[]> indexById,
                DbRowOpGraph graph,
                Set<String> tablesWithDeletes,
                Set<String> tablesWithReferencedUpdates,
                Map<String, Map<CompiledJoin, String>> toOnesByEntity) {

            this.dbRows = dbRows;
            this.types = types;
            this.indexById = indexById;
            this.graph = graph;
            this.tablesWithDeletes = tablesWithDeletes;
            this.tablesWithReferencedUpdates = tablesWithReferencedUpdates;
            this.toOnesByEntity = toOnesByEntity;
            this.pendingOldParents = new ArrayList<>();
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

            for (CompiledJoin join : joins) {

                // a deleted row matters only to the deleted rows it references
                if (!referencesChangedTable(join, false)) {
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
                    if (parentOpType != DbRowOpType.DELETE) {
                        graph.add(opIndex, parentIndex);
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
                if (sameIdIndex != opIndex && types[sameIdIndex] == DbRowOpType.DELETE) {
                    graph.add(opIndex, sameIdIndex);
                }
            }
        }
    }
}
