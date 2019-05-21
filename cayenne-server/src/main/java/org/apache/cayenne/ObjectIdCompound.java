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

import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.Util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Compound {@link ObjectId}
 * @since 4.2
 */
class ObjectIdCompound implements ObjectId {

	private static final long serialVersionUID = -2265029098344119323L;
	
	protected final String entityName;
	protected final Map<String, Object> objectIdKeys;

	protected Map<String, Object> replacementIdMap;

	// hash code is transient to make sure id is portable across VM
	private transient int hashCode;

	// exists for deserialization with Hessian and similar
	@SuppressWarnings("unused")
	private ObjectIdCompound() {
		entityName = null;
		objectIdKeys = Collections.emptyMap();
	}

	/**
	 * Creates a portable permanent ObjectId as a compound primary key.
	 * 
	 * @param entityName
	 *            The entity name which this object id is for
	 * @param idMap
	 *            Keys are usually the attribute names for each part of the
	 *            primary key. Values are unique when taken as a whole.
	 * @since 1.2
	 */
	ObjectIdCompound(String entityName, Map<String, ?> idMap) {
		this.entityName = entityName;

		if (idMap == null || idMap.size() == 0) {
			this.objectIdKeys = Collections.emptyMap();
			return;
		}
		this.objectIdKeys = wrapIdMap(idMap);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> wrapIdMap(Map<String, ?> m) {
		if(m.getClass() == HashMap.class) {
			return (Map<String, Object>)m;
		} else {
			// we have to create a copy of the map, otherwise we may run into serialization problems with hessian
			return new HashMap<>(m);
		}
	}

	/**
	 * Is this is temporary object id (used for objects which are not yet
	 * persisted to the data store).
	 */
	@Override
	public boolean isTemporary() {
		return false;
	}

	/**
	 * @since 1.2
	 */
	@Override
	public String getEntityName() {
		return entityName;
	}

	/**
	 * Get the binary temporary object id. Null if this object id is permanent
	 * (persisted to the data store).
	 */
	@Override
	public byte[] getKey() {
		return null;
	}

	/**
	 * Returns an unmodifiable Map of persistent id values, essentially a
	 * primary key map. For temporary id returns replacement id, if it was
	 * already created. Otherwise returns an empty map.
	 */
	@Override
	public Map<String, Object> getIdSnapshot() {
		return Collections.unmodifiableMap(objectIdKeys);
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof ObjectIdCompound)) {
			return false;
		}

		ObjectIdCompound id = (ObjectIdCompound) object;
		if (!Util.nullSafeEquals(entityName, id.entityName)) {
			return false;
		}

		if (id.objectIdKeys.size() != objectIdKeys.size()) {
			return false;
		}

		for (Map.Entry<String, ?> entry : objectIdKeys.entrySet()) {
			String entryKey = entry.getKey();
			Object entryValue = entry.getValue();

			if (entryValue == null
					&& (id.objectIdKeys.get(entryKey) != null || !id.objectIdKeys.containsKey(entryKey))) {
				return false;
			} else if (!valueEquals(entryValue, id.objectIdKeys.get(entryKey))) {
				return false;
			}
		}

		return true;
	}

	private boolean valueEquals(Object o1, Object o2) {
		if (o1 == o2) {
			return true;
		}

		if (o2 == null) {
			return false;
		}

		if (o1 instanceof Number) {
			return o2 instanceof Number && ((Number) o1).longValue() == ((Number) o2).longValue();
		}

		return Util.nullSafeEquals(o1, o2);
	}

	@Override
	public int hashCode() {
		if(hashCode != 0) {
			return hashCode;
		}

		HashCodeBuilder builder = new HashCodeBuilder().append(entityName.hashCode());

		// handle multiple keys - must sort the keys to use with HashCodeBuilder
		String[] keys = objectIdKeys.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (int i = 0; i < keys.length; i++) {
			// HashCodeBuilder will take care of processing object if it
			// happens to be a primitive array such as byte[]

			// also we don't have to append the key hashcode, its index will work
			builder.append(i);

			Object value = objectIdKeys.get(keys[i]);
			// must reconcile all possible numeric types
			if (value instanceof Number) {
				builder.append(((Number) value).longValue());
			} else {
				builder.append(value);
			}
		}
		return hashCode = builder.toHashCode();
	}

	/**
	 * Returns a non-null mutable map that can be used to append replacement id
	 * values. This allows to incrementally build a replacement GlobalID.
	 * 
	 * @since 1.2
	 */
	@Override
	public Map<String, Object> getReplacementIdMap() {
		if (replacementIdMap == null) {
			replacementIdMap = new HashMap<>();
		}

		return replacementIdMap;
	}

	/**
	 * Creates and returns a replacement ObjectId. No validation of ID is done.
	 * 
	 * @since 1.2
	 */
	@Override
	public ObjectId createReplacementId() {
		if(replacementIdMap == null) {
			return this;
		}
		// merge existing and replaced ids to handle a replaced subset of a compound primary key
		Map<String, Object> newIdMap = new HashMap<>(objectIdKeys);
		newIdMap.putAll(replacementIdMap);
		return ObjectId.of(entityName, newIdMap);
	}

	/**
	 * Returns true if there is full or partial replacement id attached to this
	 * id. This method is preferable to "!getReplacementIdMap().isEmpty()" as
	 * it avoids unneeded replacement id map creation.
	 */
	@Override
	public boolean isReplacementIdAttached() {
		return replacementIdMap != null && !replacementIdMap.isEmpty();
	}

	/**
	 * A standard toString method used for debugging. It is guaranteed to
	 * produce the same string if two ObjectIds are equal.
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder().append("<ObjectId:").append(entityName);
		// ensure consistent order of the keys, so that toString could be
		// used as a unique key, just like id itself
		String[] keys = objectIdKeys.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (String key : keys) {
			buffer.append(", ").append(key).append("=").append(objectIdKeys.get(key));
		}
		return buffer.append(">").toString();
	}
}
