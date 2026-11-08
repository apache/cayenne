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
import org.apache.cayenne.ashwood.graph.Digraph;
import org.apache.cayenne.ashwood.graph.IndegreeTopologicalSort;
import org.apache.cayenne.ashwood.graph.MapDigraph;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Sorts the objects of one entity mapped to a reflexive table (a table with a foreign key to its own primary key,
 * e.g. a tree table with a parent reference) in dependency order: an object is placed after the objects it
 * references through the reflexive relationships. The reverse order is used for deletes. The objects may be either
 * {@link Persistent} objects or {@link DbRowOp} operations on them.
 *
 * @since 6.0
 */
public class ReflexiveObjectSorter {

    /**
     * Sorts the objects in place. Does nothing if the table of the entity is not reflexive.
     *
     * @param entity      the entity of all the objects
     * @param objects     a list of {@link DbRowOp} or {@link Persistent} objects
     * @param deleteOrder true to sort the objects in the order of deletion, false - in the order of insertion
     */
    @SuppressWarnings("unchecked")
    public void sortObjectsForEntity(CompiledEntity entity, List<?> objects, boolean deleteOrder) {
        if (objects == null || objects.isEmpty()) {
            return;
        }

        // if no sorting is required
        if (!entity.table().isReflexive()) {
            return;
        }

        Object probe = objects.getFirst();
        if (probe instanceof DbRowOp) {
            sortObjectsForEntity(entity, (List<DbRowOp>) objects, deleteOrder, DbRowOp::getObject);
        } else if (probe instanceof Persistent) {
            sortObjectsForEntity(entity, (List<Persistent>) objects, deleteOrder, Function.identity());
        } else {
            throw new IllegalArgumentException("Can sort only Persistent or DbRow objects, got "
                    + probe.getClass().getSimpleName());
        }
    }

    protected <E> void sortObjectsForEntity(
            CompiledEntity entity,
            List<E> objects,
            boolean deleteOrder,
            Function<E, Persistent> converter) {

        Digraph<E, Boolean> objectDependencyGraph = buildDigraph(entity, objects, converter);

        if (!topologicalSort(objects, objectDependencyGraph, deleteOrder)) {
            throw new CayenneRuntimeException("Sorting objects for %s failed. Cycles found.", entity.name());
        }
    }

    protected <E> Digraph<E, Boolean> buildDigraph(
            CompiledEntity entity,
            List<E> objects,
            Function<E, Persistent> converter) {

        CompiledRelationship[] reflexiveRels = getReflexiveRelationships(entity);

        int size = objects.size();
        Digraph<E, Boolean> objectDependencyGraph = new MapDigraph<>();
        Persistent[] masters = new Persistent[reflexiveRels.length];
        for (int i = 0; i < size; i++) {
            E current = objects.get(i);
            objectDependencyGraph.addVertex(current);
            int actualMasterCount = 0;
            for (int k = 0; k < reflexiveRels.length; k++) {
                CompiledRelationship reflexiveRel = reflexiveRels[k];

                if (reflexiveRel == null) {
                    continue;
                }

                Persistent persistent = converter.apply(current);
                masters[k] = (Persistent) reflexiveRel.readProperty(persistent);

                if (masters[k] == null) {
                    masters[k] = findReflexiveMaster(persistent, reflexiveRel);
                }

                if (masters[k] != null) {
                    actualMasterCount++;
                }
            }

            int mastersFound = 0;
            for (int j = 0; j < size && mastersFound < actualMasterCount; j++) {

                if (i == j) {
                    continue;
                }

                E masterCandidate = objects.get(j);
                for (Persistent master : masters) {
                    if (converter.apply(masterCandidate) == master) {
                        objectDependencyGraph.putArc(masterCandidate, current, Boolean.TRUE);
                        mastersFound++;
                    }
                }
            }
        }
        return objectDependencyGraph;
    }

    protected <E> boolean topologicalSort(List<E> data, Digraph<E, Boolean> graph, boolean reverse) {
        IndegreeTopologicalSort<E> sorter = new IndegreeTopologicalSort<>(graph);
        List<E> sorted = new ArrayList<>(data.size());

        while (sorter.hasNext()) {
            E o = sorter.next();
            if (o == null) {
                return false;
            }
            sorted.add(o);
        }

        // since API requires sorting within the same array,
        // simply replace all objects with objects in the right order...
        // may come up with something cleaner later
        data.clear();
        data.addAll(sorted);

        if (reverse) {
            Collections.reverse(data);
        }
        return true;
    }

    /**
     * Returns the object relationships of the entity mapped to the reflexive relationships of its table, one per
     * reflexive DB relationship. An entry is null for a DB relationship that has no object relationship.
     */
    protected CompiledRelationship[] getReflexiveRelationships(CompiledEntity entity) {
        List<CompiledJoin> reflexiveJoins = entity.table().reflexiveJoins();
        CompiledRelationship[] reflexiveRels = new CompiledRelationship[reflexiveJoins.size()];
        for (int i = 0; i < reflexiveRels.length; i++) {
            CompiledJoin join = reflexiveJoins.get(i);
            reflexiveRels[i] = join != null ? entity.relationshipForJoin(join) : null;
        }
        return reflexiveRels;
    }

    protected Persistent findReflexiveMaster(Persistent object, CompiledRelationship toOneRel) {

        ObjectContext context = object.getObjectContext();

        // find committed snapshot - so we can't fetch from the context as it will return dirty snapshot;
        // must go down the stack instead

        // how do we handle this for NEW objects correctly? For now bail from the method
        if (object.getObjectId().isTemporary()) {
            return null;
        }

        DataRow snapshot = context.getObjectStore().getSnapshot(object.getObjectId());
        if (snapshot == null) {
            return null;
        }

        ObjectId targetId = ObjectSnapshots.targetId(toOneRel, snapshot);
        if (targetId == null) {
            return null;
        }

        // not using 'localObject', looking up in context instead, as within the sorter
        // we only care about objects participating in transaction, so no need to create hollow objects
        return context.getObjectStore().getObject(targetId);
    }
}
