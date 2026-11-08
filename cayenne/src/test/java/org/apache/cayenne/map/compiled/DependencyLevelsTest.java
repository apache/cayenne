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

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DependencyLevelsTest {

    private static final int[] NONE = {};

    @Test
    public void empty() {
        assertArrayEquals(new int[0], DependencyLevels.compute(new int[0][]));
    }

    @Test
    public void independentNodes() {
        assertArrayEquals(new int[]{0, 0, 0}, DependencyLevels.compute(new int[][]{NONE, NONE, NONE}));
    }

    @Test
    public void chain() {
        // 0 -> 1 -> 2 -> 3, in the two possible numberings
        assertArrayEquals(new int[]{3, 2, 1, 0}, DependencyLevels.compute(new int[][]{{1}, {2}, {3}, NONE}));
        assertArrayEquals(new int[]{0, 1, 2, 3}, DependencyLevels.compute(new int[][]{NONE, {0}, {1}, {2}}));
    }

    @Test
    public void diamond() {
        // 3 references 1 and 2, both reference 0
        assertArrayEquals(new int[]{0, 1, 1, 2}, DependencyLevels.compute(new int[][]{NONE, {0}, {0}, {1, 2}}));
    }

    @Test
    public void deepestReferenceWins() {
        // 2 references 0 directly and through 1
        assertArrayEquals(new int[]{0, 1, 2}, DependencyLevels.compute(new int[][]{NONE, {0}, {0, 1}}));
    }

    @Test
    public void cycleSharesLevel() {
        // 0 <-> 1 form a cycle above 2; 3 references the cycle
        int[] levels = DependencyLevels.compute(new int[][]{{1, 2}, {0}, NONE, {1}});
        assertArrayEquals(new int[]{1, 1, 0, 2}, levels);
    }

    @Test
    public void selfReferenceIsNotADependency() {
        assertArrayEquals(new int[]{0, 1}, DependencyLevels.compute(new int[][]{{0}, {1, 0}}));
    }

    @Test
    public void duplicateReferences() {
        assertArrayEquals(new int[]{0, 1}, DependencyLevels.compute(new int[][]{NONE, {0, 0, 0}}));
    }

    @Test
    public void largeCycleWithTail() {
        // 0 -> 1 -> ... -> 99 -> 0, and 100 -> 50
        int[][] referenced = new int[101][];
        for (int i = 0; i < 100; i++) {
            referenced[i] = new int[]{(i + 1) % 100};
        }
        referenced[100] = new int[]{50};
        int[] levels = DependencyLevels.compute(referenced);
        for (int i = 0; i < 100; i++) {
            assertEquals(0, levels[i], "cycle member " + i);
        }
        assertEquals(1, levels[100]);
    }

    @Test
    public void longChainDoesNotOverflowTheStack() {
        int n = 100_000;
        int[][] referenced = new int[n][];
        for (int i = 0; i < n - 1; i++) {
            referenced[i] = new int[]{i + 1};
        }
        referenced[n - 1] = NONE;
        int[] levels = DependencyLevels.compute(referenced);
        assertEquals(n - 1, levels[0]);
        assertEquals(0, levels[n - 1]);
    }

    @Test
    public void randomGraphsSatisfyTheDefinition() {
        Random random = new Random(3058);
        for (int round = 0; round < 200; round++) {
            int n = 1 + random.nextInt(40);
            int[][] referenced = new int[n][];
            for (int i = 0; i < n; i++) {
                int[] edges = new int[random.nextInt(4)];
                for (int k = 0; k < edges.length; k++) {
                    edges[k] = random.nextInt(n);
                }
                referenced[i] = edges;
            }
            int[] levels = DependencyLevels.compute(referenced);
            boolean[][] reach = reachability(referenced);
            for (int i = 0; i < n; i++) {
                // the definition: one above the deepest node referenced from outside the node's own cycle, where
                // the cycle is every node mutually reachable with it (just the node itself when it is in no cycle)
                int expected = 0;
                for (int j = 0; j < n; j++) {
                    if (!(reach[i][j] && reach[j][i])) {
                        continue;
                    }
                    assertEquals(levels[i], levels[j], "round " + round + " cycle " + i + "," + j);
                    for (int target : referenced[j]) {
                        if (!(reach[j][target] && reach[target][j])) {
                            expected = Math.max(expected, levels[target] + 1);
                        }
                    }
                }
                assertEquals(expected, levels[i], "round " + round + " node " + i);
            }
        }
    }

    private static boolean[][] reachability(int[][] referenced) {
        int n = referenced.length;
        boolean[][] reach = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            reach[i][i] = true;
            for (int target : referenced[i]) {
                reach[i][target] = true;
            }
        }
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    reach[i][j] |= reach[i][k] && reach[k][j];
                }
            }
        }
        return reach;
    }
}
