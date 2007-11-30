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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * Superclass of batch query translators.
 * 
 * @author Andriy Shapochka, Andrus Adamchik
 */

public abstract class BatchQueryBuilder {

    protected DbAdapter adapter;
    protected String trimFunction;

    public BatchQueryBuilder() {
    }

    public BatchQueryBuilder(DbAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Translates BatchQuery into an SQL string formatted to use in a PreparedStatement.
     */
    public abstract String createSqlString(BatchQuery batch);

    /**
     * Appends the name of the column to the query buffer. Subclasses use this method to
     * append column names in the WHERE clause, i.e. for the columns that are not being
     * updated.
     */
    protected void appendDbAttribute(StringBuffer buf, DbAttribute dbAttribute) {

        // TODO: (Andrus) is there a need for trimming binary types?
        boolean trim = dbAttribute.getType() == Types.CHAR && trimFunction != null;
        if (trim) {
            buf.append(trimFunction).append('(');
        }

        buf.append(dbAttribute.getName());

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
     * Binds parameters for the current batch iteration to the PreparedStatement.
     * 
     * @since 1.2
     */
    public void bindParameters(PreparedStatement statement, BatchQuery query)
            throws SQLException, Exception {

        List dbAttributes = query.getDbAttributes();
        int attributeCount = dbAttributes.size();

        for (int i = 0; i < attributeCount; i++) {
            Object value = query.getValue(i);
            DbAttribute attribute = (DbAttribute) dbAttributes.get(i);
            adapter.bindParameter(statement, value, i + 1, attribute.getType(), attribute
                    .getScale());

        }
    }

    /**
     * Returns a list of values for the current batch iteration. Used primarily for
     * logging.
     * 
     * @since 1.2
     */
    public List getParameterValues(BatchQuery query) {
        int len = query.getDbAttributes().size();
        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            values.add(query.getValue(i));
        }
        return values;
    }
}
