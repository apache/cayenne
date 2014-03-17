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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Translator for delete BatchQueries. Creates parameterized DELETE SQL
 * statements.
 */
public class DeleteBatchTranslator extends DefaultBatchTranslator {

    public DeleteBatchTranslator(DeleteBatchQuery query, DbAdapter adapter, String trimFunction) {
        super(query, adapter, trimFunction);
    }

    @Override
    public String createSqlString() throws IOException {

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("DELETE FROM ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));

        applyQualifier(buffer);

        return buffer.toString();
    }

    /**
     * Appends WHERE clause to SQL string
     */
    protected void applyQualifier(StringBuilder buffer) {
        buffer.append(" WHERE ");

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;
        Iterator<DbAttribute> i = deleteBatch.getDbAttributes().iterator();
        while (i.hasNext()) {
            DbAttribute attribute = i.next();
            appendDbAttribute(buffer, attribute);
            buffer.append(deleteBatch.isNull(attribute) ? " IS NULL" : " = ?");

            if (i.hasNext()) {
                buffer.append(" AND ");
            }
        }
    }

    /**
     * @since 3.2
     */
    @Override
    public List<BatchParameterBinding> createBindings(BatchQueryRow row) {

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;
        List<DbAttribute> attributes = deleteBatch.getDbAttributes();
        int len = attributes.size();

        List<BatchParameterBinding> bindings = new ArrayList<BatchParameterBinding>(len);

        for (int i = 0; i < len; i++) {

            DbAttribute a = attributes.get(i);

            // skip null attributes... they are translated as "IS NULL"
            if (deleteBatch.isNull(a)) {
                continue;
            }

            Object value = row.getValue(i);
            bindings.add(new BatchParameterBinding(a, value));
        }

        return bindings;
    }
}
