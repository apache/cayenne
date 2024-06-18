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

import org.apache.cayenne.access.sqlbuilder.DeleteBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * @since 4.2
 */
public class DeleteBatchTranslator extends BaseBatchTranslator<DeleteBatchQuery> implements BatchTranslator {

    public DeleteBatchTranslator(DeleteBatchQuery query, DbAdapter adapter) {
        super(query, adapter);
    }

    @Override
    public String getSql() {
        DeleteBuilder deleteBuilder = SQLBuilder
                .delete(context.getRootDbEntity())
                .where(buildQualifier(context.getQuery().getDbAttributes()));
        return doTranslate(deleteBuilder);
    }

    @Override
    protected boolean isNullAttribute(DbAttribute attribute) {
        return context.getQuery().isNull(attribute);
    }

    @Override
    public DbAttributeBinding[] updateBindings(BatchQueryRow row) {
        DeleteBatchQuery deleteBatch = context.getQuery();
        for(int i=0, position=0; i<deleteBatch.getDbAttributes().size(); i++) {
            position = updateBinding(row.getValue(i), position);
        }
        return bindings;
    }

    protected int updateBinding(Object value, int position) {
        // skip null attributes... they are translated as "IS NULL"
        if(value != null) {
            ExtendedType<?> extendedType = context.getAdapter().getExtendedTypes().getRegisteredType(value.getClass());
            bindings[position].include(++position, value, extendedType);
        }
        return position;
    }
}
