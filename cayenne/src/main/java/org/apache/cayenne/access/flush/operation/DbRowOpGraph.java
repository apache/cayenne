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
import java.util.function.IntPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dependency graph of {@link DbRowOp}s, with a topological sort (Kahn's algorithm) that keeps the ops of the same
 * group together. Vertices are addressed by the index of the op in the list the graph was created with, so the graph
 * doesn't depend on {@link DbRowOp} "equals". An edge "from -> to" means that "from" depends on "to", so "to" is
 * sorted before "from".
 */
class DbRowOpGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRowOpGraph.class);

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
     * Returns the topologically sorted ops, dependencies first.
     *
     * <p>A cycle doesn't stop the sort: one op of the cycle is sorted ahead of its dependencies, so that only the edges
     * of the cycles are broken, and the DB decides if the order works (it does, e.g., with no FK constraints, or with
     * the deferred ones). The op is the first by the comparator, then by the list order, among the ops of the cycles
     * that depend on nothing else unsorted.
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
        CycleBreaker cycleBreaker = null;
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
                    if (result.size() == vertexCount) {
                        break;
                    }

                    // the remaining ops are blocked by the cycles. Created on the first one, as it is rare
                    if (cycleBreaker == null) {
                        cycleBreaker = new CycleBreaker(pending, groupOrder);
                    }
                    int released = cycleBreaker.release();
                    pending[released] = 0;
                    ready.computeIfAbsent(ops.get(released), k -> new ArrayDeque<>()).add(released);
                }
                groupKey = ready.firstKey();
                group = ready.get(groupKey);
            }

            int op = group.poll();
            result.add(ops.get(op));

            for (int d = dependentsStart[op]; d < dependentsStart[op + 1]; d++) {
                int dependent = dependents[d];

                // a released op goes below zero, and is not made ready again
                if (--pending[dependent] == 0) {
                    ready.computeIfAbsent(ops.get(dependent), k -> new ArrayDeque<>()).add(dependent);
                }
            }
        }

        return result;
    }

    /**
     * Picks the ops to sort ahead of their dependencies when the sort is blocked by the dependency cycles. Called only
     * when no op is ready, so the unsorted ops are the ones with the pending dependencies (a released op goes below
     * zero). The strongly connected components of the unsorted ops are found once, on the first call, dependencies
     * first. Each call breaks a cycle in the first component with unsorted ops, as it depends on nothing else unsorted,
     * so only the edges within the component are broken, each being on a cycle. Releasing an op may leave more cycles
     * in the component, so the components of its unsorted ops are found again, but only of this component.
     */
    private final class CycleBreaker {

        private final int[] pending;
        private final Comparator<DbRowOp> groupOrder;

        // the components of the ops unsorted on the first call, dependencies first, and the component of each op
        private final List<int[]> components;
        private final int[] componentOf;

        // the first component that may have unsorted ops
        private int current;

        // the state of Tarjan's algorithm, reused by the runs
        private final int[] index;
        private final int[] low;
        private final boolean[] onStack;
        private final int[] stack;
        private final int[] callStack;
        private final int[] callEdge;

        CycleBreaker(int[] pending, Comparator<DbRowOp> groupOrder) {
            int size = ops.size();
            this.pending = pending;
            this.groupOrder = groupOrder;
            this.componentOf = new int[size];
            this.index = new int[size];
            this.low = new int[size];
            this.onStack = new boolean[size];
            this.stack = new int[size];
            this.callStack = new int[size];
            this.callEdge = new int[size];

            int[] unsorted = new int[vertexCount];
            int unsortedCount = 0;
            for (int i = 0; i < vertexCount; i++) {
                if (pending[vertexOrder[i]] > 0) {
                    unsorted[unsortedCount++] = vertexOrder[i];
                }
            }

            this.components = components(Arrays.copyOf(unsorted, unsortedCount), v -> pending[v] > 0, false);
            for (int c = 0; c < components.size(); c++) {
                for (int v : components.get(c)) {
                    componentOf[v] = c;
                }
            }
        }

        int release() {
            int[] unsorted = unsortedOf(components.get(current));
            while (unsorted.length == 0) {
                unsorted = unsortedOf(components.get(++current));
            }

            // every unsorted op of the component has an unsorted dependency, all of them in the component, so its
            // first component found, that depends on nothing else, is a cycle
            int component = current;
            int[] cycle = components(unsorted, v -> pending[v] > 0 && componentOf[v] == component, true).getFirst();

            int released = cycle[0];
            for (int v : cycle) {
                int c = groupOrder.compare(ops.get(v), ops.get(released));
                if (c < 0 || c == 0 && v < released) {
                    released = v;
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Dependency cycle of the operations {}, sorting {} ahead of its dependencies",
                        Arrays.stream(cycle).mapToObj(ops::get).toList(), ops.get(released));
            }
            return released;
        }

        private int[] unsortedOf(int[] component) {
            return Arrays.stream(component).filter(v -> pending[v] > 0).toArray();
        }

        // Tarjan's algorithm on the ops in scope, iterative, so that a long chain of dependencies doesn't overflow the
        // stack. Returns the strongly connected components, dependencies first, i.e. a component only depends on
        // itself and on the components before it. With "firstOnly", stops after the first one.
        private List<int[]> components(int[] scope, IntPredicate inScope, boolean firstOnly) {
            for (int v : scope) {
                index[v] = -1;
                onStack[v] = false;
            }

            List<int[]> result = new ArrayList<>();
            int counter = 0;
            int stackSize = 0;
            for (int root : scope) {
                if (index[root] >= 0) {
                    continue;
                }

                index[root] = low[root] = counter++;
                stack[stackSize++] = root;
                onStack[root] = true;
                int depth = 0;
                callStack[0] = root;
                callEdge[0] = 0;

                while (depth >= 0) {
                    int v = callStack[depth];
                    int e = callEdge[depth];

                    if (e < edgeCounts[v]) {
                        callEdge[depth] = e + 1;
                        int w = edges[v][e];
                        if (!inScope.test(w)) {
                            continue;
                        }

                        if (index[w] < 0) {
                            index[w] = low[w] = counter++;
                            stack[stackSize++] = w;
                            onStack[w] = true;
                            depth++;
                            callStack[depth] = w;
                            callEdge[depth] = 0;
                        } else if (onStack[w]) {
                            low[v] = Math.min(low[v], index[w]);
                        }
                        continue;
                    }

                    // all the dependencies of "v" are visited
                    if (low[v] == index[v]) {
                        int start = stackSize - 1;
                        while (stack[start] != v) {
                            start--;
                        }
                        int[] component = Arrays.copyOfRange(stack, start, stackSize);
                        for (int c : component) {
                            onStack[c] = false;
                        }
                        stackSize = start;
                        result.add(component);
                        if (firstOnly) {
                            return result;
                        }
                    }

                    depth--;
                    if (depth >= 0) {
                        int parent = callStack[depth];
                        low[parent] = Math.min(low[parent], low[v]);
                    }
                }
            }

            return result;
        }
    }
}
