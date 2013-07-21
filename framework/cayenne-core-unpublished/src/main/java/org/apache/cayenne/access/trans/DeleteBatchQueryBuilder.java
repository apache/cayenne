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

package org.apache.cayenne.access.trans;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Translator for delete BatchQueries. Creates parameterized DELETE SQL
 * statements.
 */
public class DeleteBatchQueryBuilder extends BatchQueryBuilder {

    public DeleteBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public String createSqlString(BatchQuery batch) throws IOException {

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        StringBuffer query = new StringBuffer("DELETE FROM ");
        query.append(strategy.quotedFullyQualifiedName(batch.getDbEntity()));

        applyQualifier(query, batch);

        return query.toString();
    }

    /**
     * Appends WHERE clause to SQL string
     */
    protected void applyQualifier(StringBuffer query, BatchQuery batch) {
        query.append(" WHERE ");

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) batch;
        Iterator<DbAttribute> i = deleteBatch.getQualifierAttributes().iterator();
        while (i.hasNext()) {
            DbAttribute attribute = i.next();
            appendDbAttribute(query, attribute);
            query.append(deleteBatch.isNull(attribute) ? " IS NULL" : " = ?");

            if (i.hasNext()) {
                query.append(" AND ");
            }
        }
    }

    /**
     * Binds BatchQuery parameters to the PreparedStatement.
     */
    @Override
    public void bindParameters(PreparedStatement statement, BatchQuery query) throws SQLException, Exception {

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;

        int parameterIndex = getFirstParameterIndex(query);
        int i = 0;

        for (DbAttribute attribute : deleteBatch.getQualifierAttributes()) {
            Object value = query.getValue(i++);

            // skip null attributes... they are translated as "IS NULL"
            if (deleteBatch.isNull(attribute)) {
                continue;
            }

            adapter.bindParameter(statement, value, parameterIndex++, attribute.getType(), attribute.getScale());
        }
    }

    /**
     * @return index of first parameter in delete clause
     */
    protected int getFirstParameterIndex(BatchQuery query) {
        return 1;
    }

}
