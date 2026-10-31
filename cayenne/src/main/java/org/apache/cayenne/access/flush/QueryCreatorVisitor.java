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

import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.SoftDeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.query.resolved.BatchRow;
import org.apache.cayenne.query.resolved.DeleteBatch;
import org.apache.cayenne.query.resolved.InsertBatch;
import org.apache.cayenne.query.resolved.ResolvedBatch;
import org.apache.cayenne.query.resolved.UpdateBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Creates the batches of a flush from the sorted row operations. Consecutive operations that belong to the same batch
 * (see {@link DbRowOp#isSameBatch(DbRowOp)}) become the rows of one batch, which is created once the next operation
 * starts another batch, or when the list is read.
 *
 * @since 4.2
 */
class QueryCreatorVisitor implements DbRowOpVisitor<Void> {

    private final List<ResolvedBatch> batches;
    private final int batchSize;
    private DbRowOp lastRow;

    // the batch being collected: its rows, and the factory creating it from them once it is complete
    private List<BatchRow> rows;
    private Function<List<BatchRow>, ResolvedBatch> factory;

    QueryCreatorVisitor(int size) {
        // these sizes are pretty much random ...
        this.batches = new ArrayList<>(Math.min(4, size / 2));
        this.batchSize = Math.min(2, size / 3);
    }

    List<ResolvedBatch> getQueryList() {
        closeBatch();
        return batches;
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        if (lastRow == null || !lastRow.isSameBatch(dbRow)) {
            openBatch(rows -> new InsertBatch(dbRow.getEntity(), rows));
        }
        rows.add(new BatchRow(dbRow.getChangeId(), null, dbRow.getValues().getSnapshot()));
        lastRow = dbRow;
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRowOp dbRow) {
        // skip empty update..
        if (dbRow.getValues().isEmpty()) {
            return null;
        }
        if (lastRow == null || !lastRow.isSameBatch(dbRow)) {
            openBatch(rows -> new UpdateBatch(
                    dbRow.getEntity(),
                    dbRow.getQualifier().getQualifierAttributes(),
                    dbRow.getValues().getUpdatedAttributes(),
                    dbRow.getQualifier().getNullQualifierNames(),
                    dbRow.getQualifier().isUsingOptimisticLocking(),
                    rows));
        }
        rows.add(new BatchRow(dbRow.getChangeId(), dbRow.getQualifier().getSnapshot(),
                dbRow.getValues().getSnapshot()));
        lastRow = dbRow;
        return null;
    }

    @Override
    public Void visitSoftDelete(SoftDeleteDbRowOp dbRow) {
        // a soft delete is executed as an update of the soft-delete flag column
        if (lastRow == null || !lastRow.isSameBatch(dbRow)) {
            openBatch(rows -> new UpdateBatch(
                    dbRow.getEntity(),
                    dbRow.getQualifier().getQualifierAttributes(),
                    dbRow.getValues().getUpdatedAttributes(),
                    dbRow.getQualifier().getNullQualifierNames(),
                    dbRow.getQualifier().isUsingOptimisticLocking(),
                    rows));
        }
        rows.add(new BatchRow(dbRow.getChangeId(), dbRow.getQualifier().getSnapshot(),
                dbRow.getValues().getSnapshot()));
        lastRow = dbRow;
        return null;
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        if (lastRow == null || !lastRow.isSameBatch(dbRow)) {
            openBatch(rows -> new DeleteBatch(
                    dbRow.getEntity(),
                    dbRow.getQualifier().getQualifierAttributes(),
                    dbRow.getQualifier().getNullQualifierNames(),
                    dbRow.getQualifier().isUsingOptimisticLocking(),
                    rows));
        }
        rows.add(new BatchRow(null, dbRow.getQualifier().getSnapshot(), null));
        lastRow = dbRow;
        return null;
    }

    private void openBatch(Function<List<BatchRow>, ResolvedBatch> factory) {
        closeBatch();
        this.rows = new ArrayList<>(batchSize);
        this.factory = factory;
    }

    private void closeBatch() {
        if (factory != null) {
            batches.add(factory.apply(rows));
            factory = null;
            rows = null;
        }
    }
}
