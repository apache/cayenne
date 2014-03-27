/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.batch;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;

/**
 * Translator of InsertBatchQueries.
 */
public class InsertBatchTranslator extends DefaultBatchTranslator {

    public InsertBatchTranslator(InsertBatchQuery query, DbAdapter adapter) {
        // no trimming is needed here, so passing hardcoded NULL for trim
        // function
        super(query, adapter, null);
    }

    /**
     * @since 3.2
     */
    @Override
    public List<BatchParameterBinding> createBindings(BatchQueryRow row) {

        List<DbAttribute> attributes = query.getDbAttributes();
        int len = attributes.size();

        List<BatchParameterBinding> bindings = new ArrayList<BatchParameterBinding>(len);

        for (int i = 0; i < len; i++) {
            DbAttribute a = attributes.get(i);
            if (includeInBatch(a)) {
                Object value = row.getValue(i);
                bindings.add(new BatchParameterBinding(a, value));
            }
        }

        return bindings;
    }

    @Override
    protected String createSql() {

        List<DbAttribute> dbAttributes = query.getDbAttributes();
        QuotingStrategy strategy = adapter.getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("INSERT INTO ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" (");

        int columnCount = 0;
        for (DbAttribute attribute : dbAttributes) {

            // attribute inclusion rule - one of the rules below must be true:
            // (1) attribute not generated
            // (2) attribute is generated and PK and adapter does not support
            // generated
            // keys

            if (includeInBatch(attribute)) {

                if (columnCount > 0) {
                    buffer.append(", ");
                }
                buffer.append(strategy.quotedName(attribute));
                columnCount++;
            }
        }

        buffer.append(") VALUES (");

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                buffer.append(", ");
            }

            buffer.append('?');
        }
        buffer.append(')');
        return buffer.toString();
    }

    /**
     * Returns true if an attribute should be included in the batch.
     * 
     * @since 1.2
     */
    protected boolean includeInBatch(DbAttribute attribute) {
        // attribute inclusion rule - one of the rules below must be true:
        // (1) attribute not generated
        // (2) attribute is generated and PK and adapter does not support
        // generated
        // keys

        return !attribute.isGenerated() || (attribute.isPrimaryKey() && !adapter.supportsGeneratedKeys());
    }
}
