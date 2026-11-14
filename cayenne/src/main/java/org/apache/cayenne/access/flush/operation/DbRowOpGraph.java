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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A dependency graph of {@link DbRowOp}s, with a linear-time (Kahn's algorithm) topological sort. Vertices are
 * addressed by the index of the op in the list the graph was created with, so the graph does no hashing, no boxing,
 * and doesn't depend on {@link DbRowOp} "equals". An edge "from -> to" means that "from" depends on "to", so "to"
 * is sorted before "from".
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
     */
    List<DbRowOp> topSort() {
        int n = vertexCount;
        int[] inDegree = new int[ops.size()];

        // in-degrees, plus the order of the vertices to seed the queue with. This order (each vertex's targets
        // first, then the vertex itself) is preserved from the original map-based implementation, as it determines
        // the relative order of the independent ops.
        int[] degreeOrder = new int[n];
        int degreeOrderSize = 0;
        boolean[] seen = new boolean[ops.size()];
        for (int i = 0; i < n; i++) {
            int from = vertexOrder[i];
            int[] fromEdges = edges[from];
            for (int e = 0, count = edgeCounts[from]; e < count; e++) {
                int to = fromEdges[e];
                inDegree[to]++;
                if (!seen[to]) {
                    seen[to] = true;
                    degreeOrder[degreeOrderSize++] = to;
                }
            }
            if (!seen[from]) {
                seen[from] = true;
                degreeOrder[degreeOrderSize++] = from;
            }
        }

        // each vertex is queued exactly once, so a plain array works as a FIFO queue. Seed vertices go in reverse
        int[] queue = new int[n];
        int tail = 0;
        for (int i = degreeOrderSize - 1; i >= 0; i--) {
            if (inDegree[degreeOrder[i]] == 0) {
                queue[tail++] = degreeOrder[i];
            }
        }

        for (int head = 0; head < tail; head++) {
            int v = queue[head];
            int[] vEdges = edges[v];
            for (int e = 0, count = edgeCounts[v]; e < count; e++) {
                int to = vEdges[e];
                if (--inDegree[to] == 0) {
                    queue[tail++] = to;
                }
            }
        }

        // if not all the vertices were queued, there was a cycle
        if (tail != n) {
            List<DbRowOp> remaining = new ArrayList<>(n - tail);
            for (int i = 0; i < n; i++) {
                if (inDegree[vertexOrder[i]] > 0) {
                    remaining.add(ops.get(vertexOrder[i]));
                }
            }
            throw new IllegalStateException("Cycle detected in list for keys: " + remaining);
        }

        // the queue has the dependents first, so reverse it
        List<DbRowOp> result = new ArrayList<>(n);
        for (int i = n - 1; i >= 0; i--) {
            result.add(ops.get(queue[i]));
        }
        return result;
    }
}
