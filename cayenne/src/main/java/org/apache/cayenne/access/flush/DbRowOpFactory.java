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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContextObjectStore;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOpFactory;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Factory that produces a collection of {@link DbRowOp} from given {@link ObjectDiff}.
 *
 * @since 4.2
 */
class DbRowOpFactory {

    private final CompiledMapping mapping;
    private final DataContextObjectStore store;
    private final Set<ArcTarget> processedArcs;
    private final Map<ObjectId, DbRowOp> dbRows;
    private final RootRowOpProcessor rootRowOpProcessor;
    private final DeleteDbRowOpFactory deleteDbRowOpFactory;

    private CompiledEntity entity;
    private Persistent object;
    private ObjectDiff diff;

    DbRowOpFactory(CompiledMapping mapping,
                   DataContextObjectStore store,
                   Set<ArcTarget> processedArcs,
                   DeleteDbRowOpFactory deleteDbRowOpFactory) {

        this.mapping = mapping;
        this.store = store;
        this.dbRows = new HashMap<>(4);
        this.processedArcs = processedArcs;
        this.rootRowOpProcessor = new RootRowOpProcessor(this);
        this.deleteDbRowOpFactory = deleteDbRowOpFactory;
    }

    private void updateDiff(ObjectDiff diff) {
        ObjectId id = diff.getNodeId();
        this.diff = diff;
        this.entity = mapping.entity(id.getEntityName());
        this.object = store.getObject(id);
        this.dbRows.clear();
    }

    Collection<? extends DbRowOp> createRows(ObjectDiff diff) {
        updateDiff(diff);
        CompiledTable rootTable = entity.table();
        DbRowOp row = getOrCreate(rootTable, object.getObjectId(), DbRowOpType.forObject(object));
        rootRowOpProcessor.setDiff(diff);
        row.accept(rootRowOpProcessor);
        return dbRows.values();
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E get(ObjectId id) {
        return Objects.requireNonNull((E) dbRows.get(id));
    }

    @SuppressWarnings("unchecked")
    <E extends DbRowOp> E getOrCreate(CompiledTable table, ObjectId id, DbRowOpType type) {
        return (E) dbRows.computeIfAbsent(id, nextId -> createRow(table, id, type));
    }

    private DbRowOp createRow(CompiledTable table, ObjectId id, DbRowOpType type) {
        // skip phantom nodes, this could be a created and immediately deleted relationship
        if (store.getObject(id) == null && !id.getEntityName().startsWith("db:")) {
            return null;
        }
        return switch (type) {
            case INSERT -> new InsertDbRowOp(object, table, id);
            case UPDATE -> new UpdateDbRowOp(object, table, id);
            case DELETE -> deleteDbRowOpFactory.createOp(object, table, id);
        };
    }

    CompiledEntity getEntity() {
        return entity;
    }

    Persistent getObject() {
        return object;
    }

    DataContextObjectStore getStore() {
        return store;
    }

    ObjectDiff getDiff() {
        return diff;
    }

    Set<ArcTarget> getProcessedArcs() {
        return processedArcs;
    }
}
