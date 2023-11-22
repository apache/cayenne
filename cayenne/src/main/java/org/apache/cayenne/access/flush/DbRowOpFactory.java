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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Factory that produces a collection of {@link DbRowOp} from given {@link ObjectDiff}.
 *
 * @since 4.2
 */
class DbRowOpFactory {

    private final EntityResolver resolver;
    private final ObjectStore store;
    private final Set<ArcTarget> processedArcs;
    private final Map<ObjectId, DbRowOp> dbRows;
    private final RootRowOpProcessor rootRowOpProcessor;

    private ClassDescriptor descriptor;
    private Persistent object;
    private ObjectDiff diff;

    DbRowOpFactory(EntityResolver resolver, ObjectStore store, Set<ArcTarget> processedArcs) {
        this.resolver = resolver;
        this.store = store;
        this.dbRows = new HashMap<>(4);
        this.processedArcs = processedArcs;
        this.rootRowOpProcessor = new RootRowOpProcessor(this);
    }

    private void updateDiff(ObjectDiff diff) {
        ObjectId id = (ObjectId)diff.getNodeId();
        this.diff = diff;
        this.descriptor = resolver.getClassDescriptor(id.getEntityName());
        this.object = (Persistent) store.getNode(id);
        this.dbRows.clear();
    }

    Collection<? extends DbRowOp> createRows(ObjectDiff diff) {
        updateDiff(diff);
        DbEntity rootEntity = descriptor.getEntity().getDbEntity();
        DbRowOp row = getOrCreate(rootEntity, object.getObjectId(), DbRowOpType.forObject(object));
        rootRowOpProcessor.setDiff(diff);
        row.accept(rootRowOpProcessor);
        return dbRows.values();
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E get(ObjectId id) {
        return Objects.requireNonNull((E) dbRows.get(id));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E getOrCreate(DbEntity entity, ObjectId id, DbRowOpType type) {
        return (E) dbRows.computeIfAbsent(id, nextId -> createRow(entity, id, type));
    }

    private DbRowOp createRow(DbEntity entity, ObjectId id, DbRowOpType type) {
        // skip phantom nodes, this could be a created and immediately deleted relationship
        if(store.getNode(id) == null && !id.getEntityName().startsWith("db:")) {
            return null;
        }
        switch (type) {
            case INSERT:
                return new InsertDbRowOp(object, entity, id);
            case UPDATE:
                return new UpdateDbRowOp(object, entity, id);
            case DELETE:
                return new DeleteDbRowOp(object, entity, id);
        }
        throw new CayenneRuntimeException("Unknown DbRowType '%s'", type);
    }

    ClassDescriptor getDescriptor() {
        return descriptor;
    }

    Persistent getObject() {
        return object;
    }

    ObjectStore getStore() {
        return store;
    }

    ObjectDiff getDiff() {
        return diff;
    }

    DbEntity getDbEntity(ObjectId id) {
        String entityName = id.getEntityName();
        if(entityName.startsWith(ASTDbPath.DB_PREFIX)) {
            entityName = entityName.substring(ASTDbPath.DB_PREFIX.length());
            return resolver.getDbEntity(entityName);
        } else {
            ObjEntity objEntity = resolver.getObjEntity(entityName);
            return objEntity.getDbEntity();
        }
    }

    Set<ArcTarget> getProcessedArcs() {
        return processedArcs;
    }
}
