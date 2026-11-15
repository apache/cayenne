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

package org.apache.cayenne.access.flush.operation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * A dependency graph of {@link DbRowOp}s, with a topological sort (Kahn's algorithm) that keeps the ops of the same
 * group together. Vertices are addressed by the index of the op in the list the graph was created with, so the graph
 * doesn't depend on {@link DbRowOp} "equals". An edge "from -> to" means that "from" depends on "to", so "to" is
 * sorted before "from".
 */
class DbRowOpGraph {

    private static final int[] NO_EDGES = new int[0];

    private final List<DbRowOp> ops;

    // adjacency lists, in the edge insertion order
    private final int[][] edges;
    private final int[] edgeCounts;

    // vertices in the order they were first added to the graph, either explicitly or as an edge end
    private final int[] vertexOrder;
    private final boolean[] inGraph;
    private int vertexCount;

    DbRowOpGraph(List<DbRowOp> ops) {
        int size = ops.size();
        this.ops = ops;
        this.edges = new int[size][];
        this.edgeCounts = new int[size];
        this.vertexOrder = new int[size];
        this.inGraph = new boolean[size];
        Arrays.fill(edges, NO_EDGES);
    }

    /**
     * Adds a vertex to the graph. Nothing happens if the vertex is already in the graph.
     */
    void add(int vertex) {
        if (!inGraph[vertex]) {
            inGraph[vertex] = true;
            vertexOrder[vertexCount++] = vertex;
        }
    }

    /**
     * Adds an edge to the graph, adding either vertex if it is not there yet. Multi-edges are allowed.
     */
    void add(int from, int to) {
        add(from);

        int[] fromEdges = edges[from];
        int count = edgeCounts[from];
        if (count == fromEdges.length) {
            fromEdges = Arrays.copyOf(fromEdges, Math.max(4, count * 2));
            edges[from] = fromEdges;
        }
        fromEdges[count] = to;
        edgeCounts[from] = count + 1;

        add(to);
    }

    /**
     * Returns the topologically sorted ops, dependencies first. Throws an exception if a cycle is detected.
     *
     * <p>Of the ops whose dependencies are sorted, the next is taken from the same group as the previous one while
     * there is one, the groups being the ops equal by the comparator, so that the ops of a group stay together as much
     * as the dependencies allow (e.g., to run as one batch). Otherwise, it is taken from the first group by the
     * comparator. Within a group, the ops keep their order in the list. So, without the dependencies, the result is the
     * list stably sorted by the comparator.
     */
    List<DbRowOp> topSort(Comparator<DbRowOp> groupOrder) {
        int size = ops.size();

        // the dependents of each op, and the number of the dependencies of each op not sorted yet
        int[] pending = new int[size];
        int[] dependentsStart = new int[size + 1];
        for (int i = 0; i < vertexCount; i++) {
            int from = vertexOrder[i];
            pending[from] = edgeCounts[from];
            int[] fromEdges = edges[from];
            for (int e = 0; e < edgeCounts[from]; e++) {
                dependentsStart[fromEdges[e] + 1]++;
            }
        }
        for (int i = 0; i < size; i++) {
            dependentsStart[i + 1] += dependentsStart[i];
        }
        int[] dependents = new int[dependentsStart[size]];
        int[] fill = new int[size];
        for (int from = 0; from < size; from++) {
            int[] fromEdges = edges[from];
            for (int e = 0; e < edgeCounts[from]; e++) {
                int to = fromEdges[e];
                dependents[dependentsStart[to] + fill[to]++] = from;
            }
        }

        // The ready ops by group, each group in the list order. Picking the next op from the same group as the
        // previous one, rather than in any order the dependencies allow, is essential: the flush only puts the
        // consecutive operations of the same kind into one batch (see QueryCreatorVisitor), so an order that
        // interleaves the groups turns every row into a separate statement, i.e. a DB round trip per row instead of
        // one per batch.
        TreeMap<DbRowOp, ArrayDeque<Integer>> ready = new TreeMap<>(groupOrder);
        for (int i = 0; i < size; i++) {
            if (inGraph[i] && pending[i] == 0) {
                ready.computeIfAbsent(ops.get(i), k -> new ArrayDeque<>()).add(i);
            }
        }

        List<DbRowOp> result = new ArrayList<>(vertexCount);
        DbRowOp groupKey = null;
        ArrayDeque<Integer> group = null;
        while (true) {

            // stay in the current group while it has ready ops, including the ones that became ready while sorting
            // it, even if an earlier group by the comparator became ready, as each switch of the group ends a batch
            if (group == null || group.isEmpty()) {
                if (groupKey != null) {
                    ready.remove(groupKey);
                }
                if (ready.isEmpty()) {
                    break;
                }
                groupKey = ready.firstKey();
                group = ready.get(groupKey);
            }

            int op = group.poll();
            result.add(ops.get(op));

            for (int d = dependentsStart[op]; d < dependentsStart[op + 1]; d++) {
                int dependent = dependents[d];
                if (--pending[dependent] == 0) {
                    ready.computeIfAbsent(ops.get(dependent), k -> new ArrayDeque<>()).add(dependent);
                }
            }
        }

        // if not all the vertices were sorted, there was a cycle
        if (result.size() != vertexCount) {
            List<DbRowOp> remaining = new ArrayList<>(vertexCount - result.size());
            for (int i = 0; i < vertexCount; i++) {
                if (pending[vertexOrder[i]] > 0) {
                    remaining.add(ops.get(vertexOrder[i]));
                }
            }
            throw new IllegalStateException("Cycle detected in list for keys: " + remaining);
        }

        return result;
    }
}
