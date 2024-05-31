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

import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.AdditionalDbEntityDescriptor;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Visitor that runs all required actions based on operation type.
 * <p>
 * E.g. it creates values for insert and update, it fills optimistic lock qualifier for update and delete, etc.
 *
 * @since 4.2
 */
class RootRowOpProcessor implements DbRowOpVisitor<Void> {
    private final DbRowOpFactory dbRowOpFactory;
    private final GraphChangeHandler insertHandler;
    private final GraphChangeHandler updateHandler;
    private final GraphChangeHandler deleteHandler;
    private ObjectDiff diff;

    RootRowOpProcessor(DbRowOpFactory dbRowOpFactory) {
        this.dbRowOpFactory = dbRowOpFactory;
        this.insertHandler = new ValuesCreationHandler(dbRowOpFactory, DbRowOpType.INSERT);
        this.updateHandler = new ValuesCreationHandler(dbRowOpFactory, DbRowOpType.UPDATE);
        this.deleteHandler = new ArcValuesCreationHandler(dbRowOpFactory, DbRowOpType.DELETE);
    }

    void setDiff(ObjectDiff diff) {
        this.diff = diff;
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        diff.apply(insertHandler);
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRowOp dbRow) {
        diff.apply(updateHandler);
        if (dbRowOpFactory.getDescriptor().getEntity().getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            dbRowOpFactory.getDescriptor().visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
        }
        return null;
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        ObjEntity entity = dbRowOpFactory.getDescriptor().getEntity();
        if (entity.isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", entity.getName());
        }
        diff.apply(deleteHandler);

        ClassDescriptor descriptor = dbRowOpFactory.getDescriptor();
        Map<CayennePath,ObjectId> flattenedPathIdMap = dbRowOpFactory.getStore().getFlattenedPathIdMap(dbRow.getChangeId());
        flattenedPathIdMap.forEach((path, id) -> {
            AdditionalDbEntityDescriptor addEntity = descriptor.getAdditionalDbEntities().get(path);
            if (!addEntity.noDelete()) {// See PersistentDescriptorFactory.indexAdditionalDbEntities
                dbRowOpFactory.getOrCreate(addEntity.getDbEntity(), id, DbRowOpType.DELETE);
            }
        });

        if (entity.getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            dbRowOpFactory.getDescriptor().visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
        }
        return null;
    }
}
