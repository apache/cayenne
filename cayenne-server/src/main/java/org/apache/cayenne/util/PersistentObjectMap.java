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
package org.apache.cayenne.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.reflect.Accessor;

/**
 * @since 3.0
 */
public class PersistentObjectMap extends RelationshipFault implements Map, ValueHolder {

    protected Map objectMap;
    protected Accessor mapKeyAccessor;

    // exists for the benefit of manual serialization schemes such as the one in Hessian.
    private PersistentObjectMap() {
    }

    /**
     * Creates PersistentObjectList initializing it with list owner persistent object and
     * relationship name that this list maps to.
     * 
     * @param relationshipOwner persistent object that owns this list.
     * @param relationshipName a query used to resolve the list
     * @param mapKeyAccessor an accessor that can read a map key from an object.
     */
    public PersistentObjectMap(Persistent relationshipOwner, String relationshipName,
            Accessor mapKeyAccessor) {
        super(relationshipOwner, relationshipName);
        this.mapKeyAccessor = mapKeyAccessor;
    }

    public Object getValue() throws CayenneRuntimeException {
        return resolvedObjectMap();
    }

    public Object getValueDirectly() throws CayenneRuntimeException {
        return objectMap;
    }

    public void invalidate() {
        setObjectMap(null);
    }
    
    @Override
    protected void mergeLocalChanges(List resolved) {
        // TODO implement
    }

    public boolean isFault() {

        if (objectMap != null) {
            return false;
        }

        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        // synchronize??
        else if (isTransientParent()) {
            objectMap = new HashMap();
            return false;
        }
        else {
            return true;
        }
    }

    public Object setValue(Object value) throws CayenneRuntimeException {
        resolvedObjectMap();
        return setValueDirectly(objectMap);
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {
        Object old = this.objectMap;

        if (value == null || value instanceof Map) {
            setObjectMap((Map) value);
        }
        // we can index collections on the fly - this is needed for prefetch handling...
        // although it seems to be breaking the contract for 'setValueDirectly' ???
        else if (value instanceof Collection) {
            setObjectMap(indexCollection((Collection) value));
        }
        else {
            throw new CayenneRuntimeException(
                    "Value must be a Map, a Collection or null, got: "
                            + value.getClass().getName());
        }

        return old;
    }

    public void setObjectMap(Map objectMap) {
        this.objectMap = objectMap;
    }

    // ====================================================
    // Tracking map modifications, and resolving it
    // on demand
    // ====================================================

    /**
     * Returns internal objects list resolving it if needed.
     */
    protected Map resolvedObjectMap() {
        if (isFault()) {

            synchronized (this) {

                // now that we obtained the lock, check
                // if another thread just resolved the map
                if (isFault()) {
                    List localList = resolveFromDB();

                    // map objects by property
                    Map localMap = indexCollection(localList);

                    // TODO: andrus 8/20/2007 implement merging local changes like
                    // PersistentObjectList does
                    // mergeLocalChanges(localList);

                    this.objectMap = localMap;
                }
            }
        }

        return objectMap;
    }

    /**
     * Converts a collection into a map indexed by map key.
     */
    protected Map indexCollection(Collection collection) {
        // map objects by property
        Map map = new HashMap((int) (collection.size() * 1.33d) + 1);

        if (collection.size() > 0) {

            Iterator it = collection.iterator();
            while (it.hasNext()) {
                Object next = it.next();
                Object key = mapKeyAccessor.getValue(next);
                Object previous = map.put(key, next);
                if (previous != null && previous != next) {
                    throw new CayenneRuntimeException("Duplicate key '"
                            + key
                            + "' in relationship map. Relationship: "
                            + relationshipName
                            + ", source object: "
                            + relationshipOwner.getObjectId());
                }
            }
        }

        return map;
    }

    @Override
    public String toString() {
        return (objectMap != null) ? objectMap.toString() : "{<unresolved>}";
    }

    protected void postprocessAdd(Object addedObject) {

        // notify ObjectContext
        if (relationshipOwner.getObjectContext() != null) {
            relationshipOwner.getObjectContext().propertyChanged(
                    relationshipOwner,
                    relationshipName,
                    null,
                    addedObject);
            
            if (addedObject instanceof Persistent) {
                Util.setReverse(relationshipOwner, relationshipName,
                        (Persistent) addedObject);
            }
        }
    }

    protected void postprocessAdd(Collection collection) {
        Iterator it = collection.iterator();
        while (it.hasNext()) {
            postprocessAdd(it.next());
        }
    }

    protected void postprocessRemove(Object removedObject) {

        // notify ObjectContext
        if (relationshipOwner.getObjectContext() != null) {
            relationshipOwner.getObjectContext().propertyChanged(
                    relationshipOwner,
                    relationshipName,
                    removedObject,
                    null);
            if (removedObject instanceof Persistent) {
                Util.unsetReverse(relationshipOwner, relationshipName,
                        (Persistent) removedObject);
            }
        }
    }

    protected void postprocessRemove(Collection collection) {
        Iterator it = collection.iterator();
        while (it.hasNext()) {
            postprocessRemove(it.next());
        }
    }

    // ====================================================
    // Standard Map Methods.
    // ====================================================

    public void clear() {
        Map resolved = resolvedObjectMap();
        postprocessRemove(resolved.values());
        resolved.clear();
    }

    public boolean containsKey(Object key) {
        return resolvedObjectMap().containsKey(key);
    }

    public boolean containsValue(Object value) {
        return resolvedObjectMap().containsValue(value);
    }

    public Set entrySet() {
        return resolvedObjectMap().entrySet();
    }

    public Object get(Object key) {
        return resolvedObjectMap().get(key);
    }

    public boolean isEmpty() {
        return resolvedObjectMap().isEmpty();
    }

    public Set keySet() {
        return resolvedObjectMap().keySet();
    }

    public Object put(Object key, Object value) {
        // TODO: andrus 8/20/2007 implement handling local changes without faulting like
        // PersistentObjectList does

        Object oldValue = resolvedObjectMap().put(key, value);

        postprocessAdd(value);
        postprocessRemove(oldValue);

        return oldValue;
    }

    public void putAll(Map map) {
        resolvedObjectMap().putAll(map);
        postprocessAdd(map.values());
    }

    public Object remove(Object key) {
        // TODO: andrus 8/20/2007 implement handling local changes without faulting like
        // PersistentObjectList does

        Object removed = resolvedObjectMap().remove(key);
        postprocessRemove(removed);
        return removed;
    }

    public int size() {
        return resolvedObjectMap().size();
    }

    public Collection values() {
        return resolvedObjectMap().values();
    }
    
    public void putDirectly(Object key, Object value) {
        //TODO: should not resolve manually
        resolvedObjectMap().put(key, value);
    }
    
    public void removeDirectly(Object key) {
        //TODO: should not resolve manually
        resolvedObjectMap().remove(key);
    }
}
