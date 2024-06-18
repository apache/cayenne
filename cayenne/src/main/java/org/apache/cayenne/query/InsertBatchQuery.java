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

package org.apache.cayenne.query;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbEntity;

import java.util.ArrayList;
import java.util.Map;

/**
 * Batched INSERT query. Allows inserting multiple object snapshots (DataRows)
 * for a given DbEntity in a single query. InsertBatchQuery normally is not used
 * directly. Rather DataContext creates one internally when committing
 * Persistent objects.
 */
public class InsertBatchQuery extends BatchQuery {

    /**
     * Creates new InsertBatchQuery for a given DbEntity and estimated capacity.
     */
    public InsertBatchQuery(DbEntity entity, int batchCapacity) {
        super(entity, new ArrayList<>(entity.getAttributes()), batchCapacity);
    }

    /**
     * Adds a snapshot to batch. A shortcut for "add(snapshot, null)".
     */
    public void add(Map<String, Object> snapshot) {
        add(snapshot, null);
    }

    /**
     * Adds a snapshot to batch. Optionally stores the object id for the
     * snapshot. Note that snapshot can hold either the real values or the
     * instances of java.util.Supplier that will be resolved
     * to the actual value on the spot, thus allowing deferred propagated keys
     * resolution.
     * 
     * @since 1.2
     */
    public void add(final Map<String, Object> snapshot, ObjectId id) {
        rows.add(new BatchQueryRow(id, null) {
            @Override
            public Object getValue(int i) {
                return getValue(snapshot, dbAttributes.get(i));
            }
        });
    }

}
