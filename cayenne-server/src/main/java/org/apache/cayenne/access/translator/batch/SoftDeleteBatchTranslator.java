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

import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.UpdateBuilder;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
public class SoftDeleteBatchTranslator extends DeleteBatchTranslator {

    private final String deletedFieldName;

    public SoftDeleteBatchTranslator(DeleteBatchQuery query, DbAdapter adapter, String deletedFieldName) {
        super(query, adapter);
        this.deletedFieldName = deletedFieldName;
    }

    @Override
    public String getSql() {
        DeleteBatchQuery query = context.getQuery();
        DbAttribute deleteAttribute = query.getDbEntity().getAttribute(deletedFieldName);

        UpdateBuilder updateBuilder = update(context.getRootDbEntity())
                .set(column(deletedFieldName).attribute(deleteAttribute)
                        .eq(SQLBuilder.value(true).attribute(deleteAttribute)))
                .where(buildQualifier(query.getDbAttributes()));

        String sql = doTranslate(updateBuilder);

        String typeName = TypesMapping.getJavaBySqlType(deleteAttribute);
        ExtendedType<?> extendedType = context.getAdapter().getExtendedTypes().getRegisteredType(typeName);
        bindings[0].include(1, true, extendedType);

        return sql;
    }

    @Override
    public DbAttributeBinding[] updateBindings(BatchQueryRow row) {
        DeleteBatchQuery deleteBatch = context.getQuery();

        for(int i=0, position=1; i<deleteBatch.getDbAttributes().size(); i++) {
            position = updateBinding(row.getValue(i), position);
        }

        return bindings;
    }
}
