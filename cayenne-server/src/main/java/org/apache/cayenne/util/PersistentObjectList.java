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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;

/**
 * A list of persistent objects lazily resolved on the first access.
 * 
 * @since 1.2
 */
public class PersistentObjectList extends RelationshipFault implements List, ValueHolder,
        PersistentObjectCollection {

    // wrapped objects list
    protected List objectList;

    // track additions/removals in unresolved...
    protected LinkedList<Object> addedToUnresolved;
    protected LinkedList<Object> removedFromUnresolved;

    // exists for the benefit of custom serialization schemes such as the one in Hessian.
    @SuppressWarnings("unused")
    private PersistentObjectList() {

    }

    /**
     * Creates PersistentObjectList initializing it with list owner persistent object and
     * relationship name that this list maps to.
     * 
     * @param relationshipOwner persistent object that owns this list.
     * @param relationshipName a query used to resolve the list
     */
    public PersistentObjectList(Persistent relationshipOwner, String relationshipName) {
        super(relationshipOwner, relationshipName);
    }

    /**
     * Returns whether this list is not yet resolved and requires a fetch.
     */
    public boolean isFault() {

        if (objectList != null) {
            return false;
        }
        // resolve on the fly if owner is transient... Can't do it in constructor, as
        // object may be in an inconsistent state during construction time
        // synchronize??
        else if (isTransientParent()) {
            objectList = new LinkedList();
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
        setObjectList(null);
    }

    public Object setValueDirectly(Object value) throws CayenneRuntimeException {
        if (value == null || value instanceof List) {
            Object old = this.objectList;
            setObjectList((List) value);
            return old;
        }
        else {
            throw new CayenneRuntimeException("Value must be a list, got: "
                    + value.getClass().getName());
        }
    }

    public Object getValue() throws CayenneRuntimeException {
        return resolvedObjectList();
    }

    public Object getValueDirectly() throws CayenneRuntimeException {
        return objectList;
    }

    public Object setValue(Object value) throws CayenneRuntimeException {
        resolvedObjectList();
        return setValueDirectly(value);
    }

    public void setObjectList(List objectList) {
        this.objectList = objectList;
    }

    // ====================================================
    // Standard List Methods.
    // ====================================================

    public boolean add(Object o) {
        if ((isFault()) ? addLocal(o) : objectList.add(o)) {
            postprocessAdd(o);
            return true;
        }

        return false;
    }

    public void add(int index, Object o) {
        resolvedObjectList().add(index, o);
        postprocessAdd(o);
    }

    public boolean addAll(Collection c) {
        if (resolvedObjectList().addAll(c)) {
            // TODO: here we assume that all objects were added, while addAll may
            // technically return true and add only some objects... need a smarter
            // approach (maybe use "contains" in postprocessAdd"?)
            postprocessAdd(c);

            return true;
        }

        return false;
    }

    public boolean addAll(int index, Collection c) {
        if (resolvedObjectList().addAll(index, c)) {
            // TODO: here we assume that all objects were added, while addAll may
            // technically return true and add only some objects... need a smarter
            // approach (maybe use "contains" in postprocessAdd"?)
            postprocessAdd(c);

            return true;
        }

        return false;
    }

    public void clear() {
        List resolved = resolvedObjectList();
        postprocessRemove(resolved);
        resolved.clear();
    }

    public boolean contains(Object o) {
        return resolvedObjectList().contains(o);
    }

    public boolean containsAll(Collection c) {
        return resolvedObjectList().containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof PersistentObjectList)) {
            return false;
        }

        return resolvedObjectList().equals(
                ((PersistentObjectList) o).resolvedObjectList());
    }

    @Override
    public int hashCode() {
        return 37 + resolvedObjectList().hashCode();
    }

    public Object get(int index) {
        return resolvedObjectList().get(index);
    }

    public int indexOf(Object o) {
        return resolvedObjectList().indexOf(o);
    }

    public boolean isEmpty() {
        return resolvedObjectList().isEmpty();
    }

    public Iterator iterator() {
        return resolvedObjectList().iterator();
    }

    public int lastIndexOf(Object o) {
        return resolvedObjectList().lastIndexOf(o);
    }

    public ListIterator listIterator() {
        return resolvedObjectList().listIterator();
    }

    public ListIterator listIterator(int index) {
        return resolvedObjectList().listIterator(index);
    }

    public Object remove(int index) {
        Object removed = resolvedObjectList().remove(index);
        postprocessRemove(removed);
        return removed;
    }

    public boolean remove(Object o) {
        if ((isFault()) ? removeLocal(o) : objectList.remove(o)) {
            postprocessRemove(o);
            return true;
        }

        return false;
    }

    public boolean removeAll(Collection c) {
        if (resolvedObjectList().removeAll(c)) {
            // TODO: here we assume that all objects were removed, while removeAll may
            // technically return true and remove only some objects... need a smarter
            // approach
            postprocessRemove(c);
            return true;
        }

        return false;
    }

    public boolean retainAll(Collection c) {
        // TODO: handle object graoh change notifications on object removals...
        return resolvedObjectList().retainAll(c);
    }

    public Object set(int index, Object o) {
        Object oldValue = resolvedObjectList().set(index, o);

        postprocessAdd(o);
        postprocessRemove(oldValue);

        return oldValue;
    }

    public int size() {
        return resolvedObjectList().size();
    }

    public List subList(int fromIndex, int toIndex) {
        // TODO: should we wrap a sublist into a list that does notifications on
        // additions/removals?
        return resolvedObjectList().subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return resolvedObjectList().toArray();
    }

    public Object[] toArray(Object[] a) {
        return resolvedObjectList().toArray(a);
    }

    // ====================================================
    // Tracking list modifications, and resolving it
    // on demand
    // ====================================================

    /**
     * Returns internal objects list resolving it if needed.
     */
    protected List resolvedObjectList() {
        if (isFault()) {

            synchronized (this) {

                // now that we obtained the lock, check
                // if another thread just resolved the list
                if (isFault()) {
                    this.objectList = resolveFromDB();
                }
            }
        }

        return objectList;
    }

    protected void clearLocalChanges() {
        addedToUnresolved = null;
        removedFromUnresolved = null;
    }

    protected void mergeLocalChanges(List fetchedList) {

        // only merge if an object is in an uncommitted state
        // any other state means that our local tracking
        // is invalid...
        if (isUncommittedParent()) {

            if (removedFromUnresolved != null) {
                fetchedList.removeAll(removedFromUnresolved);
            }

            // add only those that are not already on the list
            // do not include transient objects...
            if (addedToUnresolved != null && !addedToUnresolved.isEmpty()) {
                Iterator it = addedToUnresolved.iterator();
                while (it.hasNext()) {
                    Object next = it.next();

                    if (next instanceof Persistent) {
                        Persistent dataObject = (Persistent) next;
                        if (dataObject.getPersistenceState() == PersistenceState.TRANSIENT) {
                            continue;
                        }
                    }

                    if (!fetchedList.contains(next)) {
                        fetchedList.add(next);
                    }
                }
            }
        }

        // clear local information in any event
        clearLocalChanges();
    }

    protected boolean addLocal(Object object) {

        if (removedFromUnresolved != null) {
            removedFromUnresolved.remove(object);
        }

        if (addedToUnresolved == null) {
            addedToUnresolved = new LinkedList();
        }

        addedToUnresolved.addLast(object);

        // this is really meaningless, since we don't know
        // if an object was present in the list
        return true;
    }

    protected boolean removeLocal(Object object) {
        if (addedToUnresolved != null) {
            addedToUnresolved.remove(object);
        }

        if (removedFromUnresolved == null) {
            removedFromUnresolved = new LinkedList<Object>();
        }

        if (shouldAddToRemovedFromUnresolvedList(object)) {
            removedFromUnresolved.addLast(object);
        }

        // this is really meaningless, since we don't know
        // if an object was present in the list
        return true;
    }

    /**
     * @return whether object should be added to {@link #removedFromUnresolved} during
     *         removal
     */
    protected boolean shouldAddToRemovedFromUnresolvedList(Object object) {
        return true;
    }

    protected void postprocessAdd(Collection<?> collection) {
        for (Object next : collection) {
            postprocessAdd(next);
        }
    }

    protected void postprocessRemove(Collection<?> collection) {
        for (Object next : collection) {
            postprocessRemove(next);
        }
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
                Util.setReverse(
                        relationshipOwner,
                        relationshipName,
                        (Persistent) addedObject);
            }
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
                Util.unsetReverse(
                        relationshipOwner,
                        relationshipName,
                        (Persistent) removedObject);
            }
        }
    }

    @Override
    public String toString() {
        return (objectList != null) ? objectList.toString() : "[<unresolved>]";
    }

    public void addDirectly(Object target) {
        if (isFault()) {
            addLocal(target);
        }
        else {
            objectList.add(target);
        }
    }

    public void removeDirectly(Object target) {
        if (isFault()) {
            removeLocal(target);
        }
        else {
            objectList.remove(target);
        }
    }
}
