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
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * Translator of InsertBatchQueries.
 */
public class InsertBatchQueryBuilder extends BatchQueryBuilder {

    public InsertBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    /**
     * Binds parameters for the current batch iteration to the PreparedStatement. Performs
     * filtering of attributes based on column generation rules.
     * 
     * @since 1.2
     */
    @Override
    public void bindParameters(PreparedStatement statement, BatchQuery query)
            throws SQLException, Exception {

        List<DbAttribute> dbAttributes = query.getDbAttributes();
        int attributeCount = dbAttributes.size();

        // must use an independent counter "j" for prepared statement index
        for (int i = 0, j = 0; i < attributeCount; i++) {
            DbAttribute attribute = dbAttributes.get(i);
            if (includeInBatch(attribute)) {
                j++;
                Object value = query.getValue(i);
                adapter.bindParameter(statement, value, j, attribute.getType(), attribute
                        .getScale());
            }
        }
    }

    /**
     * Returns a list of values for the current batch iteration. Performs filtering of
     * attributes based on column generation rules. Used primarily for logging.
     * 
     * @since 1.2
     */
    @Override
    public List<Object> getParameterValues(BatchQuery query) {
        List<DbAttribute> attributes = query.getDbAttributes();
        int len = attributes.size();
        List<Object> values = new ArrayList<Object>(len);
        for (int i = 0; i < len; i++) {
            DbAttribute attribute = attributes.get(i);
            if (includeInBatch(attribute)) {
                values.add(query.getValue(i));
            }
        }
        return values;
    }

    @Override
    public String createSqlString(BatchQuery batch) throws IOException {

        List<DbAttribute> dbAttributes = batch.getDbAttributes();
        QuotingStrategy strategy =  getAdapter().getQuotingStrategy();

        StringBuilder query = new StringBuilder("INSERT INTO ");
        query.append(strategy.quotedFullyQualifiedName(batch.getDbEntity()));
        query.append(" (");

        int columnCount = 0;
        for (DbAttribute attribute : dbAttributes) {

            // attribute inclusion rule - one of the rules below must be true:
            // (1) attribute not generated
            // (2) attribute is generated and PK and adapter does not support generated
            // keys

            if (includeInBatch(attribute)) {

                if (columnCount > 0) {
                    query.append(", ");
                }
                query.append(strategy.quotedName(attribute));
                columnCount++;
            }
        }

        query.append(") VALUES (");

        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                query.append(", ");
            }

            query.append('?');
        }
        query.append(')');
        return query.toString();
    }

    /**
     * Returns true if an attribute should be included in the batch.
     * 
     * @since 1.2
     */
    protected boolean includeInBatch(DbAttribute attribute) {
        // attribute inclusion rule - one of the rules below must be true:
        // (1) attribute not generated
        // (2) attribute is generated and PK and adapter does not support generated
        // keys

        return !attribute.isGenerated()
                || (attribute.isPrimaryKey() && !adapter.supportsGeneratedKeys());
    }
}
