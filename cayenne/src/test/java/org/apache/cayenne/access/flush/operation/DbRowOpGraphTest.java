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

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.unit.CompiledTables;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> graph.topSort(ONE_GROUP));
        assertTrue(e.getMessage().startsWith("Cycle detected"));
    }
}
