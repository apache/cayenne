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

package org.apache.cayenne.query;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Batched delete query.
 * 
 */
public class DeleteBatchQuery extends BatchQuery {

    protected boolean usingOptimisticLocking;

    private Collection<String> nullQualifierNames;

    /**
     * Creates new DeleteBatchQuery.
     * 
     * @param dbEntity
     *            Table or view to delete.
     * @param qualifierAttributes
     *            DbAttributes used in the WHERE clause.
     * @param nullQualifierNames
     *            DbAttribute names in the WHERE clause that have null values.
     * @param batchCapacity
     *            Estimated size of the batch.
     */
    public DeleteBatchQuery(DbEntity dbEntity, List<DbAttribute> qualifierAttributes,
            Collection<String> nullQualifierNames, int batchCapacity) {

        super(dbEntity, qualifierAttributes, batchCapacity);

        if (nullQualifierNames == null) {
            throw new NullPointerException("Null 'nullQualifierNames'");
        }

        this.nullQualifierNames = nullQualifierNames;
    }

    /**
     * Returns true if a given attribute always has a null value in the batch.
     * 
     * @since 1.2
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.2
     */
    @Override
    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    /**
     * @since 1.2
     */
    public void setUsingOptimisticLocking(boolean usingOptimisticLocking) {
        this.usingOptimisticLocking = usingOptimisticLocking;
    }

    public void add(Map<String, Object> dataObjectId) {

        rows.add(new BatchQueryRow(null, dataObjectId) {
            @Override
            public Object getValue(int i) {
                return qualifier.get(dbAttributes.get(i).getName());
            }
        });
    }
}
