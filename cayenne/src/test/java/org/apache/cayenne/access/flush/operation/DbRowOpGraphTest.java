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

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class DbRowOpGraphTest {

    private static List<DbRowOp> ops(int n) {
        return IntStream.range(0, n).mapToObj(i -> mock(DbRowOp.class)).toList();
    }

    @Test
    public void noEdges() {
        List<DbRowOp> ops = ops(3);
        DbRowOpGraph graph = new DbRowOpGraph(ops);
        for (int i = 0; i < ops.size(); i++) {
            graph.add(i);
        }
        assertEquals(ops, graph.topSort());
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

        List<DbRowOp> sorted = graph.topSort();
        assertEquals(4, sorted.size());
        assertTrue(sorted.indexOf(ops.get(2)) < sorted.indexOf(ops.get(1)));
        assertTrue(sorted.indexOf(ops.get(1)) < sorted.indexOf(ops.get(0)));
        assertTrue(sorted.indexOf(ops.get(1)) < sorted.indexOf(ops.get(3)));
    }

    @Test
    public void cycle() {
        List<DbRowOp> ops = ops(3);
        DbRowOpGraph graph = new DbRowOpGraph(ops);
        graph.add(0);
        graph.add(1, 2);
        graph.add(2, 1);

        IllegalStateException e = assertThrows(IllegalStateException.class, graph::topSort);
        assertTrue(e.getMessage().startsWith("Cycle detected"));
    }
}
