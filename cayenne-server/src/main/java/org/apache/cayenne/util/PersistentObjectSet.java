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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;

/**
 * @since 3.0
 */
public class PersistentObjectSet extends RelationshipFault 
    implements Set, ValueHolder, PersistentObjectCollection {

    // wrapped objects set
    protected Set objectSet;

    // track additions/removals in unresolved...
    protected Set addedToUnresolved;
    protected Set removedFromUnresolved;

    // exists for the benefit of manual serialization schemes such as the one in Hessian.
    private PersistentObjectSet() {
    }

    public PersistentObjectSet(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
    }

    /**
     * Returns whether this list is not yet resolved and requires a fetch.
     */
    public boolean isFault() {

        if (objectSet != null) {
            return false;
        }
        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        // synchronize??
        else if (isTransientParent()) {
            objectSet = new HashSet();
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Turns itself into a fault, thus forcing a refresh on the next access.
     */
    public void invalidate() {
        setObjectSet(null);
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {
        Object old = this.objectSet;

        if (value == null || value instanceof Set) {
            setObjectSet((Set) value);
        }
        // we can wrap non-set collections on the fly - this is needed for prefetch
        // handling...
        // although it seems to be breaking the contract for 'setValueDirectly' ???
        else if (value instanceof Collection) {
            setObjectSet(new HashSet((Collection) value));
        }
        else {
            throw new CayenneRuntimeException("Value must be a list, got: "
                    + value.getClass().getName());
        }

        return old;
    }

    public Object getValue() throws CayenneRuntimeException {
        return resolvedObjectSet();
    }

    public Object getValueDirectly() throws CayenneRuntimeException {
        return objectSet;
    }

    public Object setValue(Object value) throws CayenneRuntimeException {
        resolvedObjectSet();
        return setValueDirectly(objectSet);
    }

    public void setObjectSet(Set objectSet) {
        this.objectSet = objectSet;
    }

    // ====================================================
    // Standard Set Methods.
    // ====================================================

    public boolean add(Object o) {
        if ((isFault()) ? addLocal(o) : objectSet.add(o)) {
            postprocessAdd(o);
            return true;
        }

        return false;
    }

    public boolean addAll(Collection c) {
        if (resolvedObjectSet().addAll(c)) {
            // TODO: here we assume that all objects were added, while addAll may
            // technically return true and add only some objects... need a smarter
            // approach (maybe use "contains" in postprocessAdd"?)
            postprocessAdd(c);

            return true;
        }

        return false;
    }

    public void clear() {
        Set resolved = resolvedObjectSet();
        postprocessRemove(resolved);
        resolved.clear();
    }

    public boolean contains(Object o) {
        return resolvedObjectSet().contains(o);
    }

    public boolean containsAll(Collection c) {
        return resolvedObjectSet().containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof PersistentObjectSet)) {
            return false;
        }

        return resolvedObjectSet().equals(((PersistentObjectSet) o).resolvedObjectSet());
    }

    @Override
    public int hashCode() {
        return 53 + resolvedObjectSet().hashCode();
    }

    public boolean isEmpty() {
        return resolvedObjectSet().isEmpty();
    }

    public Iterator iterator() {
        return resolvedObjectSet().iterator();
    }

    public boolean remove(Object o) {
        if ((isFault()) ? removeLocal(o) : objectSet.remove(o)) {
            postprocessRemove(o);
            return true;
        }

        return false;
    }

    public boolean removeAll(Collection c) {
        if (resolvedObjectSet().removeAll(c)) {
            // TODO: here we assume that all objects were removed, while removeAll may
            // technically return true and remove only some objects... need a smarter
            // approach
            postprocessRemove(c);
            return true;
        }

        return false;
    }

    public boolean retainAll(Collection c) {
    	Collection toRemove = new HashSet(resolvedObjectSet().size());
    	for (Object object : resolvedObjectSet()) {
			if (!c.contains(object)) {
				toRemove.add(object);
			}
		}
    	
        boolean result = resolvedObjectSet().retainAll(c);
        if (result) {
        	postprocessRemove(toRemove);
        }
        return result;
    }

    public int size() {
        return resolvedObjectSet().size();
    }

    public Object[] toArray() {
        return resolvedObjectSet().toArray();
    }

    public Object[] toArray(Object[] a) {
        return resolvedObjectSet().toArray(a);
    }

    // ====================================================
    // Tracking set modifications, and resolving it
    // on demand
    // ====================================================

    /**
     * Returns internal objects list resolving it if needed.
     */
    protected Set resolvedObjectSet() {
        if (isFault()) {

            synchronized (this) {

                // now that we obtained the lock, check
                // if another thread just resolved the list
                if (isFault()) {
                    List localList = resolveFromDB();
                    this.objectSet = new HashSet(localList);
                }
            }
        }

        return objectSet;
    }

    void clearLocalChanges() {
        addedToUnresolved = null;
        removedFromUnresolved = null;
    }
    
    @Override
    protected void mergeLocalChanges(List resolved) {

        // only merge if an object is in an uncommitted state
        // any other state means that our local tracking
        // is invalid...
        if (isUncommittedParent()) {

            if (removedFromUnresolved != null) {
                resolved.removeAll(removedFromUnresolved);
            }

            // add only those that are not already on the list
            // do not include transient objects...
            if (addedToUnresolved != null) {

                for (Object next : addedToUnresolved) {

                    if (next instanceof Persistent) {
                        Persistent dataObject = (Persistent) next;
                        if (dataObject.getPersistenceState() == PersistenceState.TRANSIENT) {
                            continue;
                        }
                    }

                    if (!resolved.contains(next)) {
                        resolved.add(next);
                    }
                }
            }
        }

        // clear local information in any event
        clearLocalChanges();
    }

    boolean addLocal(Object object) {

        if (removedFromUnresolved != null) {
            removedFromUnresolved.remove(object);
        }

        if (addedToUnresolved == null) {
            addedToUnresolved = new HashSet();
        }

        addedToUnresolved.add(object);

        // this is really meaningless, since we don't know
        // if an object was present in the list
        return true;
    }

    boolean removeLocal(Object object) {
        if (addedToUnresolved != null) {
            addedToUnresolved.remove(object);
        }

        if (removedFromUnresolved == null) {
            removedFromUnresolved = new HashSet();
        }

        removedFromUnresolved.add(object);

        // this is really meaningless, since we don't know
        // if an object was present in the list
        return true;
    }

    void postprocessAdd(Collection<?> collection) {
        for (Object next : collection) {
            postprocessAdd(next);
        }
    }

    void postprocessRemove(Collection<?> collection) {
        for (Object next : collection) {
            postprocessRemove(next);
        }
    }

    void postprocessAdd(Object addedObject) {

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

    void postprocessRemove(Object removedObject) {

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

    @Override
    public String toString() {
        return (objectSet != null) ? objectSet.toString() : "[<unresolved>]";
    }

    public void addDirectly(Object target) {
        if (isFault()) {
            addLocal(target);
        }
        else {
            objectSet.add(target);
        }
    }

    public void removeDirectly(Object target) {
        if (isFault()) {
            removeLocal(target);
        }
        else {
            objectSet.remove(target);
        }
    }
}
