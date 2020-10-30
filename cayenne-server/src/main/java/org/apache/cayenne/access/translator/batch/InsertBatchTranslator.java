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

import org.apache.cayenne.access.sqlbuilder.InsertBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;

/**
 * @since 4.2
 */
public class InsertBatchTranslator extends BaseBatchTranslator<InsertBatchQuery> implements BatchTranslator {

    public InsertBatchTranslator(InsertBatchQuery query, DbAdapter adapter) {
        super(query, adapter);
    }

    @Override
    public String getSql() {
        InsertBatchQuery query = context.getQuery();
        InsertBuilder insertBuilder = SQLBuilder.insert(context.getRootDbEntity());

        for(DbAttribute attribute : query.getDbAttributes()) {
            // skip generated attributes, if needed
            if(excludeInBatch(attribute)) {
                continue;
            }
            insertBuilder
                    .column(SQLBuilder.column(attribute.getName()).attribute(attribute))
                    // We can use here any non-null value, to create attribute binding,
                    // actual value and ExtendedType will be set at updateBindings() call.
                    .value(SQLBuilder.value(1).attribute(attribute));
        }

        return doTranslate(insertBuilder);
    }

    @Override
    public DbAttributeBinding[] updateBindings(BatchQueryRow row) {
        InsertBatchQuery query = context.getQuery();
        int i=0;
        int j=0;
        for(DbAttribute attribute : query.getDbAttributes()) {
            if(excludeInBatch(attribute)) {
                i++;
                continue;
            }

            Object value = row.getValue(i++);
            ExtendedType<?> extendedType = value != null
                    ? context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass())
                    : context.getAdapter().getExtendedTypes().getDefaultType();
            bindings[j].include(++j, value, extendedType);
        }
        return bindings;
    }

    protected boolean excludeInBatch(DbAttribute attribute) {
        // attribute inclusion rule - one of the rules below must be true:
        //  (1) attribute not generated
        //  (2) attribute is generated and PK and adapter does not support generated keys
        return attribute.isGenerated() && (!attribute.isPrimaryKey() || context.getAdapter().supportsGeneratedKeys());
    }

    @Override
    protected boolean isNullAttribute(DbAttribute attribute) {
        return false;
    }
}
