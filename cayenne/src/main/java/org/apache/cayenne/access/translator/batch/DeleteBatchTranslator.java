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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * @since 4.2
 */
public class DeleteBatchTranslator extends BaseBatchTranslator<DeleteBatchQuery> {

    @Override
    protected String createSql(BatchTranslatorContext<DeleteBatchQuery> context) {
        DeleteBuilder deleteBuilder = SQLBuilder
                .delete(context.getRootDbEntity())
                .where(buildQualifier(context, context.getQuery().getDbAttributes()));
        return doTranslate(context, deleteBuilder);
    }

    @Override
    protected boolean isNullAttribute(BatchTranslatorContext<DeleteBatchQuery> context, DbAttribute attribute) {
        return context.getQuery().isNull(attribute);
    }

    @Override
    protected int[] createRowValueIndexes(BatchTranslatorContext<DeleteBatchQuery> context) {
        DeleteBatchQuery deleteBatch = context.getQuery();
        int[] indexes = new int[deleteBatch.getDbAttributes().size()];

        int i = 0;
        int j = 0;
        // attributes with null values render as "IS NULL" and have no placeholder
        for (DbAttribute attribute : deleteBatch.getDbAttributes()) {
            if (!deleteBatch.isNull(attribute)) {
                indexes[j++] = i;
            }
            i++;
        }
        return Arrays.copyOf(indexes, j);
    }
}
