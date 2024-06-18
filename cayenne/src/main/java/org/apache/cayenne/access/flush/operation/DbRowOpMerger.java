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

import java.util.function.BiFunction;

/**
 * BiFunction that merges two {@link DbRowOp} changing same object.
 *
 * @since 4.2
 */
public class DbRowOpMerger implements DbRowOpVisitor<DbRowOp>, BiFunction<DbRowOp, DbRowOp, DbRowOp> {

    private DbRowOp dbRow;

    @Override
    public DbRowOp apply(DbRowOp oldValue, DbRowOp newValue) {
        this.dbRow = oldValue;
        return newValue.accept(this);
    }

    @Override
    public DbRowOp visitInsert(InsertDbRowOp other) {
        if(dbRow instanceof DeleteDbRowOp) {
            return new DeleteInsertDbRowOp((DeleteDbRowOp)dbRow, other);
        }
        return mergeValues((DbRowOpWithValues) dbRow, other);
    }

    @Override
    public DbRowOp visitUpdate(UpdateDbRowOp other) {
        // delete beats update ...
        if(dbRow instanceof DeleteDbRowOp) {
            return dbRow;
        }
        return mergeValues((DbRowOpWithValues) dbRow, other);
    }

    @Override
    public DbRowOp visitDelete(DeleteDbRowOp other) {
        if(dbRow.getChangeId() == other.getChangeId()) {
            return other;
        }
        // clash of Insert/Delete with equal ObjectId
        if(dbRow instanceof InsertDbRowOp) {
            return new DeleteInsertDbRowOp(other, (InsertDbRowOp)dbRow);
        }
        return other;
    }

    private DbRowOp mergeValues(DbRowOpWithValues left, DbRowOpWithValues right) {
        if(right.getChangeId() == right.getObject().getObjectId()) {
            right.getValues().merge(left.getValues());
            return right;
        } else {
            left.getValues().merge(right.getValues());
            return left;
        }
    }
}
