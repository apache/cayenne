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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * A translator for UpdateBatchQueries that produces parameterized SQL.
 * 
 * @author Andriy Shapochka, Andrus Adamchik, Mike Kienenberger
 */

public class UpdateBatchQueryBuilder extends BatchQueryBuilder {

    public UpdateBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public String createSqlString(BatchQuery batch) {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List qualifierAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();

        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(table).append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                query.append(", ");
            }

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            query.append(attribute.getName()).append(" = ?");
        }

        query.append(" WHERE ");

        Iterator i = qualifierAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            query.append(updateBatch.isNull(attribute) ? " IS NULL" : " = ?");

            if (i.hasNext()) {
                query.append(" AND ");
            }
        }

        return query.toString();
    }

    /**
     * Binds BatchQuery parameters to the PreparedStatement.
     */
    public void bindParameters(PreparedStatement statement, BatchQuery query)
            throws SQLException, Exception {

        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;
        List qualifierAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();

        int len = updatedDbAttributes.size();
        int parameterIndex = 1;
        for (int i = 0; i < len; i++) {
            Object value = query.getValue(i);

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            adapter.bindParameter(
                    statement,
                    value,
                    parameterIndex++,
                    attribute.getType(),
                    attribute.getScale());
        }

        for (int i = 0; i < qualifierAttributes.size(); i++) {
            Object value = query.getValue(len + i);
            DbAttribute attribute = (DbAttribute) qualifierAttributes.get(i);

            // skip null attributes... they are translated as "IS NULL"
            if (updateBatch.isNull(attribute)) {
                continue;
            }

            adapter.bindParameter(
                    statement,
                    value,
                    parameterIndex++,
                    attribute.getType(),
                    attribute.getScale());
        }
    }
}
