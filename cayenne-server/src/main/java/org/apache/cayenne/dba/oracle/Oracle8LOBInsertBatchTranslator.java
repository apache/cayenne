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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.InsertBatchQuery;

class Oracle8LOBInsertBatchTranslator extends Oracle8LOBBatchTranslator {

    Oracle8LOBInsertBatchTranslator(InsertBatchQuery query, DbAdapter adapter, String trimFunction) {
        super(query, adapter, trimFunction);
    }

    @Override
    List<Object> getValuesForLOBUpdateParameters(BatchQueryRow row) {
        List<DbAttribute> dbAttributes = query.getDbAttributes();
        int len = dbAttributes.size();

        List<Object> values = new ArrayList<Object>(len);
        for (int i = 0; i < len; i++) {
            Object value = row.getValue(i);
            DbAttribute attribute = dbAttributes.get(i);
            if (isUpdateableColumn(value, attribute.getType())) {
                values.add(value);
            }
        }

        return values;
    }

    @Override
    public String createSql(BatchQueryRow row) {
        List<DbAttribute> dbAttributes = query.getDbAttributes();

        QuotingStrategy strategy = adapter.getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("INSERT INTO ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" (");

        for (Iterator<DbAttribute> i = dbAttributes.iterator(); i.hasNext();) {
            DbAttribute attribute = i.next();
            buffer.append(strategy.quotedName(attribute));
            if (i.hasNext()) {
                buffer.append(", ");
            }
        }
        buffer.append(") VALUES (");
        for (int i = 0; i < dbAttributes.size(); i++) {
            if (i > 0) {
                buffer.append(", ");
            }

            appendUpdatedParameter(buffer, dbAttributes.get(i), row.getValue(i));
        }
        buffer.append(')');
        return buffer.toString();
    }
}
