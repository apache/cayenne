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
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * @author Andrus Adamchik
 */
public class LOBUpdateBatchQueryBuilder extends LOBBatchQueryBuilder {

    public LOBUpdateBatchQueryBuilder(DbAdapter adapter) {
        super(adapter);
    }

    public List getValuesForLOBUpdateParameters(BatchQuery query) {
        int len = query.getDbAttributes().size();
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) query;

        List values = new ArrayList(len);
        List qualifierAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();

        int updatedLen = updatedDbAttributes.size();
        int qualifierLen = qualifierAttributes.size();
        for (int i = 0; i < updatedLen; i++) {
            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            Object value = query.getValue(i);
            if(isUpdateableColumn(value, attribute.getType())) {
            	values.add(value);
            }
        }

		for (int i = 0; i < qualifierLen; i++) {
			values.add(query.getValue(updatedLen + i));
        }

        return values;
    }

    public String createSqlString(BatchQuery batch) {
        UpdateBatchQuery updateBatch = (UpdateBatchQuery) batch;
        String table = batch.getDbEntity().getFullyQualifiedName();
        List idDbAttributes = updateBatch.getQualifierAttributes();
        List updatedDbAttributes = updateBatch.getUpdatedAttributes();
        StringBuffer query = new StringBuffer("UPDATE ");
        query.append(table).append(" SET ");

        int len = updatedDbAttributes.size();
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                query.append(", ");
            }

            DbAttribute attribute = (DbAttribute) updatedDbAttributes.get(i);
            query.append(attribute.getName()).append(" = ");
            appendUpdatedParameter(query, attribute, batch.getValue(i));
        }

        query.append(" WHERE ");
        Iterator i = idDbAttributes.iterator();
        while (i.hasNext()) {
            DbAttribute attribute = (DbAttribute) i.next();
            appendDbAttribute(query, attribute);
            query.append(" = ?");
            if (i.hasNext()) {
                query.append(" AND ");
            }
        }
        return query.toString();
    }
}
