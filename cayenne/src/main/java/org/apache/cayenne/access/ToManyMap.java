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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.Accessor;

/**
 * A map that holds objects for to-many relationships keyed by a property of the target, lazily resolved on first
 * access. Unlike {@link ToManyList} and {@link ToManySet}, writes to an unresolved map resolve it first.
 *
 * @since 3.0
 */
class ToManyMap<K, V> extends ToManyHolder<V> implements Map<K, V> {

    protected Map<K, V> objectMap;
    protected Accessor mapKeyAccessor;

    ToManyMap(Persistent relationshipOwner, String relationshipName, Accessor mapKeyAccessor) {
        super(relationshipOwner, relationshipName);
        this.mapKeyAccessor = mapKeyAccessor;
    }

    // ====================================================
    // RelationshipCollection methods
    // ====================================================

    @Override
    public boolean isFault() {
        if (objectMap != null) {
            return false;
        }

        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        if (isTransientParent()) {
            objectMap = new HashMap<>();
            return false;
        }

        return true;
    }

    @Override
    public void invalidate() {
        objectMap = null;
    }

    /**
     * Resolves the map with the given objects (e.g. a prefetch result), indexing them by map key.
     */
    @Override
    public void resolveWith(Collection<V> objects) {
        objectMap = indexCollection(objects);
    }

    /**
     * Returns internal objects map resolving it if needed.
     */
    protected Map<K, V> resolvedObjectMap() {
        if (isFault()) {
            synchronized (this) {
                // now that we obtained the lock, check if another thread just resolved the map
                if (isFault()) {
                    objectMap = indexCollection(resolveRelationship());
                }
            }
        }

        return objectMap;
    }

    /**
     * Converts a collection into a map indexed by map key.
     */
    @SuppressWarnings("unchecked")
    protected Map<K, V> indexCollection(Collection<V> collection) {
        Map<K, V> map = new HashMap<>((int) (collection.size() * 1.33d) + 1);

        for (V next : collection) {
            K key = (K) mapKeyAccessor.getValue(next);
            V previous = map.put(key, next);
            if (previous != null && previous != next) {
                throw new CayenneRuntimeException(
                        "Duplicate key '%s' in relationship map. Relationship: %s, source object: %s",
                        key, relationshipName, relationshipOwner.getObjectId());
            }
        }

        return map;
    }

    // ====================================================
    // Standard Map Methods.
    // ====================================================

    @Override
    public void clear() {
        resolvedObjectMap().clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return resolvedObjectMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return resolvedObjectMap().containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return resolvedObjectMap().entrySet();
    }

    @Override
    public V get(Object key) {
        return resolvedObjectMap().get(key);
    }

    @Override
    public boolean isEmpty() {
        return resolvedObjectMap().isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return resolvedObjectMap().keySet();
    }

    @Override
    public V put(K key, V value) {
        return resolvedObjectMap().put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        resolvedObjectMap().putAll(map);
    }

    @Override
    public V remove(Object key) {
        return resolvedObjectMap().remove(key);
    }

    @Override
    public int size() {
        return resolvedObjectMap().size();
    }

    @Override
    public Collection<V> values() {
        return resolvedObjectMap().values();
    }

    @Override
    public String toString() {
        return objectMap != null ? objectMap.toString() : "{<unresolved>}";
    }
}
