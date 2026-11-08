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
package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.ObjectSnapshots;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledRelationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Sorts the objects of one entity mapped to a reflexive table (a table with a foreign key to its own primary key,
 * e.g. a tree table with a parent reference) in dependency order: an object is placed after the objects it
 * references through the reflexive relationships. The reverse order is used for deletes. The objects may be either
 * {@link Persistent} objects or {@link DbRowOp} operations on them.
 *
 * <p>Linear in the objects and their reflexive references: the objects are indexed by identity once, and sorted
 * with Kahn's algorithm over int arrays. Objects that depend on nothing among the sorted ones keep their relative
 * order.
 *
 * @since 6.0
 */
final class ReflexiveObjectSorter {

    /**
     * Sorts the objects in place. Does nothing if the table of the entity is not reflexive.
     *
     * @param entity      the entity of all the objects
     * @param objects     a list of {@link DbRowOp} or {@link Persistent} objects
     * @param deleteOrder true to sort the objects in the order of deletion, false - in the order of insertion
     */
    @SuppressWarnings("unchecked")
    void sortObjectsForEntity(CompiledEntity entity, List<?> objects, boolean deleteOrder) {
        if (objects == null || objects.size() < 2) {
            return;
        }
        if (!entity.table().isReflexive()) {
            return;
        }
        Object probe = objects.getFirst();
        if (probe instanceof DbRowOp) {
            sort(entity, (List<DbRowOp>) objects, deleteOrder, DbRowOp::getObject);
        } else if (probe instanceof Persistent) {
            sort(entity, (List<Persistent>) objects, deleteOrder, Function.identity());
        } else {
            throw new IllegalArgumentException("Can sort only Persistent or DbRow objects, got "
                    + probe.getClass().getSimpleName());
        }
    }

    private <E> void sort(CompiledEntity entity, List<E> objects, boolean deleteOrder,
                          Function<E, Persistent> converter) {

        CompiledRelationship[] reflexiveRels = reflexiveRelationships(entity);
        int size = objects.size();

        // the objects by identity, to find each object's masters among the sorted objects
        Persistent[] persistents = new Persistent[size];
        Map<Persistent, Integer> positions = new IdentityHashMap<>(size * 2);
        for (int i = 0; i < size; i++) {
            persistents[i] = converter.apply(objects.get(i));
            positions.put(persistents[i], i);
        }

        // masters[i * rels + k]: the position of the master of object i over relationship k, or -1
        int rels = reflexiveRels.length;
        int[] masters = new int[size * rels];
        int[] dependentCount = new int[size];
        int[] inDegree = new int[size];
        int edges = 0;
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < rels; k++) {
                int master = -1;
                CompiledRelationship rel = reflexiveRels[k];
                if (rel != null) {
                    Persistent target = (Persistent) rel.readProperty(persistents[i]);
                    if (target == null) {
                        target = findReflexiveMaster(persistents[i], rel);
                    }
                    Integer position = target != null ? positions.get(target) : null;
                    // a master outside the sorted objects, or the object itself, is not a dependency
                    if (position != null && position != i) {
                        master = position;
                        dependentCount[master]++;
                        inDegree[i]++;
                        edges++;
                    }
                }
                masters[i * rels + k] = master;
            }
        }

        if (edges == 0) {
            // nothing depends on anything: insertion order is the list order, deletion order the reverse
            if (deleteOrder) {
                Collections.reverse(objects);
            }
            return;
        }

        // dependents of each master, in list order
        int[] start = new int[size + 1];
        for (int i = 0; i < size; i++) {
            start[i + 1] = start[i] + dependentCount[i];
        }
        int[] fill = new int[size];
        int[] dependents = new int[edges];
        for (int i = 0; i < size; i++) {
            for (int k = 0; k < rels; k++) {
                int master = masters[i * rels + k];
                if (master != -1) {
                    dependents[start[master] + fill[master]++] = i;
                }
            }
        }

        // Kahn's algorithm: objects become ready as their masters are emitted
        int[] order = new int[size];
        int ready = 0;
        for (int i = 0; i < size; i++) {
            if (inDegree[i] == 0) {
                order[ready++] = i;
            }
        }
        for (int next = 0; next < ready; next++) {
            int master = order[next];
            for (int e = start[master]; e < start[master + 1]; e++) {
                int dependent = dependents[e];
                if (--inDegree[dependent] == 0) {
                    order[ready++] = dependent;
                }
            }
        }
        if (ready < size) {
            throw new CayenneRuntimeException("Sorting objects for %s failed. Cycles found.", entity.name());
        }

        List<E> sorted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            sorted.add(objects.get(order[i]));
        }
        if (deleteOrder) {
            Collections.reverse(sorted);
        }
        objects.clear();
        objects.addAll(sorted);
    }

    /**
     * Returns the object relationships of the entity mapped to the reflexive relationships of its table, one per
     * reflexive DB relationship. An entry is null for a DB relationship that has no object relationship.
     */
    private static CompiledRelationship[] reflexiveRelationships(CompiledEntity entity) {
        List<CompiledJoin> reflexiveJoins = entity.table().reflexiveJoins();
        CompiledRelationship[] reflexiveRels = new CompiledRelationship[reflexiveJoins.size()];
        for (int i = 0; i < reflexiveRels.length; i++) {
            CompiledJoin join = reflexiveJoins.get(i);
            reflexiveRels[i] = join != null ? entity.relationshipForJoin(join) : null;
        }
        return reflexiveRels;
    }

    /**
     * Finds the master of an object whose to-one property is not resolved (a fault) in the committed snapshot of
     * the object, among the objects of its context. A new object has no snapshot, and its property is set or null.
     */
    private static Persistent findReflexiveMaster(Persistent object, CompiledRelationship toOneRel) {
        if (object.getObjectId().isTemporary()) {
            return null;
        }
        ObjectContext context = object.getObjectContext();
        // the committed snapshot, not the dirty one of the context: the object store has it
        DataRow snapshot = context.getObjectStore().getSnapshot(object.getObjectId());
        if (snapshot == null) {
            return null;
        }
        ObjectId targetId = ObjectSnapshots.targetId(toOneRel, snapshot);
        if (targetId == null) {
            return null;
        }
        // not 'localObject': only the objects taking part in the transaction matter, no need to create hollow ones
        return context.getObjectStore().getObject(targetId);
    }
}
