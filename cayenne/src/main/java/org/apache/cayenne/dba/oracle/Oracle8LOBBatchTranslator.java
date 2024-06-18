/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.oracle;

import java.sql.Types;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.batch.legacy.DefaultBatchTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * Superclass of query builders for the DML operations involving LOBs.
 * TODO: update to the new batch translation logic
 */
abstract class Oracle8LOBBatchTranslator extends DefaultBatchTranslator {

    protected String newClobFunction;
    protected String newBlobFunction;

    Oracle8LOBBatchTranslator(BatchQuery query, DbAdapter adapter, String trimFunction) {
        super(query, adapter, trimFunction);
    }

    abstract List<Object> getValuesForLOBUpdateParameters(BatchQueryRow row);

    abstract String createSql(BatchQueryRow row);

    @Override
    protected String createSql() {
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
            DbAttribute attribute = it.next();
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
                throw new CayenneRuntimeException("Unknown LOB column type: %s(%s). Query buffer: %s."
                         , type, TypesMapping.getSqlNameByType(type), buf);
            }
        }
    }
    
    @Override
    protected DbAttributeBinding[] createBindings() {
        List<DbAttribute> dbAttributes = query.getDbAttributes();
        int len = dbAttributes.size();

        DbAttributeBinding[] bindings = new DbAttributeBinding[len];

        for (int i = 0; i < len; i++) {
            bindings[i] = new DbAttributeBinding(dbAttributes.get(i));
        }

        return bindings;
    }
    
    @Override
    protected DbAttributeBinding[] doUpdateBindings(BatchQueryRow row) {

        int len = bindings.length;

        for (int i = 0, j = 1; i < len; i++) {

            DbAttributeBinding b = bindings[i];

            Object value = row.getValue(i);
            DbAttribute attribute = b.getAttribute();
            int type = attribute.getType();

            // TODO: (Andrus) This works as long as there is no LOBs in
            // qualifier
            if (isUpdateableColumn(value, type)) {
                ExtendedType extendedType = value != null
                        ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                        : adapter.getExtendedTypes().getDefaultType();

                b.include(j++, value, extendedType);
            } else {
                b.exclude();
            }
        }

        return bindings;
    }

    protected boolean isUpdateableColumn(Object value, int type) {
        return value == null || type != Types.BLOB && type != Types.CLOB;
    }

    void setNewBlobFunction(String string) {
        newBlobFunction = string;
    }

    void setNewClobFunction(String string) {
        newClobFunction = string;
    }
}
