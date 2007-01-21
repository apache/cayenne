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
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Translator for delete BatchQueries. Creates parametrized DELETE SQL statements.
 * 
 * @author Andriy Shapochka, Andrus Adamchik, Mike Kienenberger
 */

public class DeleteBatchQueryBuilder extends BatchQueryBuilder {

    public DeleteBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public String createSqlString(BatchQuery batch) {
        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List qualifierAttributes = deleteBatch.getQualifierAttributes();

        StringBuffer query = new StringBuffer("DELETE FROM ");
        query.append(table).append(" WHERE ");

        Iterator i = qualifierAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            query.append(deleteBatch.isNull(attribute) ? " IS NULL" : " = ?");

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

        DeleteBatchQuery deleteBatch = (DeleteBatchQuery) query;
        List qualifierAttributes = deleteBatch.getQualifierAttributes();

        int parameterIndex = 1;

        for (int i = 0; i < qualifierAttributes.size(); i++) {
            Object value = query.getValue(i);
            DbAttribute attribute = (DbAttribute) qualifierAttributes.get(i);

            // skip null attributes... they are translated as "IS NULL"
            if (deleteBatch.isNull(attribute)) {
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
