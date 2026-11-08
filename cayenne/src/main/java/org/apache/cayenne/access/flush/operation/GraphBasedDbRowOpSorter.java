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

        // build index op by ID
        Map<EffectiveOpId, List<DbRowOp>> indexById = HashMap.newHashMap(dbRows.size());
        dbRows.forEach(op -> indexById
                .computeIfAbsent(effectiveIdFor(op), id -> new ArrayList<>(1))
                .add(op)
        );
        boolean hasMeaningfulIds = indexById.size() != dbRows.size();

        // build ops dependency graph
        DbRowOpGraph graph = new DbRowOpGraph();
        dbRows.forEach(op -> {
            processRelationships(relationships, indexById, graph, op);
            if(hasMeaningfulIds) {
                processMeaningfulIds(indexById, graph, op);
            }
            graph.add(op);
        });

        // sort
        return graph.topSort();
    }

    private void processRelationships(
            Map<CompiledTable, List<CompiledJoin>> relationships,
            Map<EffectiveOpId, List<DbRowOp>> indexByDbId,
            DbRowOpGraph graph,
            DbRowOp op) {
        // get graph edges for reflexive relationships
        DbRowOpType opType = op.accept(rowOpTypeVisitor);
        relationships.getOrDefault(op.getEntity(), Collections.emptyList()).forEach(relationship ->
            getParentsOpId(op, relationship).forEach((parentOpId, snapshot) ->
                indexByDbId.getOrDefault(parentOpId, Collections.emptyList()).forEach(parentOp -> {
                    if(op == parentOp) {
                        return;
                    }
                    DbRowOpType parentOpType = parentOp.accept(rowOpTypeVisitor);
                    // 1. Our insert can depend on others insert or update
                    // 2. Our update can depend on others insert or update, or others delete can depend on our update
                    // 3. Others delete can depend on our delete
                    switch (opType) {
                        case INSERT:
                            if(parentOpType != DbRowOpType.DELETE) {
                                graph.add(op, parentOp);
                            }
                            break;
                        case UPDATE:
                            switch (parentOpType) {
                                case INSERT:
                                    graph.add(op, parentOp);
                                    break;
                                case DELETE:
                                    graph.add(parentOp, op);
                                    break;
                                case UPDATE:
                                    // Update will only depend on the operations where FK could be affected
                                    Boolean fkReferenceUpdated = parentOp.accept(new FkUpdateChecker(relationship));
                                    if (fkReferenceUpdated == Boolean.TRUE) {
                                        // TODO: this sorting logic works only for the setting relationship to null case
                                        //       if the meaningful PK is updated we got more places to cover, before fixing this logic
                                        //       namely:
                                        //          - no update would be generated for any dependent entities for the master PK update
                                        //          - update would fail with constraint violation even if it is generated
                                        graph.add(parentOp, op);
                                    }
                                    break;
                            }
                            break;
                        case DELETE:
                            if(parentOpType == DbRowOpType.DELETE) {
                                graph.add(parentOp, op);
                            }
                    }
                })
            )
        );
    }

    private void processMeaningfulIds(Map<EffectiveOpId, List<DbRowOp>> indexById, DbRowOpGraph graph, DbRowOp op) {
        // get graph edges from same ID operations, for such operations delete depends on other operations
        indexById.get(effectiveIdFor(op)).forEach(sameIdOp -> {
            if(op == sameIdOp) {
                return;
            }
            DbRowOpType sameIdOpType = sameIdOp.accept(rowOpTypeVisitor);
            if(sameIdOpType == DbRowOpType.DELETE) {
                graph.add(op, sameIdOp);
            }
        });
    }

    private Map<EffectiveOpId, Map<String, Object>> getParentsOpId(DbRowOp op, CompiledJoin join) {
        List<Map<String, Object>> parentIdSnapshots = op.accept(new DbRowOpSnapshotVisitor(join));
        if(parentIdSnapshots.size() == 1) {
            EffectiveOpId id = effectiveIdFor(join, parentIdSnapshots.get(0));
            if(id != null) {
                return Collections.singletonMap(id, parentIdSnapshots.get(0));
            } else {
                return Collections.emptyMap();
            }
        } else {
            Map<EffectiveOpId, Map<String, Object>> effectiveOpIds = HashMap.newHashMap(parentIdSnapshots.size());
            parentIdSnapshots.forEach(snapshot -> {
                EffectiveOpId id = this.effectiveIdFor(join, snapshot);
                if(id != null) {
                    effectiveOpIds.put(id, snapshot);
                }
            });
            return effectiveOpIds;
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

    private static class FkUpdateChecker implements DbRowOpVisitor<Boolean> {
        private final CompiledJoin join;

        public FkUpdateChecker(CompiledJoin join) {
            this.join = join;
        }

        @Override
        public Boolean visitUpdate(UpdateDbRowOp dbRow) {
            for(CompiledColumnPair join : join.columnPairs()) {
                for (CompiledColumn column : dbRow.getValues().getUpdatedAttributes()) {
                    if (column.name().equals(join.target().name())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
