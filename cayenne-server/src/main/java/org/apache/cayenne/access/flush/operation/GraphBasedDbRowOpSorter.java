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
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.flush.EffectiveOpId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ObjectIdQuery;
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

    private volatile Map<DbEntity, List<DbRelationship>> relationships;

    public GraphBasedDbRowOpSorter(@Inject Provider<DataDomain> dataDomainProvider) {
        this.dataDomainProvider = dataDomainProvider;
    }

    private void initDataSync() {
        Map<DbEntity, List<DbRelationship>> localRelationships = relationships;
        if(localRelationships == null) {
            synchronized (this) {
                localRelationships = relationships;
                if(localRelationships == null) {
                    initDataNoSync();
                }
            }
        }
    }

    /**
     * Init all the data we need for faster processing actual rows.
     */
    private void initDataNoSync() {
        relationships = new HashMap<>();
        EntityResolver resolver = dataDomainProvider.get().getEntityResolver();

        resolver.getDbEntities().forEach(entity ->
            entity.getRelationships().forEach(dbRelationship -> {
                if(dbRelationship.isToMany() || !dbRelationship.isToPK() || dbRelationship.isToDependentPK()) {
                    // TODO: can we ignore all of these relationships?
                    return;
                }

                relationships
                        .computeIfAbsent(entity, e -> new ArrayList<>())
                        .add(dbRelationship);
            })
        );
    }

    @Override
    public List<DbRowOp> sort(List<DbRowOp> dbRows) {
        // lazy init Cayenne model data
        initDataSync();

        // build index op by ID
        Map<EffectiveOpId, List<DbRowOp>> indexById = new HashMap<>(dbRows.size());
        dbRows.forEach(op -> indexById
                .computeIfAbsent(effectiveIdFor(op), id -> new ArrayList<>(1))
                .add(op)
        );
        boolean hasMeaningfulIds = indexById.size() != dbRows.size();

        // build ops dependency graph
        DbRowOpGraph graph = new DbRowOpGraph();
        dbRows.forEach(op -> {
            processRelationships(indexById, graph, op);
            if(hasMeaningfulIds) {
                processMeaningfulIds(indexById, graph, op);
            }
            graph.add(op);
        });

        // sort
        return graph.topSort();
    }

    private void processRelationships(Map<EffectiveOpId, List<DbRowOp>> indexByDbId, DbRowOpGraph graph, DbRowOp op) {
        // get graph edges for reflexive relationships
        DbRowOpType opType = op.accept(rowOpTypeVisitor);
        relationships.getOrDefault(op.getEntity(), Collections.emptyList()).forEach(relationship ->
            getParentsOpId(op, relationship).forEach(parentOpId ->
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
                            if(parentOpType != DbRowOpType.DELETE) {
                                graph.add(op, parentOp);
                            } else {
                                graph.add(parentOp, op);
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

    private List<EffectiveOpId> getParentsOpId(DbRowOp op, DbRelationship relationship) {
        List<Map<String, Object>> parentIdSnapshots = op.accept(new DbRowOpSnapshotVisitor(relationship));
        if(parentIdSnapshots.size() == 1) {
            EffectiveOpId id = effectiveIdFor(relationship, parentIdSnapshots.get(0));
            if(id != null) {
                return Collections.singletonList(id);
            } else {
                return Collections.emptyList();
            }
        } else {
            List<EffectiveOpId> effectiveOpIds = new ArrayList<>(parentIdSnapshots.size());
            parentIdSnapshots.forEach(snapshot -> {
                EffectiveOpId id = this.effectiveIdFor(relationship, snapshot);
                if(id != null) {
                    effectiveOpIds.add(id);
                }
            });
            return effectiveOpIds;
        }
    }

    private EffectiveOpId effectiveIdFor(DbRowOp op) {
        return new EffectiveOpId(op.getEntity().getName(), op.getChangeId());
    }

    private EffectiveOpId effectiveIdFor(DbRelationship relationship, Map<String, Object> opSnapshot) {
        int len = relationship.getJoins().size();
        Map<String, Object> idMap = len == 1
                ? new SingleEntryMap<>(relationship.getJoins().get(0).getTargetName())
                : new HashMap<>(len);
        relationship.getJoins().forEach(join -> {
            Object value = opSnapshot.get(join.getSourceName());
            if(value == null) {
                return;
            }
            idMap.put(join.getTargetName(), value);
        });
        if(idMap.size() != len) {
            return null;
        }
        return new EffectiveOpId(relationship.getTargetEntityName(), idMap);
    }

    private static class DbRowOpSnapshotVisitor implements DbRowOpVisitor<List<Map<String, Object>>> {

        private final DbRelationship relationship;

        private DbRowOpSnapshotVisitor(DbRelationship relationship) {
            this.relationship = relationship;
        }

        @Override
        public List<Map<String, Object>> visitInsert(InsertDbRowOp dbRow) {
            return Collections.singletonList(dbRow.getValues().getSnapshot());
        }

        @Override
        public List<Map<String, Object>> visitUpdate(UpdateDbRowOp dbRow) {
            List<Map<String, Object>> result;
            Map<String, Object> updatedSnapshot = dbRow.getValues().getSnapshot();
            if(dbRow.getChangeId().getEntityName().startsWith(ASTDbPath.DB_PREFIX)) {
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
            ObjectIdQuery query = new ObjectIdQuery(object.getObjectId(), true, ObjectIdQuery.CACHE);
            QueryResponse response = object.getObjectContext().getChannel().onQuery(null, query);
            @SuppressWarnings("unchecked")
            List<DataRow> result = (List<DataRow>) response.firstList();
            if (result == null || result.size() == 0) {
                return Collections.emptyMap();
            }

            // copy snapshot as we can modify it later
            DataRow dataRow = result.get(0);
            int joinSize = relationship.getJoins().size();
            Map<String, Object> snapshot = joinSize == 1
                    ? new SingleEntryMap<>(relationship.getJoins().get(0).getSourceName())
                    : new HashMap<>(joinSize);
            relationship.getJoins().forEach(join -> {
                Object value = dataRow.get(join.getSourceName());
                if(value != null) {
                    snapshot.put(join.getSourceName(), value);
                }
            });

            // check and merge flattened IDs snapshots
            GraphManager graphManager = object.getObjectContext().getGraphManager();
            if(graphManager instanceof ObjectStore) {
                ObjectStore store = (ObjectStore)graphManager;
                store.getFlattenedIds(object.getObjectId()).forEach(flattenedId -> {
                    // map values of flattened ids from target to source
                    Map<String, Object> idSnapshot = flattenedId.getIdSnapshot();
                    relationship.getJoins().forEach(join -> {
                        Object value = idSnapshot.get(join.getTargetName());
                        if(value != null) {
                            snapshot.put(join.getSourceName(), value);
                        }
                    });
                });
            }

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
