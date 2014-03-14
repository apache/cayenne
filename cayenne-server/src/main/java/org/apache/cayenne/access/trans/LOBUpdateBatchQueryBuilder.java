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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

public class LOBUpdateBatchQueryBuilder extends LOBBatchQueryBuilder {

    public LOBUpdateBatchQueryBuilder(UpdateBatchQuery query, DbAdapter adapter) {
        super(query, adapter);
    }

    @Override
    public List getValuesForLOBUpdateParameters() {
        int len = query.getDbAttributes().size();
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        List values = new ArrayList(len);
        List<DbAttribute> qualifierAttributes = updateBatch.getQualifierAttributes();
        List<DbAttribute> updatedDbAttributes = updateBatch.getUpdatedAttributes();

        int updatedLen = updatedDbAttributes.size();
        int qualifierLen = qualifierAttributes.size();
        for (int i = 0; i < updatedLen; i++) {
            DbAttribute attribute = updatedDbAttributes.get(i);
            Object value = query.getValue(i);
            if (isUpdateableColumn(value, attribute.getType())) {
                values.add(value);
            }
        }

        for (int i = 0; i < qualifierLen; i++) {
            values.add(query.getValue(updatedLen + i));
        }

        return values;
    }

    @Override
    public String createSqlString() {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;
        List<DbAttribute> idDbAttributes = updateBatch.getQualifierAttributes();
        List<DbAttribute> updatedDbAttributes = updateBatch.getUpdatedAttributes();

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        StringBuilder buffer = new StringBuilder("UPDATE ");
        buffer.append(strategy.quotedFullyQualifiedName(query.getDbEntity()));
        buffer.append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                buffer.append(", ");
            }

            DbAttribute attribute = updatedDbAttributes.get(i);
            buffer.append(strategy.quotedName(attribute));
            buffer.append(" = ");
            appendUpdatedParameter(buffer, attribute, query.getValue(i));
        }

        buffer.append(" WHERE ");
        Iterator<DbAttribute> i = idDbAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = i.next();
            appendDbAttribute(buffer, attribute);
            buffer.append(" = ?");
            if (i.hasNext()) {
                buffer.append(" AND ");
            }
        }
        return buffer.toString();
    }
}
