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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.util.EqualsBuilder;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.IDUtil;
import org.apache.cayenne.util.Util;

/**
 * A portable global identifier for persistent objects. ObjectId can be temporary (used
 * for transient or new uncommitted objects) or permanent (used for objects that have been
 * already stored in DB). A temporary ObjectId stores object entity name and a
 * pseudo-unique binary key; permanent id stores a map of values from an external
 * persistent store (aka "primary key").
 * 
 */
public class ObjectId implements Serializable {

    protected String entityName;
    protected Map<String, Object> objectIdKeys;

    private String singleKey;
    private Object singleValue;

    // key which is used for temporary ObjectIds only
    protected byte[] key;

    protected Map<String, Object> replacementIdMap;

    // hash code is transient to make sure id is portable across VM
    transient int hashCode;

    // exists for deserialization with Hessian and similar
    @SuppressWarnings("unused")
    private ObjectId() {
    }

    /**
     * Creates a TEMPORARY ObjectId. Assigns a generated unique key.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName) {
        this(entityName, IDUtil.pseudoUniqueByteSequence8());
    }

    /**
     * Creates a TEMPORARY id with a specified entity name and a binary key. It is a
     * caller responsibility to provide a globally unique binary key.
     * 
     * @since 1.2
     */
    public ObjectId(String entityName, byte[] key) {
        this.entityName = entityName;
        this.key = key;
    }

    /**
     * Creates a portable permanent ObjectId.
     * 
     * @param entityName The entity name which this object id is for
     * @param key A key describing this object id, usually the attribute name for the
     *            primary key
     * @param value The unique value for this object id
     * @since 1.2
     */
    public ObjectId(String entityName, String key, int value) {
        this(entityName, key, Integer.valueOf(value));
    }

    /**
     * Creates a portable permanent ObjectId.
     * 
     * @param entityName The entity name which this object id is for
     * @param key A key describing this object id, usually the attribute name for the
     *            primary key
     * @param value The unique value for this object id
     * @since 1.2
     */
    public ObjectId(String entityName, String key, Object value) {
        this.entityName = entityName;

        this.singleKey = key;
        this.singleValue = value;
    }

    /**
     * Creates a portable permanent ObjectId as a compound primary key.
     * 
     * @param entityName The entity name which this object id is for
     * @param idMap Keys are usually the attribute names for each part of the primary key.
     *            Values are unique when taken as a whole.
     * @since 1.2
     */
    public ObjectId(String entityName, Map<String, ?> idMap) {
        this.entityName = entityName;

        if (idMap == null || idMap.size() == 0) {

        }
        else if (idMap.size() == 1) {
            Map.Entry<String, ?> e = idMap.entrySet().iterator().next();
            this.singleKey = String.valueOf(e.getKey());
            this.singleValue = e.getValue();
        }
        else {

            // we have to create a copy of the map, otherwise we may run into
            // serialization
            // problems with hessian
            this.objectIdKeys = new HashMap<String, Object>(idMap);
        }
    }

    /**
     * Is this is temporary object id (used for objects which are not yet persisted to the
     * data store).
     */
    public boolean isTemporary() {
        return key != null;
    }

    /**
     * @since 1.2
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Get the binary temporary object id. Null if this object id is permanent (persisted
     * to the data store).
     */
    public byte[] getKey() {
        return key;
    }

