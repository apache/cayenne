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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbRowOpGraphTest {

    // all the ops in one group
    private static final Comparator<DbRowOp> ONE_GROUP = (o1, o2) -> 0;

    // a group per table, by table name
    private static final Comparator<DbRowOp> BY_TABLE = Comparator.comparing(op -> op.getEntity().name());

    private static List<DbRowOp> ops(int n) {
        return IntStream.range(0, n).mapToObj(i -> op("t", i)).toList();
    }

    private static DbRowOp op(String table, int id) {
        CompiledTable compiledTable = CompiledTables.table(table);
        ObjectId objectId = ObjectId.of(table, "id", id);
        return new InsertDbRowOp(TestObjects.modifiedObject(objectId), compiledTable, objectId);
    }

    private static DbRowOpGraph graph(List<DbRowOp> ops) {
        DbRowOpGraph graph = new DbRowOpGraph(ops);
        for (int i = 0; i < ops.size(); i++) {
            graph.add(i);
        }
        return graph;
    }

    @Test
    public void noEdges() {
        List<DbRowOp> ops = ops(3);
        assertEquals(ops, graph(ops).topSort(ONE_GROUP));
    }

    @Test
    public void dependenciesFirst() {
        List<DbRowOp> ops = ops(4);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 -> 1 -> 2, 3 -> 1; multi-edge 0 -> 1
        graph.add(0, 1);
        graph.add(0, 1);
        graph.add(1, 2);
        graph.add(3, 1);
        for (int i = 0; i < ops.size(); i++) {
            graph.add(i);
        }

        List<DbRowOp> sorted = graph.topSort(ONE_GROUP);
        assertEquals(4, sorted.size());
        assertTrue(sorted.indexOf(ops.get(2)) < sorted.indexOf(ops.get(1)));
        assertTrue(sorted.indexOf(ops.get(1)) < sorted.indexOf(ops.get(0)));
        assertTrue(sorted.indexOf(ops.get(1)) < sorted.indexOf(ops.get(3)));
    }

    @Test
    public void groupsTogether() {
        DbRowOp a0 = op("a", 0);
        DbRowOp b0 = op("b", 0);
        DbRowOp a1 = op("a", 1);
        DbRowOp b1 = op("b", 1);

        assertEquals(List.of(a0, a1, b0, b1), graph(List.of(b0, a0, b1, a1)).topSort(BY_TABLE));
    }

    @Test
    public void groupContinuesWithReadyDependents() {
        DbRowOp a0 = op("a", 0);
        DbRowOp b0 = op("b", 0);
        DbRowOp a1 = op("a", 1);

        // a1 depends on a0, and joins its group as soon as a0 is sorted, before the group of b0
        DbRowOpGraph graph = graph(List.of(a0, b0, a1));
        graph.add(2, 0);

        assertEquals(List.of(a0, a1, b0), graph.topSort(BY_TABLE));
    }

    @Test
    public void groupContinuesBeforeEarlierGroup() {
        DbRowOp b0 = op("b", 0);
        DbRowOp a0 = op("a", 0);
        DbRowOp b1 = op("b", 1);

        // a0 depends on b0, so the group of b is first, and stays first after a0 is ready, until it is done
        DbRowOpGraph graph = graph(List.of(b0, a0, b1));
        graph.add(1, 0);

        assertEquals(List.of(b0, b1, a0), graph.topSort(BY_TABLE));
    }

    @Test
    public void cycle() {
        List<DbRowOp> ops = ops(3);
        DbRowOpGraph graph = new DbRowOpGraph(ops);
        graph.add(0);
        graph.add(1, 2);
        graph.add(2, 1);

        // the cycle is broken at the first op in the list order
        assertEquals(ops, graph.topSort(ONE_GROUP));
    }

    @Test
    public void cycleBrokenByGroupOrder() {
        DbRowOp b0 = op("b", 0);
        DbRowOp a0 = op("a", 0);

        DbRowOpGraph graph = graph(List.of(b0, a0));
        graph.add(0, 1);
        graph.add(1, 0);

        // the group order wins over the list order
        assertEquals(List.of(a0, b0), graph.topSort(BY_TABLE));
    }

    @Test
    public void cycleDependentsWait() {
        List<DbRowOp> ops = ops(4);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 -> (1 <-> 2) -> 3: only an edge of the cycle is broken. 0 is the first in the list, but is not on the
        // cycle, so it still waits for the cycle, and the cycle for 3
        graph.add(0, 1);
        graph.add(0, 2);
        graph.add(1, 2);
        graph.add(2, 1);
        graph.add(2, 3);

        assertEquals(List.of(ops.get(3), ops.get(1), ops.get(2), ops.get(0)), graph.topSort(ONE_GROUP));
    }

    @Test
    public void twoCycles() {
        List<DbRowOp> ops = ops(4);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 <-> 1 -> 2 <-> 3: the cycle depending on nothing else is broken first, at 2, then the other one, at 0
        graph.add(0, 1);
        graph.add(1, 0);
        graph.add(1, 2);
        graph.add(2, 3);
        graph.add(3, 2);

        assertEquals(List.of(ops.get(2), ops.get(3), ops.get(0), ops.get(1)), graph.topSort(ONE_GROUP));
    }

    @Test
    public void brokenCycles() {
        List<DbRowOp> ops = ops(5);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 <-> 1 -> 2 <-> 3, 4 with no edges
        graph.add(0, 1);
        graph.add(1, 0);
        graph.add(1, 2);
        graph.add(2, 3);
        graph.add(3, 2);
        graph.add(4);

        graph.topSort(ONE_GROUP);
        List<int[]> cycles = graph.brokenCycles();
        assertEquals(2, cycles.size());
        assertEquals(List.of(2, 3), Arrays.stream(cycles.get(0)).sorted().boxed().toList());
        assertEquals(List.of(0, 1), Arrays.stream(cycles.get(1)).sorted().boxed().toList());
    }

    @Test
    public void noBrokenCycles() {
        List<DbRowOp> ops = ops(2);
        DbRowOpGraph graph = new DbRowOpGraph(ops);
        graph.add(0, 1);

        graph.topSort(ONE_GROUP);
        assertTrue(graph.brokenCycles().isEmpty());
    }

    @Test
    public void cycleDependingOnCycle() {
        List<DbRowOp> ops = ops(4);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 <-> 1, 0 -> 2, 2 <-> 3: 0 is the first in the list, but breaking its cycle first would sort it before 2,
        // breaking the edge 0 -> 2 that is on no cycle
        graph.add(0, 1);
        graph.add(1, 0);
        graph.add(0, 2);
        graph.add(2, 3);
        graph.add(3, 2);

        assertEquals(List.of(ops.get(2), ops.get(3), ops.get(0), ops.get(1)), graph.topSort(ONE_GROUP));
    }

    @Test
    public void cycleLeavingCycle() {
        List<DbRowOp> ops = ops(3);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // 0 <-> 1, 1 <-> 2, 2 -> 0: one component, still cyclic after 0 is released, and broken again at 1
        graph.add(0, 1);
        graph.add(1, 0);
        graph.add(1, 2);
        graph.add(2, 1);
        graph.add(2, 0);

        assertEquals(ops, graph.topSort(ONE_GROUP));
    }

    @Test
    public void longCycle() {
        int n = 100_000;
        List<DbRowOp> ops = ops(n);
        DbRowOpGraph graph = new DbRowOpGraph(ops);

        // a chain long enough to overflow the stack if the components were found recursively
        for (int i = 0; i < n; i++) {
            graph.add(i, (i + 1) % n);
        }

        List<DbRowOp> sorted = graph.topSort(ONE_GROUP);
        assertEquals(n, sorted.size());

        // broken at 0, then the chain from the end
        assertEquals(ops.get(0), sorted.get(0));
        assertEquals(ops.get(n - 1), sorted.get(1));
        assertEquals(ops.get(1), sorted.get(n - 1));
    }
}
