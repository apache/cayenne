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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;

/**
 * An exception thrown on optimistic lock failure.
 * 
 * @since 1.1
 */
public class OptimisticLockException extends CayenneRuntimeException {

    protected String querySQL;
    protected DbEntity rootEntity;
    protected Map qualifierSnapshot;

    public OptimisticLockException(DbEntity rootEntity, String querySQL,
            Map qualifierSnapshot) {
        super("Optimistic Lock Failure");

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
            Expression attributeQualifier = ExpressionFactory.matchDbExp(attribute
                    .getName(), qualifierSnapshot.get(attribute.getName()));

            qualifier = (qualifier != null)
                    ? qualifier.andExp(attributeQualifier)
                    : attributeQualifier;
        }

        SelectQuery query = new SelectQuery(rootEntity, qualifier);
        query.setFetchingDataRows(true);
        return (Map<?, ?>) DataObjectUtils.objectForQuery(context, query);
    }
    
    /**
     * Retrieves fresh snapshot for the failed row. Null row indicates that it was
     * deleted.
     * 
     * @deprecated since 3.0 use {@link #getFreshSnapshot(ObjectContext)} instead.
     */
    public Map getFreshSnapshot(QueryEngine engine) {

        // extract PK from the qualifierSnapshot and fetch a row
        // for PK, ignoring other locking attributes...

        Expression qualifier = null;
        for (DbAttribute attribute : rootEntity.getPrimaryKeys()) {
            Expression attributeQualifier = ExpressionFactory.matchDbExp(attribute
                    .getName(), qualifierSnapshot.get(attribute.getName()));

            qualifier = (qualifier != null)
                    ? qualifier.andExp(attributeQualifier)
                    : attributeQualifier;
        }

        SelectQuery query = new SelectQuery(rootEntity, qualifier);
        query.setFetchingDataRows(true);
        QueryResult observer = new QueryResult();
        engine.performQueries(Collections.singletonList((Query) query), observer);
        List results = observer.getFirstRows(query);

        if (results == null || results.isEmpty()) {
            return null;
        }
        else if (results.size() > 1) {
            throw new CayenneRuntimeException("More than one row for ObjectId.");
        }
        else {
            return (Map) results.get(0);
        }
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
                QueryLogger.sqlLiteralForObject(buffer, entry.getValue());

                if (it.hasNext()) {
                    buffer.append(", ");
                }
            }
            buffer.append("]");
        }

        return buffer.toString();
    }
}