    /**
     * Returns an unmodifiable Map of persistent id values, essentially a primary key map.
     * For temporary id returns replacement id, if it was already created. Otherwise
     * returns an empty map.
     */
    public Map<String, Object> getIdSnapshot() {
        if (isTemporary()) {
            return (replacementIdMap == null) ? Collections.EMPTY_MAP : Collections
                    .unmodifiableMap(replacementIdMap);
        }

        if (singleKey != null) {
            return Collections.singletonMap(singleKey, singleValue);
        }

        return objectIdKeys != null
                ? Collections.unmodifiableMap(objectIdKeys)
                : Collections.EMPTY_MAP;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ObjectId)) {
            return false;
        }

        ObjectId id = (ObjectId) object;

        if (!Util.nullSafeEquals(entityName, id.entityName)) {
            return false;
        }

        if (isTemporary()) {
            return new EqualsBuilder().append(key, id.key).isEquals();
        }

        if (singleKey != null) {
            return Util.nullSafeEquals(singleKey, id.singleKey)
                    && valueEquals(singleValue, id.singleValue);
        }

        if (id.objectIdKeys == null) {
            return objectIdKeys == null;
        }

        if (id.objectIdKeys.size() != objectIdKeys.size()) {
            return false;
        }

        for (Map.Entry<String, ?> entry : objectIdKeys.entrySet()) {
            Object entryKey = entry.getKey();
            Object entryValue = entry.getValue();

            if (entryValue == null) {
                if (id.objectIdKeys.get(entryKey) != null
                        || !id.objectIdKeys.containsKey(entryKey)) {
                    return false;
                }
            }
            else {
                if (!valueEquals(entryValue, id.objectIdKeys.get(entryKey))) {
                    return false;
                }
            }
        }

        return true;
    }

    private final boolean valueEquals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }

        if (o1 == null) {
            return false;
        }

        if (o1 instanceof Number) {
            return o2 instanceof Number
                    && ((Number) o1).longValue() == ((Number) o2).longValue();
        }

        if (o1.getClass().isArray()) {
            return new EqualsBuilder().append(o1, o2).isEquals();
        }

        return Util.nullSafeEquals(o1, o2);
    }

    @Override
    public int hashCode() {

        if (this.hashCode == 0) {

            HashCodeBuilder builder = new HashCodeBuilder(3, 5);
            builder.append(entityName.hashCode());

            if (key != null) {
                builder.append(key);
            }
            else if (singleKey != null) {
                builder.append(singleKey.hashCode());

                // must reconcile all possible numeric types
                if (singleValue instanceof Number) {
                    builder.append(((Number) singleValue).longValue());
                }
                else {
                    builder.append(singleValue);
                }
            }
            else if (objectIdKeys != null) {
                int len = objectIdKeys.size();

                // handle multiple keys - must sort the keys to use with HashCodeBuilder

                Object[] keys = objectIdKeys.keySet().toArray();
                Arrays.sort(keys);

                for (int i = 0; i < len; i++) {
                    // HashCodeBuilder will take care of processing object if it
                    // happens to be a primitive array such as byte[]

                    // also we don't have to append the key hashcode, its index will
                    // work
                    builder.append(i);

                    Object value = objectIdKeys.get(keys[i]);
                    // must reconcile all possible numeric types
                    if (value instanceof Number) {
                        builder.append(((Number) value).longValue());
                    }
                    else {
                        builder.append(value);
                    }
                }
            }

            this.hashCode = builder.toHashCode();
            assert hashCode != 0 : "Generated zero hashCode";
        }

        return hashCode;
    }

    /**
     * Returns a non-null mutable map that can be used to append replacement id values.
     * This allows to incrementally build a replacement GlobalID.
     * 
     * @since 1.2
     */
    public Map<String, Object> getReplacementIdMap() {
        if (replacementIdMap == null) {
            replacementIdMap = new HashMap<String, Object>();
        }

        return replacementIdMap;
    }

    /**
     * Creates and returns a replacement ObjectId. No validation of ID is done.
     * 
     * @since 1.2
     */
    public ObjectId createReplacementId() {
        // merge existing and replaced ids to handle a replaced subset of
        // a compound primary key
        Map<String, Object> newIdMap = new HashMap<String, Object>(getIdSnapshot());
        if (replacementIdMap != null) {
            newIdMap.putAll(replacementIdMap);
        }
        return new ObjectId(getEntityName(), newIdMap);
    }

    /**
     * Returns true if there is full or partial replacement id attached to this id. This
     * method is preferrable to "!getReplacementIdMap().isEmpty()" as it avoids unneeded
     * replacement id map creation.
     */
    public boolean isReplacementIdAttached() {
        return replacementIdMap != null && !replacementIdMap.isEmpty();
    }

    /**
     * A standard toString method used for debugging. It is guaranteed to produce the same
     * string if two ObjectIds are equal.
     */
    @Override
    public String toString() {

        StringBuilder buffer = new StringBuilder();

        buffer.append("<ObjectId:").append(entityName);

        if (isTemporary()) {
            buffer.append(", TEMP:");
            for (byte aKey : key) {
                IDUtil.appendFormattedByte(buffer, aKey);
            }
        }
        else if (singleKey != null) {
            buffer.append(", ").append(String.valueOf(singleKey)).append("=").append(
                    singleValue);
        }
        else if (objectIdKeys != null) {

            // ensure consistent order of the keys, so that toString could be used as a
            // unique key, just like id itself

            List<String> keys = new ArrayList<String>(objectIdKeys.keySet());
            Collections.sort(keys);
            for (Object key : keys) {
                buffer.append(", ");
                buffer.append(String.valueOf(key)).append("=").append(
                        objectIdKeys.get(key));
            }
        }

        buffer.append(">");
        return buffer.toString();
    }
}
