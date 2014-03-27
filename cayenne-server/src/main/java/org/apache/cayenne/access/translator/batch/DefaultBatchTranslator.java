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

import java.sql.Types;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * Superclass of batch query translators.
 * 
 * @since 3.2
 */
public abstract class DefaultBatchTranslator implements BatchTranslator {

    protected BatchQuery query;
    protected DbAdapter adapter;
    protected String trimFunction;

    protected boolean translated;
    protected String sql;
    protected BatchParameterBinding[] bindings;

    public DefaultBatchTranslator(BatchQuery query, DbAdapter adapter, String trimFunction) {
        this.query = query;
        this.adapter = adapter;
        this.trimFunction = trimFunction;
    }

    protected void ensureTranslated() {
        if (!translated) {
            this.sql = createSql();
            this.bindings = createBindings();
            translated = true;
        }
    }

    /**
     * Translates BatchQuery into an SQL string formatted to use in a
     * PreparedStatement.
     */
    @Override
    public String getSql() {
        ensureTranslated();
        return sql;
    }

    @Override
    public BatchParameterBinding[] getBindings() {
        ensureTranslated();
        return bindings;
    }
    
    @Override
    public BatchParameterBinding[] updateBindings(BatchQueryRow row) {
        ensureTranslated();
        return doUpdateBindings(row);
    }

    protected abstract String createSql();

    protected abstract BatchParameterBinding[] createBindings();
    
    protected abstract BatchParameterBinding[] doUpdateBindings(BatchQueryRow row);

    /**
     * Appends the name of the column to the query buffer. Subclasses use this
     * method to append column names in the WHERE clause, i.e. for the columns
     * that are not being updated.
     */
    protected void appendDbAttribute(StringBuilder buf, DbAttribute dbAttribute) {

        // TODO: (Andrus) is there a need for trimming binary types?
        boolean trim = dbAttribute.getType() == Types.CHAR && trimFunction != null;
        if (trim) {
            buf.append(trimFunction).append('(');
        }

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        buf.append(strategy.quotedName(dbAttribute));

        if (trim) {
            buf.append(')');
        }
    }

}
