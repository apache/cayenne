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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * Superclass of batch query translators.
 */
public abstract class BatchQueryBuilder {

    protected BatchQuery query;
    protected DbAdapter adapter;
    protected String trimFunction;

    public BatchQueryBuilder(BatchQuery query, DbAdapter adapter) {
        this.query = query;
        this.adapter = adapter;
    }

    /**
     * Translates BatchQuery into an SQL string formatted to use in a
     * PreparedStatement.
     * 
     * @since 3.2
     * @throws IOException
     */
    public abstract String createSqlString() throws IOException;

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

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        buf.append(strategy.quotedName(dbAttribute));

        if (trim) {
            buf.append(')');
        }
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public String getTrimFunction() {
        return trimFunction;
    }

    public void setTrimFunction(String string) {
        trimFunction = string;
    }

    /**
     * Binds parameters for the current batch iteration to the
     * PreparedStatement.
     * 
     * @since 3.2
     */
    public abstract void bindParameters(PreparedStatement statement, BatchQueryRow row) throws SQLException, Exception;

    /**
     * Returns a list of values for the current batch iteration. Used primarily
     * for logging.
     * 
     * @since 1.2
     */
    public List<Object> getParameterValues(BatchQueryRow row) {
        int len = query.getDbAttributes().size();
        List<Object> values = new ArrayList<Object>(len);
        for (int i = 0; i < len; i++) {
            values.add(row.getValue(i));
        }
        return values;
    }

}
