/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.di.spi;

import org.apache.cayenne.di.DIRuntimeException;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class DIGraphTest {

    @Test
    public void testTopSortNoCycles() {
        DIGraph<String> graph = new DIGraph<>();
        graph.add("x", "y");
        graph.add("x", "z");
        graph.add("z", "a");

        List<String> sorted = graph.topSort();
        assertEquals(asList("y", "a", "z", "x"), sorted);
    }

    @Test(expected = DIRuntimeException.class)
    public void testTopSortDirectCycle() {
        DIGraph<String> graph = new DIGraph<>();
        graph.add("x", "y");
        graph.add("y", "x");
        graph.topSort();
    }

    @Test(expected = DIRuntimeException.class)
    public void testTopSortInDirectCycle() {
        DIGraph<String> graph = new DIGraph<>();
        graph.add("x", "y");
        graph.add("y", "z");
        graph.add("z", "x");
        graph.topSort();
    }
}
