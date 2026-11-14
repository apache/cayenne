/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataContextObjectStore;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.flush.EffectiveOpId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.util.SingleEntryMap;

/**
 * Db operation sorted that builds dependency graph and uses topological sort to get final order.
 * This in general slower than {@link DefaultDbRowOpSorter} but can handle more edge cases (like multiple meaningful PKs/FKs).
 *
 * TODO: possible optimizations could be optional logic parts (e.g. detecting effective id intersections,
 *       reflexive dependencies, etc.)
 *
 * @since 4.2
 */
public class GraphBasedDbRowOpSorter implements DbRowOpSorter {

    private final DbRowOpTypeVisitor rowOpTypeVisitor = new DbRowOpTypeVisitor();
    private final Provider<DataDomain> dataDomainProvider;

    // the FK-holding relationships by table of the mapping they were indexed for;
    // rebuilt when the domain mapping is replaced
    private volatile RelationshipIndex relationships;

    public GraphBasedDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    private Map<CompiledTable, List<CompiledJoin>> initDataSync() {
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
        return localRelationships.byEntity;
    }

    /**
     * Init all the data we need for faster processing actual rows.
     */
    private RelationshipIndex initDataNoSync(CompiledMapping mapping) {
        Map<CompiledTable, List<CompiledJoin>> byEntity = new HashMap<>();
        mapping.schema().tables().forEach(entity ->
            entity.joins().forEach(dbRelationship -> {
                if(!dbRelationship.isForeignKey()) {
                    // TODO: can we ignore all of these relationships?
                    return;
                }
                byEntity
                        .computeIfAbsent(entity, e -> new ArrayList<>())
                        .add(dbRelationship);
            })
        );
        return new RelationshipIndex(mapping, byEntity);
    }

    @Override
    public List<DbRowOp> sort(List<DbRowOp> dbRows) {
        // lazy init Cayenne model data
        Map<CompiledTable, List<CompiledJoin>> relationships = initDataSync();

        // ops are addressed by their index in the list from here on
        int size = dbRows.size();
        DbRowOpType[] types = new DbRowOpType[size];
        EffectiveOpId[] ids = new EffectiveOpId[size];

        // build op index by ID. Multiple ops per ID are only possible with meaningful IDs, so the value arrays are
        // almost always of size 1
        Map<EffectiveOpId, int[]> indexById = HashMap.newHashMap(size);
        for (int i = 0; i < size; i++) {
            DbRowOp op = dbRows.get(i);
            types[i] = op.accept(rowOpTypeVisitor);
            ids[i] = effectiveIdFor(op);

            int[] sameIdOps = indexById.putIfAbsent(ids[i], new int[]{i});
            if (sameIdOps != null) {
                int[] expanded = Arrays.copyOf(sameIdOps, sameIdOps.length + 1);
                expanded[sameIdOps.length] = i;
                indexById.put(ids[i], expanded);
            }
        }
        boolean hasMeaningfulIds = indexById.size() != size;

        // build ops dependency graph
        DbRowOpGraph graph = new DbRowOpGraph(dbRows);
        for (int i = 0; i < size; i++) {
            processRelationships(relationships, indexById, types, graph, dbRows, i);
            if (hasMeaningfulIds) {
                processMeaningfulIds(indexById, types, ids, graph, i);
            }
            graph.add(i);
        }

        // sort
        return graph.topSort();
    }

    private void processRelationships(
            Map<CompiledTable, List<CompiledJoin>> relationships,
            Map<EffectiveOpId, int[]> indexById,
            DbRowOpType[] types,
            DbRowOpGraph graph,
            List<DbRowOp> dbRows,
            int opIndex) {

        DbRowOp op = dbRows.get(opIndex);
        List<CompiledJoin> joins = relationships.get(op.getEntity());
        if (joins == null) {
            return;
        }

        DbRowOpType opType = types[opIndex];
        for (CompiledJoin relationship : joins) {
            List<Map<String, Object>> parentSnapshots = op.accept(new DbRowOpSnapshotVisitor(relationship));
            EffectiveOpId prevParentId = null;
            for (Map<String, Object> parentSnapshot : parentSnapshots) {
                EffectiveOpId parentId = effectiveIdFor(relationship, parentSnapshot);

                // there are at most 2 snapshots (new and old state of an UPDATE), so checking for a duplicate ID
                // against the previous one is enough
                if (parentId == null || parentId.equals(prevParentId)) {
                    continue;
                }
                prevParentId = parentId;

                int[] parentOps = indexById.get(parentId);
                if (parentOps == null) {
                    continue;
                }

                for (int parentIndex : parentOps) {
                    if (parentIndex != opIndex) {
                        addRelationshipEdge(graph, dbRows, relationship, opIndex, opType, parentIndex,
                                types[parentIndex]);
                    }
                }
            }
        }
    }

