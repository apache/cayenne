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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Batched delete query.
 * 
 */
public class DeleteBatchQuery extends BatchQuery {

    protected List<Map> qualifierSnapshots;
    protected List<DbAttribute> dbAttributes;
    protected boolean usingOptimisticLocking;

    private Collection<DbAttribute> qualifierAttributes;
    private Collection<String> nullQualifierNames;

    /**
     * Creates new DeleteBatchQuery. Used by
     * ContextCommit.categorizeFlattenedDeletesAndCreateBatches for deleting flattenned
     * relationships.
     * 
     * @param dbEntity Table or view to delete.
     * @param batchCapacity Estimated size of the batch.
     */
    public DeleteBatchQuery(DbEntity dbEntity, int batchCapacity) {
        this(dbEntity, dbEntity.getPrimaryKeys(), Collections.EMPTY_SET, batchCapacity);
    }

    /**
     * Creates new DeleteBatchQuery.
     * 
     * @param dbEntity Table or view to delete.
     * @param qualifierAttributes DbAttributes used in the WHERE clause.
     * @param nullQualifierNames DbAttribute names in the WHERE clause that have null
     *            values.
     * @param batchCapacity Estimated size of the batch.
     */
    public DeleteBatchQuery(DbEntity dbEntity,
            Collection<DbAttribute> qualifierAttributes, Collection<String> nullQualifierNames,
            int batchCapacity) {

        super(dbEntity);

        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames = nullQualifierNames != null
                ? nullQualifierNames
                : Collections.EMPTY_SET;

        qualifierSnapshots = new ArrayList<Map>(batchCapacity);
        dbAttributes = new ArrayList<DbAttribute>(qualifierAttributes.size());
        dbAttributes.addAll(qualifierAttributes);
        batchIndex = -1;
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

    /**
     * @since 3.0 (since 3.0 changed to return collection instead of a list).
     */
    public Collection<DbAttribute> getQualifierAttributes() {
        return qualifierAttributes;
    }

    @Override
    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = dbAttributes.get(dbAttributeIndex);
        return getCurrentQualifier().get(attribute.getName());
    }

    public void add(Map dataObjectId) {
        qualifierSnapshots.add(dataObjectId);
    }

    @Override
    public int size() {
        return qualifierSnapshots.size();
    }

    @Override
    public List<DbAttribute> getDbAttributes() {
        return dbAttributes;
    }

    /**
     * Returns a snapshot of the current qualifier values.
     * 
     * @since 1.2
     */
    public Map getCurrentQualifier() {
        return qualifierSnapshots.get(batchIndex);
    }
}
