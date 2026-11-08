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

import org.apache.cayenne.access.sqlbuilder.InsertBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.query.resolved.InsertBatch;

/**
 * @since 4.2
 */
public class InsertBatchTranslator extends BaseBatchTranslator<InsertBatch> {

    @Override
    protected String createSql(BatchTranslatorContext<InsertBatch> context) {
        InsertBatch query = context.getQuery();
        InsertBuilder insertBuilder = SQLBuilder.insert(context.getRootDbEntity());

        for (CompiledColumn column : query.dbAttributes()) {
            // skip generated attributes, if needed
            if (excludeInBatch(context, column)) {
                continue;
            }
            insertBuilder
                    .column(SQLBuilder.column(column.name()).attribute(column))
                    // We can use here any non-null value, to create attribute binding,
                    // actual values and ExtendedTypes are resolved when binding each row.
                    .value(SQLBuilder.value(1).attribute(column));
        }

        return doTranslate(context, insertBuilder);
    }

    @Override
    protected int[] createRowValueIndexes(BatchTranslatorContext<InsertBatch> context) {
        InsertBatch query = context.getQuery();
        int[] indexes = new int[query.dbAttributes().size()];
        int i = 0;
        int j = 0;
        for (CompiledColumn column : query.dbAttributes()) {
            if (!excludeInBatch(context, column)) {
                indexes[j++] = i;
            }
            i++;
        }
        return Arrays.copyOf(indexes, j);
    }

    protected boolean excludeInBatch(BatchTranslatorContext<InsertBatch> context, CompiledColumn column) {
        // attribute inclusion rule - one of the rules below must be true:
        //  (1) attribute not generated
        //  (2) attribute is generated and PK and adapter does not support generated keys
        return column.isGenerated() && (!column.isPrimaryKey() || context.getAdapter().supportsGeneratedKeys());
    }

    @Override
    protected boolean isNullAttribute(BatchTranslatorContext<InsertBatch> context, CompiledColumn column) {
        return false;
    }
}
