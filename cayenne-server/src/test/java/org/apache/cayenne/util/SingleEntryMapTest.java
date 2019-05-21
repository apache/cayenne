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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class SingleEntryMapTest {

    private SingleEntryMap<String, Integer> map;

    @Before
    public void createMap() {
        map = new SingleEntryMap<>("test");
    }

    @Test
    public void constructor() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("test"));
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());

        assertEquals("test", map.getKey());
        assertNull(map.getValue());
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullKey() {
        new SingleEntryMap<>(null);
    }

    @Test
    public void constructorWithValue() {
        Map<String, Integer> mapWithValue = new SingleEntryMap<>("test", 123);
        assertEquals(1, mapWithValue.size());
        assertEquals(123, (int)mapWithValue.get("test"));

        mapWithValue.put("test", null);
        assertNull(mapWithValue.get("test"));

        mapWithValue.put("test", 321);
        assertEquals(321, (int)mapWithValue.get("test"));
    }

    @Test
    public void constructorWithNullValue() {
        Map<String, Integer> mapWithValue = new SingleEntryMap<>("test", null);
        assertEquals(0, mapWithValue.size());
        assertNull(mapWithValue.get("test"));

        mapWithValue.put("test", 321);
        assertEquals(321, (int)mapWithValue.get("test"));
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullKeyAndValue() {
        new SingleEntryMap<>(null, 123);
    }

    @Test
    public void entrySet() {
        assertTrue(map.entrySet().isEmpty());

        map.setValue(123);
        assertEquals(1, map.entrySet().size());
        assertEquals("test", map.entrySet().iterator().next().getKey());
        assertEquals(123, (int)map.entrySet().iterator().next().getValue());
        assertEquals(1, map.entrySet().size());
    }

    @Test
    public void containsKey() {
        assertFalse(map.containsKey("test"));
        assertFalse(map.containsKey("test1"));

        map.put("test", 123);
        assertTrue(map.containsKey("test"));
        assertFalse(map.containsKey("test1"));

        map.put("test", null);
        assertFalse(map.containsKey("test"));
        assertFalse(map.containsKey("test1"));
    }

    @Test
    public void size() {
        assertEquals(0, map.size());

        map.put("test", 123);
        assertEquals(1, map.size());

        map.put("test", null);
        assertEquals(0, map.size());
    }

    @Test
    public void isEmpty() {
        assertTrue(map.isEmpty());

        map.put("test", 123);
        assertFalse(map.isEmpty());

        map.put("test", null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void containsValue() {
        assertFalse(map.containsValue(123));

        map.put("test", 123);
        assertTrue(map.containsValue(123));

        map.put("test", null);
        assertFalse(map.containsValue(123));
    }

    @Test
    public void get() {
        assertNull(map.get("test"));
        assertNull(map.get("test2"));

        map.put("test", 123);
        assertEquals(123, (int)map.get("test"));
        assertNull(map.get("test2"));

        map.put("test", null);
        assertNull(map.get("test"));
        assertNull(map.get("test2"));
    }

    @Test
    public void put() {
        assertNull(map.put("test", 123));
        assertEquals(123, (int)map.put("test", 321));
        assertEquals(321, (int)map.put("test", null));
        assertNull(map.put("test", 123));
    }

    @Test(expected = IllegalArgumentException.class)
    public void putWrongKey() {
        map.put("test2", 321);
    }

    @Test
    public void remove() {
        assertNull(map.remove("test"));
        assertNull(map.remove("test2"));

        map.put("test", 123);
        assertEquals(123, (int)map.remove("test"));

        assertNull(map.remove("test"));
    }

    @Test
    public void putAll() {
        assertNull(map.get("test"));
        assertNull(map.get("test2"));

        Map<String, Integer> map2 = Collections.singletonMap("test", 123);

        map.putAll(map2);
        assertEquals(123, (int)map.get("test"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void putAllWrongKey() {
        assertNull(map.get("test"));
        assertNull(map.get("test2"));

        Map<String, Integer> map2 = Collections.singletonMap("test2", 123);
        map.putAll(map2);
    }

    @Test
    public void clear() {
        assertEquals(0, map.size());

        map.clear();
        assertEquals(0, map.size());

        map.put("test", 123);
        assertEquals(1, map.size());

        map.clear();
        assertEquals(0, map.size());
        assertNull(map.get("test"));
    }

    @Test
    public void keySet() {
        assertTrue(map.keySet().isEmpty());

        map.put("test", 123);
        assertEquals(1, map.keySet().size());
        assertEquals("test", map.keySet().iterator().next());
        assertEquals("test", map.keySet().iterator().next());

        map.clear();
        assertTrue(map.keySet().isEmpty());
    }

    @Test
    public void values() {
        assertTrue(map.values().isEmpty());

        map.put("test", 123);
        assertEquals(1, map.keySet().size());
        assertEquals(123, (int)map.values().iterator().next());
        assertEquals(123, (int)map.values().iterator().next());

        map.clear();
        assertTrue(map.values().isEmpty());
    }

    @Test
    public void getKey() {
        assertEquals("test", map.getKey());

        map.put("test", 123);
        assertEquals("test", map.getKey());
    }

    @Test
    public void getValue() {
        assertNull(map.getValue());

        map.put("test", 123);
        assertEquals(123, (int)map.getValue());

        map.put("test", null);
        assertNull(map.getValue());

        map.put("test", 321);
        assertEquals(321, (int)map.getValue());
    }

    @Test
    public void setValue() {
        assertNull(map.getValue());

        map.setValue(123);
        assertEquals(123, (int)map.getValue());

        map.setValue(null);
        assertNull(map.getValue());

        map.setValue(321);
        assertEquals(321, (int)map.getValue());
    }

    @Test
    public void testEquals() {
        assertEquals(map, emptyMap());
        assertNotEquals(map, singletonMap("test", null));

        map.put("test", 123);
        assertEquals(map, singletonMap("test", 123));
        assertNotEquals(map, singletonMap("test", null));
        assertNotEquals(map, singletonMap("test2", 123));
        assertNotEquals(map, singletonMap("test", 124));

        map.put("test", 321);
        Map<String, Integer> other = new HashMap<>();
        other.put("test", 321);
        assertEquals(map, other);

        assertEquals(map, map);
        assertNotEquals(map, new ArrayList<>());
    }

    @Test
    public void testHashCode() {
        assertEquals(emptyMap().hashCode(), map.hashCode());
        assertEquals(map.hashCode(), map.hashCode());
        assertNotEquals(singletonMap("test", null).hashCode(), map.hashCode());

        map.put("test", 123);
        assertEquals(singletonMap("test", 123).hashCode(), map.hashCode());
        assertNotEquals(singletonMap("test", null).hashCode(), map.hashCode());
        assertNotEquals(singletonMap("test2", 123).hashCode(), map.hashCode());
        assertNotEquals(singletonMap("test", 124).hashCode(), map.hashCode());
        assertEquals(map.hashCode(), map.hashCode());

        map.put("test", 321);
        Map<String, Integer> other = new HashMap<>();
        other.put("test", 321);
        assertEquals(other.hashCode(), map.hashCode());
        assertEquals(map.hashCode(), map.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals(emptyMap().toString(), map.toString());
        assertNotEquals(singletonMap("test", null).toString(), map.toString());

        map.put("test", 123);
        assertEquals(singletonMap("test", 123).toString(), map.toString());
        assertNotEquals(singletonMap("test", null).toString(), map.toString());
        assertNotEquals(singletonMap("test2", 123).toString(), map.toString());
        assertNotEquals(singletonMap("test", 124).toString(), map.toString());

        map.put("test", 321);
        Map<String, Integer> other = new HashMap<>();
        other.put("test", 321);
        assertEquals(other.toString(), map.toString());
    }

    @Test
    public void forEach() {
        map.forEach((k, v) -> fail("Unexpected value in map: " + k + "=" + v));

        map.put("test", 123);

        AtomicInteger counter = new AtomicInteger();
        map.forEach((k, v) -> {
            assertEquals("test", k);
            assertEquals(123, (int)v);
            counter.incrementAndGet();
        });

        assertEquals(1, counter.get());
    }

    @Test
    public void getOrDefault() {
        assertEquals(321, (int)map.getOrDefault("test", 321));
        assertEquals(321, (int)map.getOrDefault("test2", 321));

        map.put("test", 123);
        assertEquals(123, (int)map.getOrDefault("test", 321));
        assertEquals(321, (int)map.getOrDefault("test2", 321));
    }

    @Test
    public void putIfAbsent() {
        assertNull(map.putIfAbsent("test", 123));
        assertEquals(123, (int)map.putIfAbsent("test", 321));
        assertEquals(123, (int)map.putIfAbsent("test", 456));
    }

    @Test(expected = IllegalArgumentException.class)
    public void putIfAbsentWrongKey() {
        map.putIfAbsent("test2", 321);
    }

    @Test
    public void computeIfAbsent() {
        assertEquals(123, (int)map.computeIfAbsent("test", k -> 123));
        assertEquals(123, (int)map.computeIfAbsent("test", k -> 321));
        assertEquals(123, (int)map.computeIfAbsent("test", k -> 456));
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeIfAbsentWrongKey() {
        map.computeIfAbsent("test2", k -> 123);
    }

    @Test
    public void computeIfPresent() {
        assertNull(map.computeIfPresent("test", (k, v) -> v + 1));

        map.put("test", 123);
        assertEquals(Integer.valueOf(124), map.computeIfPresent("test", (k, v) -> v + 1));
        assertNull(map.computeIfPresent("test2", (k, v) -> v + 1));
        assertNull(map.computeIfPresent("test3", (k, v) -> 321));
    }

    @Test
    public void compute() {
        assertEquals(123, (int)map.compute("test", (k, v) -> v == null ? 123 : v + 1));
        assertEquals(124, (int)map.compute("test", (k, v) -> v == null ? 123 : v + 1));
        assertEquals(125, (int)map.compute("test", (k, v) -> v == null ? 123 : v + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeWrongKey() {
        map.compute("test2", (k, v) -> 123);
    }

    @Test
    public void merge() {
        assertEquals(1, (int)map.merge("test", 1, Integer::sum));
        assertEquals(2, (int)map.merge("test", 1, Integer::sum));
        assertEquals(3, (int)map.merge("test", 1, Integer::sum));
    }

    @Test(expected = IllegalArgumentException.class)
    public void mergeWrongKey() {
        map.merge("test2", 123, (oldV, newV) -> oldV + newV);
    }

    @Test
    public void replace() {
        assertNull(map.replace("test", 123));
        assertNull(map.replace("test", 321));
        assertNull(map.replace("test2", 123));

        map.put("test", 123);
        assertEquals(123, (int)map.replace("test", 321));
        assertEquals(321, (int)map.replace("test", 123));
        assertNull(map.replace("test2", 123));
    }

    @Test
    public void replaceWithValue() {
        assertFalse(map.replace("test", 321, 123));
        assertFalse(map.replace("test", null, 123));
        assertFalse(map.replace("test2", null, 123));

        map.put("test", 123);

        assertTrue(map.replace("test", 123, 321));
        assertTrue(map.replace("test", 321, 456));
        assertFalse(map.replace("test", 321, 456));
        assertFalse(map.replace("test", null, 123));
        assertFalse(map.replace("test2", null, 123));
        assertFalse(map.replace("test2", 456, 123));
    }

    @Test
    public void removeWithValue() {
        assertFalse(map.remove("test", null));
        assertFalse(map.remove("test", 123));
        assertFalse(map.remove("test2", null));
        assertFalse(map.remove("test2", 123));

        map.put("test", 123);

        assertFalse(map.remove("test", null));
        assertFalse(map.remove("test", 321));
        assertFalse(map.remove("test2", null));
        assertFalse(map.remove("test2", 123));

        assertTrue(map.remove("test", 123));
        assertFalse(map.remove("test", 123));
    }
}