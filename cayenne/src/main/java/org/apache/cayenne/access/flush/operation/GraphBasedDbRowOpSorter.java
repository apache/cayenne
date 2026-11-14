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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataContextObjectStore;
import org.apache.cayenne.access.DataDomain;
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

/**
 * Db operation sorted that builds dependency graph and uses topological sort to get final order.
 * This in general slower than {@link DefaultDbRowOpSorter} but can handle more edge cases (like multiple meaningful
 * PKs/FKs).
 *
 * <p>The FKs of the inserted and updated rows are taken from the operations. The FKs the updated and deleted rows had
 * before the commit are taken from the committed snapshots, that may need a DB query when not cached, so they are
 * only read when they can produce a dependency: for the FKs referencing the tables with deletes, or with updates of
 * the referenced columns, in the same commit.
 *
 * @since 4.2
 */
public class GraphBasedDbRowOpSorter implements DbRowOpSorter {

    private final Provider<DataDomain> dataDomainProvider;

    // the FK-holding relationships by table of the mapping they were indexed for;
    // rebuilt when the domain mapping is replaced
    private volatile RelationshipIndex relationships;

    public GraphBasedDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
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
                    // TODO: can we ignore all of these relationships?
                    return;
                }
                byEntity
                        .computeIfAbsent(entity, e -> new ArrayList<>())
                        .add(dbRelationship);
                dbRelationship.columnPairs().forEach(pair -> referencedColumns.add(pair.target()));
            })
        );
        return new RelationshipIndex(mapping, byEntity, referencedColumns);
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
                tablesWithReferencedUpdates);

        for (int i = 0; i < size; i++) {
            edges.addRelationshipEdges(relationships.byEntity, i);
            if (hasMeaningfulIds) {
                edges.addMeaningfulIdEdges(ids, i);
            }
            edges.graph.add(i);
        }

        // sort
        return edges.graph.topSort();
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
            String targetName = pair.target().name();
            for (CompiledColumn column : updatedColumns) {
                if (column.name().equals(targetName)) {
                    return true;
                }
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
            Set<CompiledColumn> referencedColumns) {
    }

    /**
     * Adds the edges between the operations of one sort to the graph.
     */
    private record Edges(
            List<DbRowOp> dbRows,
            DbRowOpType[] types,
            Map<EffectiveOpId, int[]> indexById,
            DbRowOpGraph graph,
            Set<String> tablesWithDeletes,
            Set<String> tablesWithReferencedUpdates) {

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

            // read on first need
            CommittedRow committedRow = null;

            for (CompiledJoin join : joins) {
                EffectiveOpId newParentId = effectiveIdFor(join, values);
                addParentEdges(join, opIndex, newParentId);

                // the parent the FK referenced before the update matters only if the update changes the FK, and the
                // parent table has the operations that depend on the update: deletes, or updates of the referenced
                // columns
                if (joinTableRow || !changesFk(values, join) || !referencesChangedTable(join, true)) {
                    continue;
                }

                if (committedRow == null) {
                    committedRow = new CommittedRow(op.getObject());
                }

                EffectiveOpId oldParentId = effectiveIdFor(join, committedRow.fks(join));
                if (oldParentId != null && !oldParentId.equals(newParentId)) {
                    addParentEdges(join, opIndex, oldParentId);
                }
            }
        }

        private void addDeleteEdges(DeleteDbRowOp op, List<CompiledJoin> joins, int opIndex) {

            // a join table row of a flattened relationship: its id has the FKs, while the object of the op is one
            // of the related objects, and its snapshot is of another table
            Map<String, Object> joinRowFks = op.getChangeId().getEntityName().startsWith(DbPathExp.DB_PREFIX)
                    ? op.getChangeId().getIdSnapshot()
                    : null;

            // read on first need
            CommittedRow committedRow = null;

            for (CompiledJoin join : joins) {

                // a deleted row matters only to the deleted rows it references
                if (!referencesChangedTable(join, false)) {
                    continue;
                }

                Map<String, Object> fks;
                if (joinRowFks != null) {
                    fks = joinRowFks;
                } else {
                    if (committedRow == null) {
                        committedRow = new CommittedRow(op.getObject());
                    }
                    fks = committedRow.fks(join);
                }

                addParentEdges(join, opIndex, effectiveIdFor(join, fks));
            }
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
                            // Update will only depend on the operations where FK could be affected
                            if (fkReferenceUpdated((UpdateDbRowOp) dbRows.get(parentIndex), relationship)) {
                                // TODO: this sorting logic works only for the setting relationship to null case
                                //       if the meaningful PK is updated we got more places to cover, before fixing
                                //       this logic namely:
                                //          - no update would be generated for any dependent entities for the master
                                //            PK update
                                //          - update would fail with constraint violation even if it is generated
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
            // get graph edges from same ID operations, for such operations delete depends on other operations
            for (int sameIdIndex : indexById.get(ids[opIndex])) {
                if (sameIdIndex != opIndex && types[sameIdIndex] == DbRowOpType.DELETE) {
                    graph.add(opIndex, sameIdIndex);
                }
            }
        }
    }

    /**
     * The committed state of the row of an updated or deleted object, read once, on first need.
     */
    private static final class CommittedRow {

        private final Persistent object;
        private boolean read;
        private DataRow dataRow;
        private Collection<ObjectId> flattenedIds;

        CommittedRow(Persistent object) {
            this.object = object;
        }

        /**
         * Returns the committed values of the FK columns of the join, by column name.
         */
        Map<String, Object> fks(CompiledJoin join) {
            read();

            if (dataRow == null) {
                return Map.of();
            }

            int joinSize = join.columnPairs().size();
            Map<String, Object> fks = joinSize == 1
                    ? new SingleEntryMap<>(join.columnPairs().get(0).source().name())
                    : HashMap.newHashMap(joinSize);

            join.columnPairs().forEach(pair -> {
                Object value = dataRow.get(pair.source().name());
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

        private void read() {
            if (read) {
                return;
            }

            read = true;

            // flush only ever handles DataContext objects
            DataContextObjectStore store = ((DataContext) object.getObjectContext()).getObjectStore();

            // the committed state is in the snapshot cache (or the DB), never in the object itself
            dataRow = store.getSnapshot(object.getObjectId());
            flattenedIds = store.getFlattenedIds(object.getObjectId());
        }
    }
}
