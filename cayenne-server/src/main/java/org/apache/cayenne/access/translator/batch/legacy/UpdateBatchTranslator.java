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

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * A translator for UpdateBatchQueries that produces parameterized SQL.
 * @deprecated since 4.2
 */
@Deprecated
public class UpdateBatchTranslator extends DefaultBatchTranslator {

    public UpdateBatchTranslator(UpdateBatchQuery query, DbAdapter adapter, String trimFunction) {
        super(query, adapter, trimFunction);
    }

    @Override
    protected String createSql() {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        List<DbAttribute> qualifierAttributes = updateBatch.getQualifierAttributes();
        List<DbAttribute> updatedDbAttributes = updateBatch.getUpdatedAttributes();

        StringBuilder buffer = new StringBuilder("UPDATE ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                buffer.append(", ");
            }

            DbAttribute attribute = updatedDbAttributes.get(i);
            buffer.append(strategy.quotedName(attribute));
            buffer.append(" = ?");
        }

        buffer.append(" WHERE ");

        Iterator<DbAttribute> i = qualifierAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = i.next();
            appendDbAttribute(buffer, attribute);
            buffer.append(updateBatch.isNull(attribute) ? " IS NULL" : " = ?");

            if (i.hasNext()) {
                buffer.append(" AND ");
            }
        }

        return buffer.toString();
    }

    @Override
    protected DbAttributeBinding[] createBindings() {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        List<DbAttribute> updatedDbAttributes = updateBatch.getUpdatedAttributes();
        List<DbAttribute> qualifierAttributes = updateBatch.getQualifierAttributes();

        int ul = updatedDbAttributes.size();
        int ql = qualifierAttributes.size();

        DbAttributeBinding[] bindings = new DbAttributeBinding[ul + ql];

        for (int i = 0; i < ul; i++) {
            bindings[i] = new DbAttributeBinding(updatedDbAttributes.get(i));
        }

        for (int i = 0; i < ql; i++) {
            bindings[ul + i] = new DbAttributeBinding(qualifierAttributes.get(i));
        }

        return bindings;
    }

    @Override
    protected DbAttributeBinding[] doUpdateBindings(BatchQueryRow row) {

        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        List<DbAttribute> updatedDbAttributes = updateBatch.getUpdatedAttributes();
        List<DbAttribute> qualifierAttributes = updateBatch.getQualifierAttributes();

        int ul = updatedDbAttributes.size();
        int ql = qualifierAttributes.size();

        int j = 1;

        for (int i = 0; i < ul; i++) {
            Object value = row.getValue(i);
            ExtendedType extendedType = value != null
                    ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                    : adapter.getExtendedTypes().getDefaultType();

            bindings[i].include(j++, value, extendedType);
        }

        for (int i = 0; i < ql; i++) {

            DbAttribute a = qualifierAttributes.get(i);

            // skip null attributes... they are translated as "IS NULL"
            if (updateBatch.isNull(a)) {
                continue;
            }

            Object value = row.getValue(ul + i);
            ExtendedType extendedType = value != null
                    ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                    : adapter.getExtendedTypes().getDefaultType();

            bindings[ul + i].include(j++, value, extendedType);
        }

        return bindings;
    }
}
