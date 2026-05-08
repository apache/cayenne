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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DbRowOpWithValues;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Graph handler that collects information about arc changes into
 * {@link org.apache.cayenne.access.flush.operation.Values} and/or {@link org.apache.cayenne.access.flush.operation.Qualifier}.
 *
 * @since 4.2
 */
class ArcValuesCreationHandler implements GraphChangeHandler {

    final DbRowOpFactory factory;
    final DbRowOpType defaultType;

    ArcValuesCreationHandler(DbRowOpFactory factory, DbRowOpType defaultType) {
        this.factory = factory;
        this.defaultType = defaultType;
    }

    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange(nodeId, targetNodeId, arcId, true);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange(nodeId, targetNodeId, arcId, false);
    }

    private void processArcChange(Object nodeId, Object targetNodeId, ArcId arcId, boolean created) {
        ObjectId actualTargetId = (ObjectId)targetNodeId;
        ObjectId snapshotId = factory.getDiff().getCurrentArcSnapshotValue(arcId.getForwardArc());
        if(snapshotId != null) {
            actualTargetId = snapshotId;
        }
        ArcTarget arcTarget = new ArcTarget((ObjectId) nodeId, actualTargetId, arcId, !created);
        if(factory.getProcessedArcs().contains(arcTarget.getReversed())) {
            return;
        }

        ObjEntity entity = factory.getDescriptor().getEntity();
        ObjRelationship objRelationship = entity.getRelationship(arcTarget.getArcId().getForwardArc());
        if(objRelationship == null) {
            String arc = arcId.getForwardArc();
            if(arc.startsWith(ASTDbPath.DB_PREFIX)) {
                String relName = arc.substring(ASTDbPath.DB_PREFIX.length());
                DbRelationship dbRelationship = entity.getDbEntity().getRelationship(relName);
                processRelationship(dbRelationship, arcTarget.getSourceId(), arcTarget.getTargetId(), created);
            }
            return;
        }

        if(objRelationship.isFlattened()) {
            FlattenedPathProcessingResult result = processFlattenedPath(arcTarget.getSourceId(), arcTarget.getTargetId(), entity.getDbEntity(),
                    objRelationship.getDbRelationshipPath(), created);
            if(result.isProcessed()) {
                factory.getProcessedArcs().add(arcTarget);
            }
        } else {
            DbRelationship dbRelationship = objRelationship.getDbRelationships().get(0);
            processRelationship(dbRelationship, arcTarget.getSourceId(), arcTarget.getTargetId(), created);
            factory.getProcessedArcs().add(arcTarget);
        }
    }

    FlattenedPathProcessingResult processFlattenedPath(ObjectId id, ObjectId finalTargetId, DbEntity entity, CayennePath dbPath, boolean add) {
        if(shouldSkipFlattenedOp(id, finalTargetId)) {
            return flattenedResultNotProcessed();
        }

        CayennePath flattenedPath = CayennePath.EMPTY_PATH;

        ObjectId srcId = id;
        ObjectId targetId = null;

        List<CayenneMapEntry> dbPathComponents = new ArrayList<>();
        Iterator<CayenneMapEntry> dbPathIterator = entity.resolvePathComponents(dbPath);
        dbPathIterator.forEachRemaining(dbPathComponents::add);

        for (int i = 0; i < dbPathComponents.size(); i++) {
            CayenneMapEntry entry = dbPathComponents.get(i);
            flattenedPath = flattenedPath.dot(entry.getName());
            if (entry instanceof DbRelationship) {
                DbRelationship relationship = (DbRelationship)entry;
                // intermediate db entity to be inserted
                DbEntity target = relationship.getTargetEntity();
                // if ID is present, just use it, otherwise create new
                // if this is the last segment, and it's a relationship, use known target id from arc creation
                boolean isLast = i == dbPathComponents.size() - 1;
                if (isLast) {
                    targetId = finalTargetId;
                } else {
                    if(!relationship.isToMany()) {
                        targetId = factory.getStore().getFlattenedId(id, flattenedPath);
                    } else {
                        targetId = null;
                    }
                }

                // if targetId is not present, try to derive it from finalTargetId
                if (targetId == null && finalTargetId != null) {
                    List<CayenneMapEntry> remainingPath = dbPathComponents.subList(i + 1, dbPathComponents.size());
                    Map<String, Object> derivedPk = derivePkValuesFromFinal(target, finalTargetId, remainingPath);
                    if (!derivedPk.isEmpty()) {
                        targetId = ObjectId.of(ASTDbPath.DB_PREFIX + target.getName(), derivedPk);
                        if (!relationship.isToMany()) {
                            factory.getStore().markFlattenedPath(id, flattenedPath, targetId);
                        }
                    }
                }

                if (targetId == null) {
                    // should insert, regardless of original operation (insert/update)
                    targetId = ObjectId.of(ASTDbPath.DB_PREFIX + target.getName());
                    if (!relationship.isToMany()) {
                        factory.getStore().markFlattenedPath(id, flattenedPath, targetId);
                    }

                    DbRowOpType type;
                    if (relationship.isToMany()) {
                        // in case of vertical inheritance avoid DELETE/INSERT - use UPDATE instead (CAY-2890)
                        boolean isVI = isInVerticalInheritanceChain(target);
                        if (isVI) {
                            type = (defaultType == DbRowOpType.INSERT && add) ? DbRowOpType.INSERT : DbRowOpType.UPDATE;
                        } else {
                            type = add ? DbRowOpType.INSERT : DbRowOpType.DELETE;
                        }
                        factory.getOrCreate(target, targetId, type);
                    } else {
                        type = add ? DbRowOpType.INSERT : DbRowOpType.UPDATE;
                        factory.<DbRowOpWithValues>getOrCreate(target, targetId, type)
                                .getValues()
                                .addFlattenedId(flattenedPath, targetId);
                    }
                } else if (!isLast) {
                    // should update existing DB row
                    factory.getOrCreate(target, targetId, add ? DbRowOpType.UPDATE : defaultType);
                }
                processRelationship(relationship, srcId, targetId, shouldProcessAsAddition(relationship, add));
                srcId = targetId; // use target as next source
            }
        }

        return flattenedResultId(targetId);
    }

    private boolean shouldSkipFlattenedOp(ObjectId id, ObjectId finalTargetId) {
        // as we get two sides of the relationship processed,
        // check if we got more information for a reverse operation
        return finalTargetId != null
                && factory.getStore().getFlattenedIds(id).isEmpty()
                && !factory.getStore().getFlattenedIds(finalTargetId).isEmpty();
    }

    private boolean shouldProcessAsAddition(DbRelationship relationship, boolean add) {
        if(add) {
            return true;
        }

        // should always add data from one-to-one relationships
        for(DbJoin join : relationship.getJoins()) {
            if(!join.getSource().isPrimaryKey() || !join.getTarget().isPrimaryKey()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given DbEntity is part of a vertical inheritance (VI) hierarchy.
     * This is determined by finding ObjEntity inheritance roots and checking if the target
     * DbEntity is reachable via PK-to-PK relationships from the root's DbEntity.
     */
    private boolean isInVerticalInheritanceChain(DbEntity target) {
        DataMap dataMap = target.getDataMap();
        if (dataMap == null) {
            return false;
        }

        EntityResolver resolver = new EntityResolver(List.of(dataMap));
        for (ObjEntity objEntity : dataMap.getObjEntities()) {
            if (objEntity.getSuperEntity() != null) {
                continue;
            }
            EntityInheritanceTree inheritanceTree = resolver.getInheritanceTree(objEntity.getName());
            if (inheritanceTree == null || inheritanceTree.getChildren().isEmpty()) {
                continue;
            }
            DbEntity rootDbEntity = objEntity.getDbEntity();
            if (rootDbEntity != null && isInDependentPkChain(rootDbEntity, target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * BFS traversal to check if target DbEntity is reachable from root via toDependentPK relationships.
     * In vertical inheritance, child tables are linked to parent tables via PK-to-PK foreign keys.
     */
    private boolean isInDependentPkChain(DbEntity root, DbEntity target) {
        Queue<DbEntity> queue = new LinkedList<>();
        Set<DbEntity> visited = new HashSet<>();
        queue.add(root);
        visited.add(root);

        while (!queue.isEmpty()) {
            DbEntity current = queue.remove();
            for (DbRelationship relationship : current.getRelationships()) {
                if (!relationship.isToDependentPK()) {
                    continue;
                }
                DbEntity childEntity = relationship.getTargetEntity();
                if (childEntity == null || !visited.add(childEntity)) {
                    continue;
                }
                if (childEntity == target) {
                    return true;
                }
                queue.add(childEntity);
            }
        }
        return false;
    }

    /**
     * Derives PK values for the target DbEntity from finalTargetId by tracing through
     * the remaining path. Only works if the entire remaining path consists of PK-to-PK joins.
     *
     * @return map of target PK attribute names to their values, or empty map if derivation fails
     */
    private Map<String, Object> derivePkValuesFromFinal(DbEntity target, ObjectId finalTargetId,
                                                        List<CayenneMapEntry> remainingPath) {
        Map<String, Object> finalIdSnapshot = finalTargetId.getIdSnapshot();
        if (finalIdSnapshot == null) {
            return Map.of();
        }
        Map<String, String> targetToFinalPkMapping = resolvePkMapping(target, remainingPath);
        if (targetToFinalPkMapping.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> derivedPkValues = new HashMap<>(targetToFinalPkMapping.size());
        for (Map.Entry<String, String> entry : targetToFinalPkMapping.entrySet()) {
            String targetPkAttr = entry.getKey();
            String finalPkAttr = entry.getValue();
            Object value = finalIdSnapshot.get(finalPkAttr);
            if (value == null) {
                return Map.of();
            }
            derivedPkValues.put(targetPkAttr, value);
        }
        return derivedPkValues;
    }

    /**
     * Builds a mapping from target's PK attribute names to the corresponding PK attribute names
     * in the final entity of the path. Traces through each relationship's joins to follow
     * the PK-to-PK chain.
     *
     * @return map where key = target PK attr name, value = final entity PK attr name;
     *         empty map if the path is not a valid PK-to-PK chain
     */
    private Map<String, String> resolvePkMapping(DbEntity target, List<CayenneMapEntry> remainingPath) {
        Map<String, String> targetToCurrentPk = new HashMap<>();
        for (DbAttribute pk : target.getPrimaryKeys()) {
            targetToCurrentPk.put(pk.getName(), pk.getName());
        }
        if (targetToCurrentPk.isEmpty()) {
            return Map.of();
        }

        for (CayenneMapEntry pathComponent : remainingPath) {
            if (!(pathComponent instanceof DbRelationship)) {
                return Map.of();
            }
            DbRelationship rel = (DbRelationship) pathComponent;
            DbRelationship reverse = rel.getReverseRelationship();
            boolean isPkToPk = rel.isToDependentPK() || (reverse != null && reverse.isToDependentPK());
            if (!isPkToPk || rel.isToMany()) {
                return Map.of();
            }
            Map<String, String> nextMapping = new HashMap<>(targetToCurrentPk.size());
            for (DbJoin join : rel.getJoins()) {
                if (!join.getSource().isPrimaryKey() || !join.getTarget().isPrimaryKey()) {
                    return Map.of();
                }
                for (Map.Entry<String, String> entry : targetToCurrentPk.entrySet()) {
                    String targetPkAttr = entry.getKey();
                    String currentPkAttr = entry.getValue();
                    if (currentPkAttr.equals(join.getSource().getName())) {
                        nextMapping.put(targetPkAttr, join.getTarget().getName());
                    }
                }
            }
            if (nextMapping.size() != targetToCurrentPk.size()) {
                return Map.of();
            }
            targetToCurrentPk = nextMapping;
        }
        return targetToCurrentPk;
    }

    protected void processRelationship(DbRelationship dbRelationship, ObjectId srcId, ObjectId targetId, boolean add) {
        for(DbJoin join : dbRelationship.getJoins()) {
            boolean srcPK = join.getSource().isPrimaryKey();
            boolean targetPK = join.getTarget().isPrimaryKey();

            Object valueToUse;
            DbRowOp rowOp;
            DbAttribute attribute;
            ObjectId id;
            boolean processDelete;

            // We manage 3 cases here:
            // 1. PK -> FK: just propagate value from PK and to FK
            // 2. PK -> PK: check isToDep flag and set dependent one
            // 3. NON-PK -> FK (not supported fully for now, see CAY-2488): also check isToDep flag,
            //    but get value from DbRow, not ObjID
            if(srcPK != targetPK) {
                // case 1
                processDelete = true;
                id = null;
                if(srcPK) {
                    valueToUse = ObjectIdValueSupplier.getFor(srcId, join.getSourceName());
                    rowOp = factory.getOrCreate(dbRelationship.getTargetEntity(), targetId, DbRowOpType.UPDATE);
                    attribute = join.getTarget();
                } else {
                    valueToUse = ObjectIdValueSupplier.getFor(targetId, join.getTargetName());
                    rowOp = factory.getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType);
                    attribute = join.getSource();
                }
            } else {
                // case 2 and 3
                processDelete = false;
                if(dbRelationship.isToDependentPK()) {
                    valueToUse = ObjectIdValueSupplier.getFor(srcId, join.getSourceName());
                    rowOp = factory.getOrCreate(dbRelationship.getTargetEntity(), targetId, DbRowOpType.UPDATE);
                    attribute = join.getTarget();
                    id = targetId;
                    if(dbRelationship.isToMany()) {
                        // strange mapping toDepPK and toMany, but just skip it
                        rowOp = null;
                    }
                } else {
                    valueToUse = ObjectIdValueSupplier.getFor(targetId, join.getTargetName());
                    rowOp = factory.getOrCreate(dbRelationship.getSourceEntity(), srcId, defaultType);
                    attribute = join.getSource();
                    id = srcId;
                    if(dbRelationship.getReverseRelationship().isToMany()) {
                        // strange mapping toDepPK and toMany, but just skip it
                        rowOp = null;
                    }
                }
            }

            // propagated master -> child PK
            if(id != null && attribute.isPrimaryKey()) {
                id.getReplacementIdMap().put(attribute.getName(), valueToUse);
            }
            if(rowOp != null) {
                rowOp.accept(new ValuePropagationVisitor(attribute, add, valueToUse, processDelete));
            }
        }
    }

    private static class ValuePropagationVisitor implements DbRowOpVisitor<Void> {
        private final DbAttribute attribute;
        private final boolean add;
        private final Object valueToUse;
        private final boolean processDelete;

        private ValuePropagationVisitor(DbAttribute attribute, boolean add, Object valueToUse, boolean processDelete) {
            this.attribute = attribute;
            this.add = add;
            this.valueToUse = valueToUse;
            this.processDelete = processDelete;
        }

        @Override
        public Void visitInsert(InsertDbRowOp dbRow) {
            dbRow.getValues().addValue(attribute, add ? valueToUse : null, true);
            return null;
        }

        @Override
        public Void visitUpdate(UpdateDbRowOp dbRow) {
            dbRow.getValues().addValue(attribute, add ? valueToUse : null, true);
            return null;
        }

        @Override
        public Void visitDelete(DeleteDbRowOp dbRow) {
            if(processDelete) {
                dbRow.getQualifier().addAdditionalQualifier(attribute, valueToUse);
            }
            return null;
        }
    }

    static FlattenedPathProcessingResult flattenedResultId(ObjectId id) {
        return new FlattenedPathProcessingResult(true, id);
    }

    static FlattenedPathProcessingResult flattenedResultNotProcessed() {
        return new FlattenedPathProcessingResult(false, null);
    }

    final static class FlattenedPathProcessingResult {
        private final boolean processed;
        private final ObjectId id;

        private FlattenedPathProcessingResult(boolean processed, ObjectId id) {
            this.processed = processed;
            this.id = id;
        }

        public boolean isProcessed() {
            return processed;
        }

        public ObjectId getId() {
            return id;
        }
    }
}
