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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceMapTest {

    @Test
    public void emptyConstructor() {
        Map<String, Integer> map = ReferenceMap.weakValues();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("nonexistent_key1"));
        assertFalse(map.containsValue(42));
        assertNull(map.get("nonexistent_key2"));
        assertEquals(Integer.valueOf(42), map.getOrDefault("nonexistent_key2", 42));

        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    public void putAll() {
        Map<String, Integer> data = new HashMap<>();
        data.put("key_1", 123);
        data.put("key_2", 42);
        data.put("key_3", 543);

        Map<String, Integer> map = ReferenceMap.weakValues();
        map.putAll(data);

        assertFalse(map.isEmpty());
        assertEquals(data.size(), map.size());
        assertFalse(map.containsKey("nonexistent_key1"));
        assertTrue(map.containsKey("key_3"));
        assertFalse(map.containsValue(321));
        assertTrue(map.containsValue(42));
        assertNull(map.get("nonexistent_key2"));
        assertEquals(Integer.valueOf(543), map.get("key_3"));
        assertEquals(Integer.valueOf(123), map.getOrDefault("key_1", 42));

        assertEquals(data.size(), map.values().size());
        assertEquals(data.size(), map.keySet().size());
        assertEquals(data.size(), map.entrySet().size());

        assertTrue(map.values().containsAll(data.values()));
        assertTrue(map.keySet().containsAll(data.keySet()));
        assertTrue(map.entrySet().containsAll(data.entrySet()));
    }

    @Test
    public void simpleOperations() {
        Map<String, Integer> data = new HashMap<>();
        data.put("key_1", 123);
        data.put("key_2", 42);
        data.put("key_3", 543);

        Map<String, Integer> map = ReferenceMap.weakValues();
        map.putAll(data);

        map.put("key_4", 44);
        assertEquals(Integer.valueOf(44), map.get("key_4"));
        assertEquals(4, map.size());
        assertTrue(map.containsKey("key_4"));
        assertTrue(map.containsValue(44));

        int old = map.remove("key_4");
        assertEquals(44, old);
        assertEquals(3, map.size());
        assertFalse(map.containsKey("key_4"));
        assertFalse(map.containsValue(44));
    }

    @Test
    public void entrySetUpdateValue() {
        Map<String, Integer> map = ReferenceMap.weakValues();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", 543);
        assertEquals(3, map.size());

        int counter = 0;
        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            if("key_2".equals(entry.getKey())) {
                int old = entry.setValue(24);
                assertEquals(42, old);
            }
            counter++;
        }

        assertEquals(3, counter);
        assertEquals(Integer.valueOf(24), map.get("key_2"));
    }

    @Test
    public void equalsAndHashCode() throws Exception {
        Map<String, Integer> map1 = ReferenceMap.weakValues();
        map1.put("key_1", 123);
        map1.put("key_2", 42);
        map1.put("key_3", 543);
        assertEquals(3, map1.size());

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("key_1", 123);
        map2.put("key_2", 42);
        map2.put("key_3", 543);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void entrySetValue() {
        Map<String, Integer> map = ReferenceMap.weakValues();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", 543);
        assertEquals(3, map.size());

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            if("key_2".equals(entry.getKey())) {
                assertEquals(Integer.valueOf(42), entry.getValue());
                assertEquals(Integer.valueOf(42), entry.setValue(24));
                assertEquals(Integer.valueOf(24), entry.getValue());
            }
        }

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            if("key_2".equals(entry.getKey())) {
                assertEquals(Integer.valueOf(24), entry.getValue());
            }
        }

        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(24), map.get("key_2"));
    }

    @Test
    public void concurrentModification() {
        Map<String, Integer> map = ReferenceMap.weakValues();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", 543);
        map.put("key_4", 321);
        assertEquals(4, map.size());

        // the map is concurrent, so a removal during iteration is allowed
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if ("key_2".equals(entry.getKey())) {
                map.remove("key_2");
            }
        }

        assertEquals(3, map.size());
        assertFalse(map.containsKey("key_2"));
    }

    @Test
    public void entryIteratorRemoval() {
        Map<String, Integer> map = ReferenceMap.weakValues();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", 543);
        assertEquals(3, map.size());

        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        assertThrows(IllegalStateException.class, iterator::remove);

        while (iterator.hasNext()) {
            if (iterator.next().getValue() == 42) {
                iterator.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("key_2"));
        assertEquals(Integer.valueOf(123), map.get("key_1"));
        assertEquals(Integer.valueOf(543), map.get("key_3"));
    }

    @Test
    public void computeIfAbsent() {
        Map<String, Integer> map = ReferenceMap.weakValues();
        map.put("key_1", 123);

        assertEquals(Integer.valueOf(123), map.computeIfAbsent("key_1", k -> fail("Value is present")));
        assertEquals(Integer.valueOf(42), map.computeIfAbsent("key_2", k -> 42));
        assertEquals(Integer.valueOf(42), map.get("key_2"));

        assertNull(map.computeIfAbsent("key_3", k -> null));
        assertFalse(map.containsKey("key_3"));
        assertEquals(2, map.size());
    }

    @Test
    public void computeIfAbsentConcurrently() throws Exception {
        Map<String, Object> map = ReferenceMap.weakValues();

        // every thread computes the same key; all must get the same instance, and the function must run once
        int threads = 8;
        AtomicInteger computations = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return map.computeIfAbsent("key", k -> {
                        computations.incrementAndGet();
                        return new Object();
                    });
                }));
            }
            start.countDown();

            Object first = results.getFirst().get();
            for (Future<Object> result : results) {
                assertSame(first, result.get());
            }
            assertEquals(1, computations.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void softValues() {
        Map<String, Integer> map = ReferenceMap.softValues();
        map.put("key_1", 123);
        assertEquals(Integer.valueOf(123), map.get("key_1"));
        assertEquals(1, map.size());
    }

    @Test
    public void putNullValue() {
        Map<Object, Object> map = ReferenceMap.weakValues();
        assertThrows(NullPointerException.class, () -> map.put("1", null));
    }

    @Test
    public void putAllNullValue() {
        Map<Object, Object> values = new HashMap<>();
        values.put("123", null);

        Map<Object, Object> map = ReferenceMap.weakValues();
        assertThrows(NullPointerException.class, () -> map.putAll(values));
    }

}
