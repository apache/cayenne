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

package org.apache.cayenne;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.Util;

/**
 * DataRow a map that holds values retrieved from the database for a given query row.
 * DataRows are used to cache raw database data and as a reference point for tracking
 * DataObject changes.
 * 
 * @since 1.1
 */
public class DataRow extends HashMap<String, Object> {

    // make sure the starting value is different from DataObject default version value
    private static AtomicLong currentVersion = new AtomicLong(
            DataObject.DEFAULT_VERSION + 1);

    protected long version = currentVersion.getAndIncrement();
    protected long replacesVersion = DataObject.DEFAULT_VERSION;

    /**
     * @since 3.0
     */
    protected String entityName;

    public DataRow(Map<String, ?> map) {
        super(map);
    }

    public DataRow(int initialCapacity) {
        super(initialCapacity);
    }

    public long getVersion() {
        return version;
    }

    public long getReplacesVersion() {
        return replacesVersion;
    }

    /**
     * Sets the version of DataRow replaced by this one in the store.
     */
    public void setReplacesVersion(long replacesVersion) {
        this.replacesVersion = replacesVersion;
    }

    /**
     * Builds a new DataRow, merging changes from <code>diff</code> parameter with data
     * contained in this DataRow.
     */
    public DataRow applyDiff(DataRow diff) {
        DataRow merged = new DataRow(this);

        for (Map.Entry<String, Object> entry : diff.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }

        return merged;
    }

    /**
     * Creates a DataRow that contains only the keys that have values that differ between
     * this object and <code>row</code> parameter. Diff values are taken from the
     * <code>row</code> parameter. It is assumed that key sets are compatible in both rows
     * (e.g. they represent snapshots for the same entity). Returns null if no differences
     * are found.
     */
    public DataRow createDiff(DataRow row) {

        // build a diff...
        DataRow diff = null;

        for (Map.Entry<String, Object> entry : entrySet()) {

            String key = entry.getKey();
            Object currentValue = entry.getValue();
            Object rowValue = row.get(key);

            if (!Util.nullSafeEquals(currentValue, rowValue)) {
                if (diff == null) {
                    diff = new DataRow(this.size());
                }
                diff.put(key, rowValue);
            }
        }

        return diff;
    }

    /**
     * Returns an ObjectId of an object on the other side of the to-one relationship, for
     * this DataRow representing a source of relationship. Returns null if snapshot FK
     * columns indicate a null to-one relationship.
     */
    public ObjectId createTargetObjectId(String entityName, DbRelationship relationship) {

        if (relationship.isToMany()) {
            throw new CayenneRuntimeException("Only 'to one' can have a target ObjectId.");
        }

        Map<String, Object> target = relationship.targetPkSnapshotWithSrcSnapshot(this);
        return (target != null) ? new ObjectId(entityName, target) : null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("values", super.toString()).append(
                " version",
                version).append(" replaces", replacesVersion).toString();
    }

    /**
     * @since 3.0
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * @since 3.0
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
}
