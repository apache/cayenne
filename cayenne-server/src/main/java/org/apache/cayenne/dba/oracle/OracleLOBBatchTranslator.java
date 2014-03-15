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

package org.apache.cayenne.dba.oracle;

import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * Superclass of query builders for the DML operations involving LOBs.
 * 
 */
abstract class OracleLOBBatchTranslator extends BatchTranslator {

    protected String newClobFunction;
    protected String newBlobFunction;

    OracleLOBBatchTranslator(BatchQuery query, DbAdapter adapter) {
        super(query, adapter);
    }

    abstract List<Object> getValuesForLOBUpdateParameters(BatchQueryRow row);

    abstract String createSqlString(BatchQueryRow row);

    @Override
    public final String createSqlString() throws IOException {
        throw new UnsupportedOperationException();
    }

    String createLOBSelectString(List<DbAttribute> selectedLOBAttributes, List<DbAttribute> qualifierAttributes) {

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        StringBuilder buf = new StringBuilder();
        buf.append("SELECT ");

        Iterator<DbAttribute> it = selectedLOBAttributes.iterator();
        while (it.hasNext()) {
            buf.append(strategy.quotedName(it.next()));

            if (it.hasNext()) {
                buf.append(", ");
            }
        }

        buf.append(" FROM ").append(strategy.quotedFullyQualifiedName(query.getDbEntity())).append(" WHERE ");

        it = qualifierAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            appendDbAttribute(buf, attribute);
            buf.append(" = ?");
            if (it.hasNext()) {
                buf.append(" AND ");
            }
        }

        buf.append(" FOR UPDATE");
        return buf.toString();
    }

    /**
     * Appends parameter placeholder for the value of the column being updated.
     * If requested, performs special handling on LOB columns.
     */
    protected void appendUpdatedParameter(StringBuilder buf, DbAttribute dbAttribute, Object value) {

        int type = dbAttribute.getType();

        if (isUpdateableColumn(value, type)) {
            buf.append('?');
        } else {
            if (type == Types.CLOB) {
                buf.append(newClobFunction);
            } else if (type == Types.BLOB) {
                buf.append(newBlobFunction);
            } else {
                throw new CayenneRuntimeException("Unknown LOB column type: " + type + "("
                        + TypesMapping.getSqlNameByType(type) + "). Query buffer: " + buf);
            }
        }
    }

    @Override
    public List<BatchParameterBinding> createBindings(BatchQueryRow row) {
        List<DbAttribute> dbAttributes = query.getDbAttributes();
        int len = dbAttributes.size();

        List<BatchParameterBinding> bindings = new ArrayList<BatchParameterBinding>(len);

        for (int i = 0; i < len; i++) {
            Object value = row.getValue(i);
            DbAttribute attribute = dbAttributes.get(i);
            int type = attribute.getType();

            // TODO: (Andrus) This works as long as there is no LOBs in
            // qualifier
            if (isUpdateableColumn(value, type)) {
                bindings.add(new BatchParameterBinding(attribute, value));
            }
        }

        return bindings;
    }

    protected boolean isUpdateableColumn(Object value, int type) {
        return value == null || (type != Types.BLOB && type != Types.CLOB);
    }

    void setNewBlobFunction(String string) {
        newBlobFunction = string;
    }

    void setNewClobFunction(String string) {
        newClobFunction = string;
    }
}
