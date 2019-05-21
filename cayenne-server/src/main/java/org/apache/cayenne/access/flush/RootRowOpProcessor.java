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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.operation.DbRowOpType;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.map.ObjEntity;

/**
 * Visitor that runs all required actions based on operation type.
 * <p>
 * E.g. it creates values for insert and update, it fills optimistic lock qualifier for update and delete, etc.
 *
 * @since 4.2
 */
class RootRowOpProcessor implements DbRowOpVisitor<Void> {
    private final DbRowOpFactory dbRowOpFactory;
    private ObjectDiff diff;

    RootRowOpProcessor(DbRowOpFactory dbRowOpFactory) {
        this.dbRowOpFactory = dbRowOpFactory;
    }

    void setDiff(ObjectDiff diff) {
        this.diff = diff;
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        diff.apply(new ValuesCreationHandler(dbRowOpFactory, DbRowOpType.INSERT));
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRowOp dbRow) {
        diff.apply(new ValuesCreationHandler(dbRowOpFactory, DbRowOpType.UPDATE));
        if (dbRowOpFactory.getDescriptor().getEntity().getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            dbRowOpFactory.getDescriptor().visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
        }
        return null;
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        if (dbRowOpFactory.getDescriptor().getEntity().isReadOnly()) {
            throw new CayenneRuntimeException("Attempt to modify object(s) mapped to a read-only entity: '%s'. " +
                    "Can't commit changes.", dbRowOpFactory.getDescriptor().getEntity().getName());
        }
        diff.apply(new ArcValuesCreationHandler(dbRowOpFactory, DbRowOpType.DELETE));
        Collection<ObjectId> flattenedIds = dbRowOpFactory.getStore().getFlattenedIds(dbRow.getChangeId());
        flattenedIds.forEach(id -> dbRowOpFactory.getOrCreate(dbRowOpFactory.getDbEntity(id), id, DbRowOpType.DELETE));
        if (dbRowOpFactory.getDescriptor().getEntity().getDeclaredLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            dbRowOpFactory.getDescriptor().visitAllProperties(new OptimisticLockQualifierBuilder(dbRow, diff));
        }
        return null;
    }
}
