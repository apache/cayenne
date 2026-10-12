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
package org.apache.cayenne.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClockMapTest {

    @Test
    public void putGet() {
        ClockMap<String, Object> m = new ClockMap<>(10);

        assertEquals(0, m.size());
        assertTrue(m.isEmpty());
        assertNull(m.put("k1", 100));
        assertEquals(1, m.size());
        assertNull(m.get("nosuchkey"));
        assertEquals(100, m.get("k1"));
        assertTrue(m.containsKey("k1"));

        assertEquals(100, m.put("k1", 101));
        assertEquals(101, m.get("k1"));
        assertEquals(101, m.remove("k1"));
        assertNull(m.get("k1"));
    }

    @Test
    public void boundedByMaxSize() {
        ClockMap<String, Object> m = new ClockMap<>(5);

        for (int i = 1; i <= 50; i++) {
            m.put("k" + i, i);
            assertEquals(Math.min(i, 5), m.size(), "after k" + i);
        }
    }

    @Test
    public void zeroSizeKeepsNothing() {
        ClockMap<String, Object> m = new ClockMap<>(0);
        m.put("k1", 1);
        assertEquals(0, m.size());
        assertNull(m.get("k1"));
    }

    @Test
    public void readEntrySurvivesEviction() {
        ClockMap<String, Object> m = new ClockMap<>(100);
        m.put("hot", 0);
        for (int i = 1; i < 100; i++) {
            m.put("k" + i, i);
        }

        // the hot entry is read before every insert, so the sweep always finds it referenced and skips it, while
        // the entries that are never read again are evicted in turn
        for (int i = 100; i < 5000; i++) {
            assertNotNull(m.get("hot"), "before k" + i);
            m.put("k" + i, i);
        }

        assertEquals(0, m.get("hot"));
        assertTrue(m.size() <= 100, "size " + m.size());
        assertNull(m.get("k1"), "an entry that was never read again is gone");
    }

    @Test
    public void mergeIsAtomicOnWrappedValues() {
        ClockMap<String, Object> m = new ClockMap<>(10);

        assertEquals(1, m.merge("k", 1, (a, b) -> b), "absent key takes the offered value");
        assertEquals(1, m.merge("k", 2, (a, b) -> a), "function keeps the current value");
        assertEquals(1, m.get("k"));
        assertEquals(2, m.merge("k", 2, (a, b) -> b), "function takes the offered value");
        assertEquals(2, m.get("k"));
        assertEquals(5, m.merge("k", 3, (a, b) -> (Integer) a + (Integer) b), "function computes a new value");
        assertNull(m.merge("k", 3, (a, b) -> null), "null removes the entry");
        assertFalse(m.containsKey("k"));
    }

    @Test
    public void conditionalOperations() {
        ClockMap<String, Object> m = new ClockMap<>(10);

        assertNull(m.putIfAbsent("k", 1));
        assertEquals(1, m.putIfAbsent("k", 2));
        assertEquals(1, m.get("k"));

        assertFalse(m.replace("k", 5, 3));
        assertTrue(m.replace("k", 1, 3));
        assertEquals(3, m.get("k"));
        assertEquals(3, m.replace("k", 4));
        assertNull(m.replace("missing", 4));

        assertFalse(m.remove("k", 1));
        assertTrue(m.remove("k", 4));
        assertFalse(m.containsKey("k"));
    }

    @Test
    public void viewsUnwrapValues() {
        ClockMap<String, Object> m = new ClockMap<>(10);
        m.put("a", 1);
        m.put("b", 2);

        Map<String, Object> copy = new HashMap<>(m);
        assertEquals(Map.of("a", 1, "b", 2), copy);
        assertEquals(Map.of("a", 1, "b", 2), m);
        assertTrue(m.containsValue(2));
        assertEquals(2, m.keySet().size());
        assertTrue(m.values().contains(1));

        m.entrySet().removeIf(e -> e.getKey().equals("a"));
        assertEquals(Map.of("b", 2), m);

        m.clear();
        assertTrue(m.isEmpty());
    }

    @Test
    public void concurrentInsertsStayBounded() throws Exception {
        int maxSize = 1000;
        int threads = 8;
        ClockMap<String, Object> m = new ClockMap<>(maxSize);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                String prefix = "t" + t + "-";
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < 50_000; i++) {
                        m.put(prefix + i, i);
                        m.get(prefix + (i / 2));
                    }
                }));
            }
            for (Future<?> f : futures) {
                f.get(1, TimeUnit.MINUTES);
            }
        } finally {
            pool.shutdownNow();
        }

        assertTrue(m.size() <= 2 * maxSize, "size " + m.size());

        // single-threaded inserts bring the map back to its limit, give or take the few entries that a budgeted
        // sweep may leave behind
        for (int i = 0; i < maxSize; i++) {
            m.put("tail-" + i, i);
        }
        assertTrue(m.size() <= maxSize + 32, "size " + m.size());

        Object sentinel = new Object();
        m.put("last", sentinel);
        assertSame(sentinel, m.get("last"));
    }
}
