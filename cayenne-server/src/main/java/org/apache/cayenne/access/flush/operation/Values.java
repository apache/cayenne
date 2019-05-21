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

package org.apache.cayenne.access.flush.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;

/**
 * Collection of values that should be inserted or updated in DB.
 *
 * @since 4.2
 */
public class Values {

    protected final DbRowOp row;
    protected final boolean includeId;
    // new values to store to DB
    protected Map<String, Object> snapshot;
    protected List<DbAttribute> updatedAttributes;
    // generated flattened Ids for this insert
    protected Map<String, ObjectId> flattenedIds;

    public Values(DbRowOp row, boolean includeId) {
        this.row = row;
        this.includeId = includeId;
    }

    public void addValue(DbAttribute attribute, Object value) {
        if(snapshot == null) {
            snapshot = new HashMap<>();
            updatedAttributes = new ArrayList<>();
        }
        snapshot.put(attribute.getName(), value);
        if(!updatedAttributes.contains(attribute)) {
            updatedAttributes.add(attribute);
        }
    }

    public void merge(Values other) {
        if(this.snapshot == null) {
            this.snapshot = other.snapshot;
            this.updatedAttributes = other.updatedAttributes;
        } else if(other.snapshot != null) {
            other.snapshot.forEach(snapshot::putIfAbsent);
            other.updatedAttributes.forEach(attr -> {
                if(!updatedAttributes.contains(attr)) {
                    updatedAttributes.add(attr);
                }
            });
        }

        if(other.flattenedIds != null) {
            if(flattenedIds == null) {
                flattenedIds = other.getFlattenedIds();
            } else {
                other.flattenedIds.forEach((path, id) -> flattenedIds.compute(path, (p, existing) -> {
                     if(id.getEntityName().equals(row.getChangeId().getEntityName())
                        || (existing != null && existing.getEntityName().equals(row.getChangeId().getEntityName()))) {
                         return row.getChangeId();
                     }
                     if(existing != null) {
                         return existing;
                     }
                     return id;
                }));
            }
        }
    }

    public void addFlattenedId(String path, ObjectId id) {
        if(flattenedIds == null) {
            flattenedIds = new HashMap<>();
        }
        flattenedIds.put(path, id);
    }

    public Map<String, Object> getSnapshot() {
        if(!includeId) {
            if(snapshot == null) {
                return Collections.emptyMap();
            }
            return snapshot;
        } else {
            if (snapshot == null) {
                snapshot = new HashMap<>();
                snapshot.putAll(row.getChangeId().getIdSnapshot());
                return snapshot;
            }
            snapshot.putAll(row.getChangeId().getIdSnapshot());
            return snapshot;
        }
    }

    public List<DbAttribute> getUpdatedAttributes() {
        if(updatedAttributes == null) {
            return Collections.emptyList();
        }
        return updatedAttributes;
    }

    public Map<String, ObjectId> getFlattenedIds() {
        if(flattenedIds == null) {
            return Collections.emptyMap();
        }
        return flattenedIds;
    }

    public boolean isEmpty() {
        if(includeId) {
            return false;
        }
        return snapshot == null || snapshot.isEmpty();
    }

    public void clear() {
        if(snapshot != null) {
            snapshot.clear();
        }
        if(updatedAttributes != null) {
            updatedAttributes.clear();
        }
        if(flattenedIds != null) {
            flattenedIds.clear();
        }
    }

    public boolean isSameBatch(Values other) {
        if(snapshot == null) {
            return other.snapshot == null;
        }
        if(other.snapshot == null) {
            return false;
        }
        return snapshot.keySet().equals(other.snapshot.keySet());
    }
}
