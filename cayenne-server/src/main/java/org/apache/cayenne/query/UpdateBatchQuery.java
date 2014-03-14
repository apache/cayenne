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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Batched UPDATE query.
 */
public class UpdateBatchQuery extends BatchQuery {

    protected boolean usingOptimisticLocking;

    private List<DbAttribute> updatedAttributes;
    private List<DbAttribute> qualifierAttributes;
    private Collection<String> nullQualifierNames;

    private static List<DbAttribute> toDbAttributes(List<DbAttribute> qualifierAttributes,
            List<DbAttribute> updatedAttributes) {
        List<DbAttribute> dbAttributes = new ArrayList<DbAttribute>(updatedAttributes.size()
                + qualifierAttributes.size());
        dbAttributes.addAll(updatedAttributes);
        dbAttributes.addAll(qualifierAttributes);
        return dbAttributes;
    }

    /**
     * Creates new UpdateBatchQuery.
     * 
     * @param dbEntity
     *            Table or view to update.
     * @param qualifierAttributes
     *            DbAttributes used in the WHERE clause.
     * @param nullQualifierNames
     *            DbAttribute names in the WHERE clause that have null values.
     * @param updatedAttribute
     *            DbAttributes describing updated columns.
     * @param batchCapacity
     *            Estimated size of the batch.
     */
    public UpdateBatchQuery(DbEntity dbEntity, List<DbAttribute> qualifierAttributes,
            List<DbAttribute> updatedAttributes, Collection<String> nullQualifierNames, int batchCapacity) {

        super(dbEntity, toDbAttributes(qualifierAttributes, updatedAttributes), batchCapacity);

        if (nullQualifierNames == null) {
            throw new NullPointerException("Null 'nullQualifierNames'");
        }

        this.updatedAttributes = updatedAttributes;
        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames = nullQualifierNames;
    }

    /**
     * Returns true if a given attribute always has a null value in the batch.
     * 
     * @since 1.1
     */
    public boolean isNull(DbAttribute attribute) {
        return nullQualifierNames.contains(attribute.getName());
    }

    /**
     * Returns true if the batch query uses optimistic locking.
     * 
     * @since 1.1
     */
    @Override
    public boolean isUsingOptimisticLocking() {
        return usingOptimisticLocking;
    }

    /**
     * @since 1.1
     */
    public void setUsingOptimisticLocking(boolean usingOptimisticLocking) {
        this.usingOptimisticLocking = usingOptimisticLocking;
    }

    /**
     * Adds a parameter row to the batch.
     */
    public void add(Map<String, Object> qualifierSnapshot, Map<String, Object> updateSnapshot) {
        add(qualifierSnapshot, updateSnapshot, null);
    }

    /**
     * Adds a parameter row to the batch.
     * 
     * @since 1.2
     */
    public void add(Map<String, Object> qualifierSnapshot, final Map<String, Object> updateSnapshot, ObjectId id) {

        rows.add(new BatchQueryRow(id, qualifierSnapshot) {
            @Override
            public Object getValue(int i) {
                Map<String, Object> snapshot = (i < updatedAttributes.size()) ? updateSnapshot : qualifier;
                return getValue(snapshot, dbAttributes.get(i));
            }
        });
    }

    @Override
    public List<DbAttribute> getDbAttributes() {
        return dbAttributes;
    }

    /**
     * @since 1.1
     */
    public List<DbAttribute> getUpdatedAttributes() {
        return Collections.unmodifiableList(updatedAttributes);
    }

    /**
     * @since 1.1
     */
    public List<DbAttribute> getQualifierAttributes() {
        return Collections.unmodifiableList(qualifierAttributes);
    }

}
