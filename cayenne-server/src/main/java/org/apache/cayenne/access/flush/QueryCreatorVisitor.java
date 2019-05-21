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
import java.util.List;

import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.DeleteDbRowOp;
import org.apache.cayenne.access.flush.operation.InsertDbRowOp;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * Visitor that creates batch queries.
 * It relies on correct sorting of {@link DbRowOp} to just linearly scan of rows and put them in batches.
 *
 * @since 4.2
 */
// TODO: pass DbRowOp as argument directly to batch...
class QueryCreatorVisitor implements DbRowOpVisitor<Void> {

    private final List<BatchQuery> queryList;
    private final int batchSize;
    private DbRowOp lastRow = null;
    private BatchQuery lastBatch = null;

    QueryCreatorVisitor(int size) {
        // these sizes are pretty much random ...
        this.queryList = new ArrayList<>(Math.min(4, size / 2));
        this.batchSize = Math.min(2, size / 3);
    }

    List<BatchQuery> getQueryList() {
        return queryList;
    }

    @Override
    public Void visitInsert(InsertDbRowOp dbRow) {
        InsertBatchQuery query;
        if(lastRow == null || !lastRow.isSameBatch(dbRow)) {
            query = new InsertBatchQuery(dbRow.getEntity(), batchSize);
            queryList.add(query);
            lastBatch = query;
        } else {
            query = (InsertBatchQuery)lastBatch;
        }
        query.add(dbRow.getValues().getSnapshot(), dbRow.getChangeId());
        lastRow = dbRow;
        return null;
    }

    @Override
    public Void visitUpdate(UpdateDbRowOp dbRow) {
        // skip empty update..
        if(dbRow.getValues().isEmpty()) {
            return null;
        }

        UpdateBatchQuery query;
        if(lastRow == null || !lastRow.isSameBatch(dbRow)) {
            query = new UpdateBatchQuery(
                    dbRow.getEntity(),
                    dbRow.getQualifier().getQualifierAttributes(),
                    dbRow.getValues().getUpdatedAttributes(),
                    dbRow.getQualifier().getNullQualifierNames(),
                    batchSize
            );
            query.setUsingOptimisticLocking(dbRow.getQualifier().isUsingOptimisticLocking());
            queryList.add(query);
            lastBatch = query;
        } else {
            query = (UpdateBatchQuery)lastBatch;
        }
        query.add(dbRow.getQualifier().getSnapshot(), dbRow.getValues().getSnapshot(), dbRow.getChangeId());
        lastRow = dbRow;
        return null;
    }

    @Override
    public Void visitDelete(DeleteDbRowOp dbRow) {
        DeleteBatchQuery query;
        if(lastRow == null || !lastRow.isSameBatch(dbRow)) {
            query = new DeleteBatchQuery(
                    dbRow.getEntity(),
                    dbRow.getQualifier().getQualifierAttributes(),
                    dbRow.getQualifier().getNullQualifierNames(),
                    batchSize
            );
            query.setUsingOptimisticLocking(dbRow.getQualifier().isUsingOptimisticLocking());
            queryList.add(query);
            lastBatch = query;
        } else {
            query = (DeleteBatchQuery)lastBatch;
        }
        query.add(dbRow.getQualifier().getSnapshot());
        lastRow = dbRow;
        return null;
    }
}
