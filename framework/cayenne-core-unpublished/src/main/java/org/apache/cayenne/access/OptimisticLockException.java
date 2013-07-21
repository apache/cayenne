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

package org.apache.cayenne.access;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SelectQuery;

/**
 * An exception thrown on optimistic lock failure.
 * 
 * @since 1.1
 */
public class OptimisticLockException extends CayenneRuntimeException {

    protected ObjectId failedObjectId;
    protected String querySQL;
    protected DbEntity rootEntity;
    protected Map qualifierSnapshot;

    public OptimisticLockException(ObjectId id, DbEntity rootEntity, String querySQL,
            Map qualifierSnapshot) {
        super("Optimistic Lock Failure");

        this.failedObjectId = id;
        this.rootEntity = rootEntity;
        this.querySQL = querySQL;
        this.qualifierSnapshot = (qualifierSnapshot != null)
                ? qualifierSnapshot
                : Collections.EMPTY_MAP;
    }

    public Map getQualifierSnapshot() {
        return qualifierSnapshot;
    }

    public String getQuerySQL() {
        return querySQL;
    }

    /**
     * Retrieves fresh snapshot for the failed row. Null row indicates that it was
     * deleted.
     * 
     * @since 3.0
     */
    public Map<?, ?> getFreshSnapshot(ObjectContext context) {

        Expression qualifier = null;
        for (DbAttribute attribute : rootEntity.getPrimaryKeys()) {
            Expression attributeQualifier = ExpressionFactory.matchDbExp(
                    attribute.getName(),
                    qualifierSnapshot.get(attribute.getName()));

            qualifier = (qualifier != null)
                    ? qualifier.andExp(attributeQualifier)
                    : attributeQualifier;
        }

        SelectQuery<DataRow> query = new SelectQuery<DataRow>(rootEntity, qualifier);
        query.setFetchingDataRows(true);
        return (Map<?, ?>) Cayenne.objectForQuery(context, query);
    }

    /**
     * Returns descriptive message for this exception.
     */
    @Override
    public String getMessage() {
        StringBuffer buffer = new StringBuffer(super.getMessage());

        if (querySQL != null) {
            buffer.append(", SQL: [").append(querySQL.trim()).append("]");
        }

        if (!qualifierSnapshot.isEmpty()) {
            buffer.append(", WHERE clause bindings: [");
            Iterator it = qualifierSnapshot.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                buffer.append(entry.getKey()).append("=");
                buffer.append(entry.getValue());

                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append("]");
        }

        return buffer.toString();
    }

    /**
     * Returns the ObjectId of the object that caused the OptimisticLockException.
     * 
     * @since 3.1
     */
    public ObjectId getFailedObjectId() {
        return failedObjectId;
    }
}
