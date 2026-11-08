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

package org.apache.cayenne.access.translator.batch;

import java.util.Arrays;

import org.apache.cayenne.access.sqlbuilder.DeleteBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.query.resolved.DeleteBatch;

/**
 * @since 4.2
 */
public class DeleteBatchTranslator extends BaseBatchTranslator<DeleteBatch> {

    @Override
    protected String createSql(BatchTranslatorContext<DeleteBatch> context) {
        DeleteBuilder deleteBuilder = SQLBuilder
                .delete(context.getRootDbEntity())
                .where(buildQualifier(context, context.getQuery().dbAttributes()));
        return doTranslate(context, deleteBuilder);
    }

    @Override
    protected boolean isNullAttribute(BatchTranslatorContext<DeleteBatch> context, CompiledColumn column) {
        return context.getQuery().isNull(column);
    }

    @Override
    protected int[] createRowValueIndexes(BatchTranslatorContext<DeleteBatch> context) {
        DeleteBatch deleteBatch = context.getQuery();
        int[] indexes = new int[deleteBatch.dbAttributes().size()];

        int i = 0;
        int j = 0;
        // attributes with null values render as "IS NULL" and have no placeholder
        for (CompiledColumn column : deleteBatch.dbAttributes()) {
            if (!deleteBatch.isNull(column)) {
                indexes[j++] = i;
            }
            i++;
        }
        return Arrays.copyOf(indexes, j);
    }
}
