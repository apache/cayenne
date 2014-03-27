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

import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

/**
 * Implementation of {@link DeleteBatchTranslator}, which uses 'soft' delete
 * (runs UPDATE and sets 'deleted' field to true instead-of running SQL DELETE)
 */
public class SoftDeleteBatchTranslator extends DeleteBatchTranslator {

    private String deletedFieldName;

    public SoftDeleteBatchTranslator(DeleteBatchQuery query, DbAdapter adapter, String trimFunction,
            String deletedFieldName) {
        super(query, adapter, trimFunction);
        this.deletedFieldName = deletedFieldName;
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
    public List<BatchParameterBinding> createBindings(BatchQueryRow row) {

        List<BatchParameterBinding> bindings = super.createBindings(row);

        DbAttribute deleteAttribute = query.getDbEntity().getAttribute(deletedFieldName);
        bindings.add(0, new BatchParameterBinding(deleteAttribute, true));

        return bindings;
    }

}
