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

public class LOBInsertBatchQueryBuilder extends LOBBatchQueryBuilder {

    public LOBInsertBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    @Override
    public List getValuesForLOBUpdateParameters(BatchQuery query) {
        List<DbAttribute> dbAttributes = query.getDbAttributes();
        int len = dbAttributes.size();

        List values = new ArrayList(len);
        for (int i = 0; i < len; i++) {
            Object value = query.getValue(i);
            DbAttribute attribute = dbAttributes.get(i);
            if (isUpdateableColumn(value, attribute.getType())) {
                values.add(value);
            }
        }

        return values;
    }

    @Override
    public String createSqlString(BatchQuery batch) {
        List<DbAttribute> dbAttributes = batch.getDbAttributes();

        QuotingStrategy strategy = getAdapter().getQuotingStrategy();

        StringBuffer query = new StringBuffer("INSERT INTO ");
        query.append(strategy.quotedFullyQualifiedName(batch.getDbEntity()));
        query.append(" (");

        for (Iterator<DbAttribute> i = dbAttributes.iterator(); i.hasNext();) {
            DbAttribute attribute = i.next();
            query.append(strategy.quotedName(attribute));
            if (i.hasNext()) {
                query.append(", ");
            }
        }
        query.append(") VALUES (");
        for (int i = 0; i < dbAttributes.size(); i++) {
            if (i > 0) {
                query.append(", ");
            }

            appendUpdatedParameter(query, dbAttributes.get(i), batch.getValue(i));
        }
        query.append(')');
        return query.toString();
    }
}
