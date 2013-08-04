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
package org.apache.cayenne.access.jdbc;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.cayenne.access.trans.DeleteBatchQueryBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * Implementation of {@link DeleteBatchQueryBuilder}, which uses 'soft' delete
 * (runs UPDATE and sets 'deleted' field to true instead-of running SQL DELETE)
 */
public class SoftDeleteBatchQueryBuilder extends DeleteBatchQueryBuilder {

    private String deletedFieldName;

    public SoftDeleteBatchQueryBuilder(DbAdapter adapter, String deletedFieldName) {
        super(adapter);
        this.deletedFieldName = deletedFieldName;
    }

    @Override
    public String createSqlString(BatchQuery batch) throws IOException {
        if (!needSoftDelete(batch)) {
            return super.createSqlString(batch);
        }

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(strategy.quotedFullyQualifiedName(batch.getDbEntity()));
        query.append(" SET ").append(strategy.quotedIdentifier(batch.getDbEntity(), deletedFieldName)).append(" = ?");

        applyQualifier(query, batch);

        return query.toString();
    }

    @Override
    protected int getFirstParameterIndex(BatchQuery query) {
        return needSoftDelete(query) ? 2 : 1;
    }

    @Override
    public void bindParameters(PreparedStatement statement, BatchQuery query) throws SQLException, Exception {
        if (needSoftDelete(query)) {
            // binding first parameter (which is 'deleted') as true
            adapter.bindParameter(statement, true, 1, Types.BOOLEAN, -1);
        }

        super.bindParameters(statement, query);
    }

    /**
     * @return whether 'soft' deletion should be used
     */
    protected boolean needSoftDelete(BatchQuery query) {
        DbAttribute attr = query.getDbEntity().getAttribute(deletedFieldName);
        return attr != null && attr.getType() == Types.BOOLEAN;
    }
}
