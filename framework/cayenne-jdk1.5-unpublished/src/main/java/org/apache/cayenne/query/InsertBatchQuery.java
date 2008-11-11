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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * Batched INSERT query. Allows inserting multiple object snapshots (DataRows) for a given
 * DbEntity in a single query. InsertBatchQuery normally is not used directly. Rather
 * DataContext creates one internally when committing DataObjects.
 * 
 */
public class InsertBatchQuery extends BatchQuery {

    /**
     * @since 1.2
     */
    protected List<ObjectId> objectIds;

    protected List<Map> objectSnapshots;
    protected List<DbAttribute> dbAttributes;

    /**
     * Creates new InsertBatchQuery for a given DbEntity and estimated capacity.
     */
    public InsertBatchQuery(DbEntity entity, int batchCapacity) {
        super(entity);

        this.objectSnapshots = new ArrayList<Map>(batchCapacity);
        this.objectIds = new ArrayList<ObjectId>(batchCapacity);
        this.dbAttributes = new ArrayList<DbAttribute>(getDbEntity().getAttributes());
    }

    @Override
    public Object getValue(int dbAttributeIndex) {
        DbAttribute attribute = dbAttributes.get(dbAttributeIndex);
        Map currentSnapshot = objectSnapshots.get(batchIndex);
        return getValue(currentSnapshot, attribute);
    }

    /**
     * Adds a snapshot to batch. A shortcut for "add(snapshot, null)".
     */
    public void add(Map snapshot) {
        add(snapshot, null);
    }

    /**
     * Adds a snapshot to batch. Optionally stores the object id for the snapshot. Note
     * that snapshot can hold either the real values or the instances of
     * org.apache.commons.collections.Factory that will be resolved to the actual value on
     * the spot, thus allowing deferred propagated keys resolution.
     * 
     * @since 1.2
     */
    public void add(Map snapshot, ObjectId id) {
        objectSnapshots.add(snapshot);
        objectIds.add(id);
    }

    @Override
    public int size() {
        return objectSnapshots.size();
    }

    @Override
    public List<DbAttribute> getDbAttributes() {
        return dbAttributes;
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
