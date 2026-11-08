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
package org.apache.cayenne.map.compiled;

import java.util.Arrays;

/**
 * Computes the dependency levels of the nodes of a directed graph, such as the tables of a schema with their foreign
 * key references: a node referencing nothing is at level 0, any other node is one level above the deepest node it
 * references, and the nodes of a cycle (a strongly connected component) share a level, computed from the nodes they
 * reference outside the cycle. Sorting the nodes by level puts every node after the nodes it references, cycles aside.
 *
 * <p>One pass of Tarjan's strongly connected components algorithm, iterative, over int arrays: linear in the nodes
 * and the references. A component's level is known the moment the component completes, as every component it
 * references has completed before it. This runs once per startup, so it is written to do little work rather than to
 * read elegantly.
 *
 * @since 6.0
 */
final class DependencyLevels {

    private DependencyLevels() {
    }

    /**
     * @param referenced for each node, the nodes it references, as indexes into the outer array. Self references
     *                   and duplicates are allowed and make no difference.
     * @return the level of each node, by the same indexes
     */
    static int[] compute(int[][] referenced) {
        int n = referenced.length;
        int[] index = new int[n];     // discovery order, -1 until a node is visited
        int[] lowLink = new int[n];   // the smallest discovery order reachable from a node
        int[] component = new int[n]; // -1 until the node's component is complete
        int[] level = new int[n];
        boolean[] onStack = new boolean[n];
        int[] stack = new int[n];     // the visited nodes whose component is not complete yet
        int[] path = new int[n];      // the depth-first path, in place of recursion
        int[] nextEdge = new int[n];  // for each path position, the next reference to follow
        Arrays.fill(index, -1);
        Arrays.fill(component, -1);
        int counter = 0;
        int top = 0;
        int components = 0;

        for (int root = 0; root < n; root++) {
            if (index[root] != -1) {
                continue;
            }
            int depth = 0;
            path[0] = root;
            nextEdge[0] = 0;
            index[root] = counter;
            lowLink[root] = counter++;
            stack[top++] = root;
            onStack[root] = true;

            while (depth >= 0) {
                int node = path[depth];
                int[] edges = referenced[node];
                if (nextEdge[depth] < edges.length) {
                    int target = edges[nextEdge[depth]++];
                    if (index[target] == -1) {
                        // descend
                        index[target] = counter;
                        lowLink[target] = counter++;
                        stack[top++] = target;
                        onStack[target] = true;
                        path[++depth] = target;
                        nextEdge[depth] = 0;
                    } else if (onStack[target]) {
                        lowLink[node] = Math.min(lowLink[node], index[target]);
                    }
                    continue;
                }

                // every reference of the node followed
                if (lowLink[node] == index[node]) {
                    // the node roots a component: pop the component, everything it references outside is complete
                    int first = top;
                    int member;
                    do {
                        member = stack[--top];
                        onStack[member] = false;
                        component[member] = components;
                    } while (member != node);

                    int componentLevel = 0;
                    for (int i = top; i < first; i++) {
                        for (int target : referenced[stack[i]]) {
                            if (component[target] != components) {
                                componentLevel = Math.max(componentLevel, level[target] + 1);
                            }
                        }
                    }
                    for (int i = top; i < first; i++) {
                        level[stack[i]] = componentLevel;
                    }
                    components++;
                }

                // ascend
                depth--;
                if (depth >= 0) {
                    int parent = path[depth];
                    lowLink[parent] = Math.min(lowLink[parent], lowLink[node]);
                }
            }
        }
        return level;
    }
}