    private static void addRelationshipEdge(
            DbRowOpGraph graph,
            List<DbRowOp> dbRows,
            CompiledJoin relationship,
            int opIndex,
            DbRowOpType opType,
            int parentIndex,
            DbRowOpType parentOpType) {

        // 1. Our insert can depend on others insert or update
        // 2. Our update can depend on others insert or update, or others delete can depend on our update
        // 3. Others delete can depend on our delete
        switch (opType) {
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
                            //       if the meaningful PK is updated we got more places to cover, before fixing this
                            //       logic namely:
                            //          - no update would be generated for any dependent entities for the master PK
                            //            update
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

    private static void processMeaningfulIds(
            Map<EffectiveOpId, int[]> indexById,
            DbRowOpType[] types,
            EffectiveOpId[] ids,
            DbRowOpGraph graph,
            int opIndex) {

        // get graph edges from same ID operations, for such operations delete depends on other operations
        for (int sameIdIndex : indexById.get(ids[opIndex])) {
            if (sameIdIndex != opIndex && types[sameIdIndex] == DbRowOpType.DELETE) {
                graph.add(opIndex, sameIdIndex);
            }
        }
    }

    private EffectiveOpId effectiveIdFor(DbRowOp op) {
        return new EffectiveOpId(op.getEntity().name(), op.getChangeId());
    }

    private EffectiveOpId effectiveIdFor(CompiledJoin relationship, Map<String, Object> opSnapshot) {
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
            Map<CompiledTable, List<CompiledJoin>> byEntity) {
    }

    private static class DbRowOpSnapshotVisitor implements DbRowOpVisitor<List<Map<String, Object>>> {

        private final CompiledJoin join;

        private DbRowOpSnapshotVisitor(CompiledJoin join) {
            this.join = join;
        }

        @Override
        public List<Map<String, Object>> visitInsert(InsertDbRowOp dbRow) {
            return Collections.singletonList(dbRow.getValues().getSnapshot());
        }

        @Override
        public List<Map<String, Object>> visitUpdate(UpdateDbRowOp dbRow) {
            List<Map<String, Object>> result;
            Map<String, Object> updatedSnapshot = dbRow.getValues().getSnapshot();
            if(dbRow.getChangeId().getEntityName().startsWith(DbPathExp.DB_PREFIX)) {
                return Collections.singletonList(updatedSnapshot);
            }
            result = new ArrayList<>(2);
            // get updated state from operation
            result.add(updatedSnapshot);
            // get previous state from cache, but only for update attributes
            Map<String, Object> cachedSnapshot = getCachedSnapshot(dbRow.getObject());
            cachedSnapshot.entrySet().forEach(entry -> {
                if(!updatedSnapshot.containsKey(entry.getKey())) {
                    entry.setValue(null);
                }
            });
            result.add(cachedSnapshot);
            return result;
        }

        @Override
        public List<Map<String, Object>> visitDelete(DeleteDbRowOp dbRow) {
            Map<String, Object> cachedSnapshot = getCachedSnapshot(dbRow.getObject());
            return Collections.singletonList(cachedSnapshot);
        }

        private Map<String, Object> getCachedSnapshot(Persistent object) {
            // flush only ever handles DataContext objects
            DataContextObjectStore store = ((DataContext) object.getObjectContext()).getObjectStore();

            // the committed state is in the snapshot cache (or the DB), never in the object itself
            DataRow dataRow = store.getSnapshot(object.getObjectId());
            if (dataRow == null) {
                return Collections.emptyMap();
            }

            // copy snapshot as we can modify it later
            int joinSize = join.columnPairs().size();
            Map<String, Object> snapshot = joinSize == 1
                    ? new SingleEntryMap<>(join.columnPairs().get(0).source().name())
                    : HashMap.newHashMap(joinSize);
            join.columnPairs().forEach(join -> {
                Object value = dataRow.get(join.source().name());
                if(value != null) {
                    snapshot.put(join.source().name(), value);
                }
            });

            // check and merge flattened IDs snapshots
            store.getFlattenedIds(object.getObjectId()).forEach(flattenedId -> {
                // map values of flattened ids from target to source
                Map<String, Object> idSnapshot = flattenedId.getIdSnapshot();
                join.columnPairs().forEach(join -> {
                    Object value = idSnapshot.get(join.target().name());
                    if(value != null) {
                        snapshot.put(join.source().name(), value);
                    }
                });
            });

            return snapshot;
        }
    }

    private static class DbRowOpTypeVisitor implements DbRowOpVisitor<DbRowOpType> {
        @Override
        public DbRowOpType visitDelete(DeleteDbRowOp dbRow) {
            return DbRowOpType.DELETE;
        }

        @Override
        public DbRowOpType visitInsert(InsertDbRowOp dbRow) {
            return DbRowOpType.INSERT;
        }

        @Override
        public DbRowOpType visitUpdate(UpdateDbRowOp dbRow) {
            return DbRowOpType.UPDATE;
        }
    }
}
