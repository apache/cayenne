/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.util.Util;

/**
 * DataRow a map that holds values retrieved from the database for a given query row.
 * DataRows are used to cache raw database data and as a reference point for tracking
 * DataObject changes.
 * 
 * @author Andrus Adamchik
 * @since 1.1
 */
public class DataRow extends HashMap {

    // "volatile" is supposed to ensure consistency in read and increment operations;
    // is this universally true?

    // make sure the starting value is different from DataObject default version value
    private static volatile long currentVersion = DataObject.DEFAULT_VERSION + 1;

    protected long version = currentVersion++;
    protected long replacesVersion = DataObject.DEFAULT_VERSION;

    public DataRow(Map map) {
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

        Iterator it = diff.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            merged.put(entry.getKey(), entry.getValue());
        }

        return merged;
    }

    /**
     * Creates a DataRow that contains only the keys that have values that differ between
     * this object and <code>row</code> parameter. Diff values are taken from the
     * <code>row</code> parameter. It is assumed that key sets are compatible in both
     * rows (e.g. they represent snapshots for the same entity). Returns null if no
     * differences are found.
     */
    public DataRow createDiff(DataRow row) {

        // build a diff...
        DataRow diff = null;

        Iterator entries = entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            Object key = entry.getKey();
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
     * Creates an ObjectId from the values in the snapshot. If needed attributes are
     * missing in a snapshot, CayenneRuntimeException is thrown.
     */
    // TODO: andrus, 5/25/2006 - deprecate this method - it is unused
    public ObjectId createObjectId(ObjEntity entity) {
        return createObjectId(entity.getName(), entity.getDbEntity());
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. Now this method relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId createObjectId(Class objectClass, DbEntity entity) {
        return createObjectId(ObjectId.entityNameFromClass(objectClass), entity);
    }

    /**
     * @deprecated since 1.2, as new portable ObjectIds can't store Java Class and store
     *             entity name instead. Now this method relies on default CayenneModeler
     *             naming convention to figure out entity name from class name. This may
     *             not work if the classes where mapped differently.
     */
    public ObjectId createTargetObjectId(Class targetClass, DbRelationship relationship) {
        return createTargetObjectId(
                ObjectId.entityNameFromClass(targetClass),
                relationship);
    }

    // TODO: andrus, 5/25/2006 - deprecate this method - it is unused
    public ObjectId createObjectId(String entityName, DbEntity entity) {
        return createObjectId(entityName, entity, null);
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

        Map target = relationship.targetPkSnapshotWithSrcSnapshot(this);
        return (target != null) ? new ObjectId(entityName, target) : null;
    }

    /**
     * Extracts PK columns prefixed with some path. If namePrefix is null or empty, no
     * prefixing is done.
     * <p>
     * Prefixing is useful when extracting an ObjectId of a target row from a row obtained
     * via prefetching. namePrefix must omit the "db:" prefix and must end with ".", e.g.
     * "TO_ARTIST.PAINTING_ARRAY."
     * </p>
     * 
     * @since 1.2
     */
    // TODO: andrus, 5/25/2006 - deprecate this method - it is unused
    public ObjectId createObjectId(String entityName, DbEntity entity, String namePrefix) {

        boolean prefix = namePrefix != null && namePrefix.length() > 0;

        // ... handle special case - PK.size == 1
        // use some not-so-significant optimizations...

        List pk = entity.getPrimaryKey();
        if (pk.size() == 1) {
            DbAttribute attribute = (DbAttribute) pk.get(0);

            String key = (prefix) ? namePrefix + attribute.getName() : attribute
                    .getName();

            Object val = this.get(key);
            if (val == null) {
                throw new CayenneRuntimeException("Null value for '"
                        + key
                        + "'. Snapshot: "
                        + this
                        + ". Prefix: "
                        + namePrefix);
            }

            // PUT without a prefix
            return new ObjectId(entityName, attribute.getName(), val);
        }

        // ... handle generic case - PK.size > 1

        Map idMap = new HashMap(pk.size() * 2);
        Iterator it = pk.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();

            String key = (prefix) ? namePrefix + attribute.getName() : attribute
                    .getName();

            Object val = this.get(key);
            if (val == null) {
                throw new CayenneRuntimeException("Null value for '"
                        + key
                        + "'. Snapshot: "
                        + this
                        + ". Prefix: "
                        + namePrefix);
            }

            // PUT without a prefix
            idMap.put(attribute.getName(), val);
        }

        return new ObjectId(entityName, idMap);
    }

    public String toString() {
        return new ToStringBuilder(this).append("values", super.toString()).append(
                " version",
                version).append(" replaces", replacesVersion).toString();
    }
}
