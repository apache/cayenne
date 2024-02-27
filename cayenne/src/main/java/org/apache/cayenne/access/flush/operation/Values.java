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
import org.apache.cayenne.exp.path.CayennePath;
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
    protected Map<String, Object> attributeSnapshot;
    protected Map<String, Object> fkSnapshot;

    protected List<DbAttribute> updatedAttributes;
    // generated flattened Ids for this insert
    protected Map<CayennePath, ObjectId> flattenedIds;

    public Values(DbRowOp row, boolean includeId) {
        this.row = row;
        this.includeId = includeId;
    }

    public void addValue(DbAttribute attribute, Object value, boolean fk) {
        if(fk) {
            if (fkSnapshot == null) {
                fkSnapshot = new HashMap<>();
            }
            fkSnapshot.put(attribute.getName(), value);
        } else {
            if (attributeSnapshot == null) {
                attributeSnapshot = new HashMap<>();
            }
            attributeSnapshot.put(attribute.getName(), value);
        }

        if(updatedAttributes == null) {
            updatedAttributes = new ArrayList<>();
        }
        if(!updatedAttributes.contains(attribute)) {
            updatedAttributes.add(attribute);
        }
    }

    public void merge(Values other) {
        if(this.updatedAttributes == null || updatedAttributes.isEmpty()) {
            this.attributeSnapshot = other.attributeSnapshot;
            this.fkSnapshot = other.fkSnapshot;
            this.updatedAttributes = other.updatedAttributes;
        } else {
            if(other.attributeSnapshot != null) {
                if(this.attributeSnapshot == null) {
                    this.attributeSnapshot = new HashMap<>(other.attributeSnapshot.size());
                }
                other.attributeSnapshot.forEach(attributeSnapshot::putIfAbsent);
            }
            if(other.fkSnapshot != null) {
                if(this.fkSnapshot == null) {
                    this.fkSnapshot = new HashMap<>(other.fkSnapshot.size());
                }
                other.fkSnapshot.forEach(fkSnapshot::putIfAbsent);
            }
            if(other.updatedAttributes != null) {
                other.updatedAttributes.forEach(attr -> {
                    if (!updatedAttributes.contains(attr)) {
                        updatedAttributes.add(attr);
                    }
                });
            }
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

    public void addFlattenedId(CayennePath path, ObjectId id) {
        if(flattenedIds == null) {
            flattenedIds = new HashMap<>();
        }
        flattenedIds.put(path, id);
    }

    public Map<String, Object> getSnapshot() {
        if(!includeId) {
            return mergeSnapshots();
        } else {
            Map<String, Object> mergedSnapshot = mergeSnapshots();
            if(mergedSnapshot.isEmpty()) {
                return new HashMap<>(row.getChangeId().getIdSnapshot());
            }
            mergedSnapshot.putAll(row.getChangeId().getIdSnapshot());
            return mergedSnapshot;
        }
    }

    private Map<String, Object> mergeSnapshots() {
        if(attributeSnapshot == null && fkSnapshot == null) {
            return Collections.emptyMap();
        }
        if(attributeSnapshot == null) {
            return fkSnapshot;
        } else if(fkSnapshot == null) {
            return attributeSnapshot;
        }
        // FK should override attribute values
        attributeSnapshot.putAll(fkSnapshot);
        return attributeSnapshot;
    }

    public List<DbAttribute> getUpdatedAttributes() {
        if(updatedAttributes == null) {
            return Collections.emptyList();
        }
        return updatedAttributes;
    }

    public Map<CayennePath, ObjectId> getFlattenedIds() {
        if(flattenedIds == null) {
            return Collections.emptyMap();
        }
        return flattenedIds;
    }

    public boolean isEmpty() {
        if(includeId) {
            return false;
        }
        return (attributeSnapshot == null || attributeSnapshot.isEmpty())
                && (fkSnapshot == null || fkSnapshot.isEmpty());
    }

    public void clear() {
        if(attributeSnapshot != null) {
            attributeSnapshot.clear();
        }
        if(fkSnapshot != null) {
            fkSnapshot.clear();
        }
        if(updatedAttributes != null) {
            updatedAttributes.clear();
        }
        if(flattenedIds != null) {
            flattenedIds.clear();
        }
    }

    public boolean isSameBatch(Values other) {
        if(updatedAttributes == null) {
            return other.updatedAttributes == null;
        }
        if(other.updatedAttributes == null) {
            return false;
        }
        return updatedAttributes.equals(other.updatedAttributes);
    }
}
