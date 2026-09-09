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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An abstract superclass of lazily faulted to-many relationship collections. Tracks additions and removals made
 * while the relationship is still unresolved, and merges them into the fetched result on resolution.
 *
 * @since 5.0
 */
public abstract class ToManyHolder<E> implements Serializable {

    protected Persistent relationshipOwner;
    protected String relationshipName;

    // additions and removals made while the relationship is unresolved
    protected Set<E> addedToUnresolved;
    protected Set<E> removedFromUnresolved;

    protected ToManyHolder() {
    }

    protected ToManyHolder(Persistent relationshipOwner, String relationshipName) {
        if (relationshipOwner == null) {
            throw new NullPointerException("'relationshipOwner' can't be null.");
        }

        if (relationshipName == null) {
            throw new NullPointerException("'relationshipName' can't be null.");
        }

        this.relationshipOwner = relationshipOwner;
        this.relationshipName = relationshipName;
    }

    /**
     * Returns whether this collection is not yet resolved and requires a fetch.
     */
    public abstract boolean isFault();

    /**
     * Turns this collection into a fault, thus forcing a refresh on the next access.
     */
    public abstract void invalidate();

    /**
     * Resolves this collection with the given objects without fetching them from the database. Used to inject
     * prefetched results.
     */
    public abstract void resolveWith(Collection<E> objects);

    protected boolean isTransientParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.NEW || state == PersistenceState.TRANSIENT;
    }

    protected boolean isUncommittedParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.MODIFIED || state == PersistenceState.DELETED;
    }

    /**
     * Resolves related objects via the DataChannel stack of the owner's context, merging in any local changes made
     * while the relationship was unresolved. Subclasses invoke this method whenever they need to resolve a fault.
     */
    @SuppressWarnings("unchecked")
    protected List<E> resolveRelationship() {
        // non-persistent objects shouldn't trigger a fetch
        if (isTransientParent()) {
            return new ArrayList<>();
        }

        DataContext context = (DataContext) relationshipOwner.getObjectContext();
        List<E> resolved = (List<E>) context.resolveRelationship(
                relationshipOwner.getObjectId(),
                relationshipName,
                false);

        mergeLocalChanges(resolved);
        return resolved;
    }

    /**
     * Records an addition made while the relationship is unresolved.
     */
    protected void addLocal(E object) {
        if (removedFromUnresolved != null) {
            removedFromUnresolved.remove(object);
        }

        if (addedToUnresolved == null) {
            addedToUnresolved = new LinkedHashSet<>();
        }

        addedToUnresolved.add(object);
    }

    /**
     * Records a removal made while the relationship is unresolved.
     */
    protected void removeLocal(E object) {
        if (addedToUnresolved != null) {
            addedToUnresolved.remove(object);
        }

        // no point in tracking a new or transient object - it will never be fetched from the database
        if (object instanceof Persistent persistent) {
            int state = persistent.getPersistenceState();
            if (state == PersistenceState.TRANSIENT || state == PersistenceState.NEW) {
                return;
            }
        }

        if (removedFromUnresolved == null) {
            removedFromUnresolved = new LinkedHashSet<>();
        }

        removedFromUnresolved.add(object);
    }

    /**
     * Applies additions and removals recorded while unresolved to a freshly fetched list, then discards them.
     */
    protected void mergeLocalChanges(List<E> fetched) {

        // only merge if the owner is in an uncommitted state; any other state means that our local tracking
        // is invalid
        if (isUncommittedParent()) {
            if (removedFromUnresolved != null) {
                fetched.removeAll(removedFromUnresolved);
            }

            // add only those that are not already on the list, skipping transient objects
            if (addedToUnresolved != null) {
                for (E next : addedToUnresolved) {
                    if (next instanceof Persistent persistent
                            && persistent.getPersistenceState() == PersistenceState.TRANSIENT) {
                        continue;
                    }

                    if (!fetched.contains(next)) {
                        fetched.add(next);
                    }
                }
            }
        }

        // clear local information in any event
        addedToUnresolved = null;
        removedFromUnresolved = null;
    }
}
