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

package org.apache.cayenne;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DataRow a map that holds values retrieved from the database for a given query row.
 * DataRows are used to cache raw database data and as a reference point for tracking
 * PersistentObject changes.
 * <p>
 * Every DataRow carries a snapshot version. Versions come from one JVM-wide counter, so a version taken later is
 * greater than any version taken before, and a version identifies the unit of work that produced the row: all rows
 * read by one SQL statement share a version, as do all snapshots of one commit. Two rows of the same ObjectId with
 * the same version therefore hold the same data. A row created without an explicit version gets a version of its
 * own.
 *
 * @since 1.1
 */
public class DataRow extends HashMap<String, Object> {

    // make sure the starting value is different from Persistent default version value
    private static final AtomicLong currentVersion = new AtomicLong(Persistent.DEFAULT_VERSION + 1);

    protected final long version;
    protected long replacesVersion = Persistent.DEFAULT_VERSION;

    /**
     * @since 3.0
     */
    protected String entityName;

    /**
     * Returns a new snapshot version, greater than any version returned before. Rows produced by one unit of work
     * (a SQL statement result, a commit) are created with the one version taken for that unit of work.
     *
     * @since 5.0
     */
    public static long nextVersion() {
        return currentVersion.getAndIncrement();
    }

    /**
     * Creates a row with the given values and a version of its own.
     */
    public DataRow(Map<String, ?> map) {
        this(map, nextVersion());
    }

    /**
     * Creates an empty row with a version of its own.
     */
    public DataRow(int initialCapacity) {
        this(initialCapacity, nextVersion());
    }

    /**
     * Creates a row with the given values and the given snapshot version. The entity name is not copied.
     *
     * @since 5.0
     */
    public DataRow(Map<String, ?> map, long version) {
        super(map);
        this.version = version;
    }

    /**
     * Creates an empty row with the given snapshot version.
     *
     * @since 5.0
     */
    public DataRow(int initialCapacity, long version) {
        super(initialCapacity);
        this.version = version;
    }

    /**
     * Returns the snapshot version of this row: the version of the unit of work (a SQL statement result, a commit)
     * that produced it.
     */
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
        merged.putAll(diff);
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

            if (!Objects.deepEquals(currentValue, rowValue)) {
                if (diff == null) {
                    // the diff represents the newer row, so it carries that row's version
                    diff = new DataRow(this.size(), row.getVersion());
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
        return (target != null) ? ObjectId.of(entityName, target) : null;
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
