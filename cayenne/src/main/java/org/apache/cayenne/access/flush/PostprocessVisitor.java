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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ToManyMapProperty;

/**
 * @since 4.2
 */
class PostprocessVisitor implements DbRowOpVisitor<Void> {

    private final DataContext context;
    private Map<ObjectId, DataRow> updatedSnapshots;
    private Collection<ObjectId> deletedIds;

    PostprocessVisitor(DataContext context) {
        this.context = context;
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        processObjectChange(dbRow);
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRowOp dbRow) {
        processObjectChange(dbRow);
        return null;
    }

    private void processObjectChange(DbRowOp dbRow) {
        if (dbRow.getChangeId().getEntityName().startsWith(ASTDbPath.DB_PREFIX)) {
            return;
        }

        DataRow dataRow = context.currentSnapshot(dbRow.getObject());

        if (dbRow.getObject() != null) {
            Persistent persistent = dbRow.getObject();
            dataRow.setReplacesVersion(persistent.getSnapshotVersion());
            persistent.setSnapshotVersion(dataRow.getVersion());
        }

        if (updatedSnapshots == null) {
            updatedSnapshots = new HashMap<>();
        }
        updatedSnapshots.put(dbRow.getObject().getObjectId(), dataRow);

        // update Map reverse relationships
        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(dbRow.getChangeId().getEntityName());
        for (ArcProperty arc : descriptor.getMapArcProperties()) {
            ToManyMapProperty reverseArc = (ToManyMapProperty) arc.getComplimentaryReverseArc();

            // must resolve faults... hopefully for to-one this will not cause extra fetches...
            Object source = arc.readProperty(dbRow.getObject());
            if (source != null && !reverseArc.isFault(source)) {
                remapTarget(reverseArc, source, dbRow.getObject());
            }
        }
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        if (dbRow.getChangeId().getEntityName().startsWith(ASTDbPath.DB_PREFIX)) {
            return null;
        }
        if (deletedIds == null) {
            deletedIds = new HashSet<>();
        }
        deletedIds.add(dbRow.getChangeId());
        return null;
    }

    Collection<ObjectId> getDeletedIds() {
        return deletedIds == null ? Collections.emptyList() : deletedIds;
    }

    Map<ObjectId, DataRow> getUpdatedSnapshots() {
        return updatedSnapshots == null ? Collections.emptyMap() : updatedSnapshots;
    }

    private void remapTarget(ToManyMapProperty property, Object source, Object target) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) property.readProperty(source);
        Object newKey = property.getMapKey(target);
        Object currentValue = map.get(newKey);

        if (currentValue == target) {
            // nothing to do
            return;
        }
        // else - do not check for conflicts here (i.e. another object mapped for the same key), as we have no control
        // of the order in which this method is called, so another object may be remapped later by the caller
        // must do a slow map scan to ensure the object is not mapped under a different key...
        Iterator<Map.Entry<Object, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, Object> e = it.next();
            if (e.getValue() == target) {
                it.remove();
                break;
            }
        }

        map.put(newKey, target);
    }
}
