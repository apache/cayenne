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

import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.UpdateBuilder;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.resolved.UpdateBatch;

/**
 * @since 4.2
 */
public class UpdateBatchTranslator extends BaseBatchTranslator<UpdateBatch> {

    @Override
    protected String createSql(BatchTranslatorContext<UpdateBatch> context) {
        UpdateBatch query = context.getQuery();

        UpdateBuilder updateBuilder = SQLBuilder.update(context.getRootDbEntity());
        for (DbAttribute attr : query.updatedAttributes()) {
            updateBuilder.set(SQLBuilder
                    .column(attr.getName()).attribute(attr)
                    .eq(SQLBuilder.value(1).attribute(attr))
            );
        }
        updateBuilder.where(buildQualifier(context, query.qualifierAttributes()));

        return doTranslate(context, updateBuilder);
    }

    @Override
    protected boolean isNullAttribute(BatchTranslatorContext<UpdateBatch> context, DbAttribute attribute) {
        return context.getQuery().isNull(attribute);
    }

    @Override
    protected int[] createRowValueIndexes(BatchTranslatorContext<UpdateBatch> context) {
        UpdateBatch updateBatch = context.getQuery();
        int updatedCount = updateBatch.updatedAttributes().size();
        int[] indexes = new int[updatedCount + updateBatch.qualifierAttributes().size()];

        int i = 0;
        int j = 0;
        for (; i < updatedCount; i++) {
            indexes[j++] = i;
        }

        // qualifier attributes with null values render as "IS NULL" and have no placeholder
        for (DbAttribute attribute : updateBatch.qualifierAttributes()) {
            if (!updateBatch.isNull(attribute)) {
                indexes[j++] = i;
            }
            i++;
        }
        return Arrays.copyOf(indexes, j);
    }
}
