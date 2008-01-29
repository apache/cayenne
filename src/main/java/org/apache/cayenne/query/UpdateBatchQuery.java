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
 * 
 * @author Andriy Shapochka
 */
public class UpdateBatchQuery extends BatchQuery {

    /**
     * @since 1.2
     */
    protected List<ObjectId> objectIds;

    protected List<Map> qualifierSnapshots;
    protected List<Map> updateSnapshots;

    protected boolean usingOptimisticLocking;

    private List<DbAttribute> updatedAttributes;
    private List<DbAttribute> qualifierAttributes;
    private Collection<String> nullQualifierNames;
    private List<DbAttribute> dbAttributes;

    /**
     * Creates new UpdateBatchQuery.
     * 
     * @param dbEntity Table or view to update.
     * @param qualifierAttributes DbAttributes used in the WHERE clause.
     * @param nullQualifierNames DbAttribute names in the WHERE clause that have null
     *            values.
     * @param updatedAttribute DbAttributes describing updated columns.
     * @param batchCapacity Estimated size of the batch.
     */
    public UpdateBatchQuery(DbEntity dbEntity, List<DbAttribute> qualifierAttributes,
            List<DbAttribute> updatedAttribute, Collection<String> nullQualifierNames, int batchCapacity) {

        super(dbEntity);

        this.updatedAttributes = updatedAttribute;
        this.qualifierAttributes = qualifierAttributes;
        this.nullQualifierNames = nullQualifierNames != null
                ? nullQualifierNames
                : Collections.EMPTY_SET;

        qualifierSnapshots = new ArrayList<Map>(batchCapacity);
        updateSnapshots = new ArrayList<Map>(batchCapacity);
        objectIds = new ArrayList<ObjectId>(batchCapacity);

        dbAttributes = new ArrayList<DbAttribute>(updatedAttributes.size()
                + qualifierAttributes.size());
        dbAttributes.addAll(updatedAttributes);
        dbAttributes.addAll(qualifierAttributes);
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

    @Override
    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = dbAttributes.get(dbAttributeIndex);

        // take value either from updated values or id's,
        // depending on the index
        Object snapshot = (dbAttributeIndex < updatedAttributes.size()) ? updateSnapshots
                .get(batchIndex) : qualifierSnapshots.get(batchIndex);
        return getValue((Map<String, Object>) snapshot, attribute);
    }

    /**
     * Adds a parameter row to the batch.
     */
    public void add(Map qualifierSnapshot, Map<?, ?> updateSnapshot) {
        add(qualifierSnapshot, updateSnapshot, null);
    }

    /**
     * Adds a parameter row to the batch.
     * 
     * @since 1.2
     */
    public void add(Map qualifierSnapshot, Map<?, ?> updateSnapshot, ObjectId id) {
        qualifierSnapshots.add(qualifierSnapshot);
        updateSnapshots.add(updateSnapshot);
        objectIds.add(id);
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
     * @since 1.1
     */
    public List <DbAttribute> getUpdatedAttributes() {
        return Collections.unmodifiableList(updatedAttributes);
    }

    /**
     * @since 1.1
     */
    public List<DbAttribute> getQualifierAttributes() {
        return Collections.unmodifiableList(qualifierAttributes);
    }

    /**
     * Returns a snapshot of the current qualifier values.
     * 
     * @since 1.1
     */
    public Map getCurrentQualifier() {
        return (Map) qualifierSnapshots.get(batchIndex);
    }

    /**
     * Returns an ObjectId associated with the current batch iteration. Used internally by
     * Cayenne to match current iteration with a specific object and assign it generated
     * keys.
     * 
     * @since 1.2
     */
    @Override
    public ObjectId getObjectId() {
        return objectIds.get(batchIndex);
    }
}
