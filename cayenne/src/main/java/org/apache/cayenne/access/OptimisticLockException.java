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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.translator.TranslatedStatement;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * An exception thrown on optimistic lock failure.
 *
 * @since 1.1
 */
public class OptimisticLockException extends CayenneRuntimeException {

    private final ObjectId failedObjectId;
    private final DbEntity rootEntity;
    private final Map<String, ?> qualifierSnapshot;
    private final TranslatedStatement statement;

    public OptimisticLockException(ObjectId id, DbEntity rootEntity, Map<String, ?> qualifierSnapshot, TranslatedStatement statement) {
        super("Optimistic Lock Failure");

        this.failedObjectId = id;
        this.rootEntity = rootEntity;
        this.qualifierSnapshot = qualifierSnapshot != null ? qualifierSnapshot : Collections.emptyMap();
        this.statement = Objects.requireNonNull(statement);
    }

    /**
     * @since 5.0
     */
    public TranslatedStatement getStatement() {
        return statement;
    }

    public Map getQualifierSnapshot() {
        return qualifierSnapshot;
    }

    /**
     * @deprecated in favor of {@link #getStatement()}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public String getQuerySQL() {
        return statement.sql();
    }

    /**
     * Retrieves fresh snapshot for the failed row. Null row indicates that it was deleted.
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

        return ObjectSelect.dbQuery(rootEntity.getName(), qualifier)
                .fetchDataRows()
                .selectFirst(context);
    }

    /**
     * Returns descriptive message for this exception.
     */
    @Override
    public String getMessage() {
        StringBuilder buffer = new StringBuilder(super.getMessage());
        buffer.append(", SQL: [").append(statement.sql()).append("]");

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
