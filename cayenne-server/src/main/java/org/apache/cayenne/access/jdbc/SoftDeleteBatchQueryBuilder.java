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
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Implementation of {@link DeleteBatchQueryBuilder}, which uses 'soft' delete
 * (runs UPDATE and sets 'deleted' field to true instead-of running SQL DELETE)
 */
public class SoftDeleteBatchQueryBuilder extends DeleteBatchQueryBuilder {

    private String deletedFieldName;

    public SoftDeleteBatchQueryBuilder(DeleteBatchQuery query, DbAdapter adapter, String deletedFieldName) {
        super(query, adapter);
        this.deletedFieldName = deletedFieldName;
    }

    @Override
    public String createSqlString() throws IOException {
        if (!needSoftDelete()) {
            return super.createSqlString();
        }

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("UPDATE ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" SET ").append(strategy.quotedIdentifier(query.getDbEntity(), deletedFieldName)).append(" = ?");

        applyQualifier(buffer);

        return buffer.toString();
    }

    @Override
    protected int getFirstParameterIndex() {
        return needSoftDelete() ? 2 : 1;
    }

    @Override
    public void bindParameters(PreparedStatement statement) throws SQLException, Exception {
        if (needSoftDelete()) {
            // binding first parameter (which is 'deleted') as true
            adapter.bindParameter(statement, true, 1, Types.BOOLEAN, -1);
        }

        super.bindParameters(statement);
    }

    /**
     * @return whether 'soft' deletion should be used
     */
    protected boolean needSoftDelete() {
        DbAttribute attr = query.getDbEntity().getAttribute(deletedFieldName);
        return attr != null && attr.getType() == Types.BOOLEAN;
    }
}
