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
package org.apache.cayenne.access.translator.batch.legacy;

import java.util.Objects;

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Implementation of {@link DeleteBatchTranslator}, which uses 'soft' delete
 * (runs UPDATE and sets 'deleted' field to true instead-of running SQL DELETE)
 *
 * @deprecated since 4.2
 */
@Deprecated
public class SoftDeleteBatchTranslator extends DeleteBatchTranslator {

    private String deletedFieldName;

    public SoftDeleteBatchTranslator(DeleteBatchQuery query, DbAdapter adapter, String trimFunction,
            String deletedFieldName) {
        super(query, adapter, trimFunction);
        this.deletedFieldName = Objects.requireNonNull(deletedFieldName);
    }

    @Override
    protected String createSql() {

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("UPDATE ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" SET ").append(strategy.quotedIdentifier(query.getDbEntity(), deletedFieldName)).append(" = ?");

        applyQualifier(buffer);

        return buffer.toString();
    }

    @Override
    protected DbAttributeBinding[] createBindings() {

        DbAttributeBinding[] superBindings = super.createBindings();

        int slen = superBindings.length;

        DbAttributeBinding[] bindings = new DbAttributeBinding[slen + 1];

        DbAttribute deleteAttribute = Objects.requireNonNull(query.getDbEntity().getAttribute(deletedFieldName));
        String typeName = TypesMapping.getJavaBySqlType(deleteAttribute.getType());
        ExtendedType extendedType = adapter.getExtendedTypes().getRegisteredType(typeName);

        bindings[0] = new DbAttributeBinding(deleteAttribute);
        bindings[0].include(1, true, extendedType);
        
        System.arraycopy(superBindings, 0, bindings, 1, slen);

        return bindings;
    }

    @Override
    protected DbAttributeBinding[] doUpdateBindings(BatchQueryRow row) {
        int len = bindings.length;

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;

        // skip position 0... Otherwise follow super algorithm
        for (int i = 1, j = 2; i < len; i++) {

            DbAttributeBinding b = bindings[i];

            // skip null attributes... they are translated as "IS NULL"
            if (deleteBatch.isNull(b.getAttribute())) {
                b.exclude();
            } else {
                Object value = row.getValue(i - 1);
                ExtendedType extendedType = value != null
                        ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                        : adapter.getExtendedTypes().getDefaultType();

                b.include(j++, value, extendedType);
            }
        }

        return bindings;
    }
}
