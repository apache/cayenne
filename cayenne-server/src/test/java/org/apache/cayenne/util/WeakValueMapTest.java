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

package org.apache.cayenne.util;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * As WeakValueMap and SoftValueMap share almost all code from their super class
 * only one test is present for both of them.
 *
 * @since 4.1
 */
public class WeakValueMapTest {

    @Test
    public void testEmptyConstructor() {
        Map<String, Integer> map = new WeakValueMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("nonexistent_key1"));
        assertFalse(map.containsValue(42));
        assertNull(map.get("nonexistent_key2"));
        assertEquals(new Integer(42), map.getOrDefault("nonexistent_key2", 42));

        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    public void testCapacityConstructor() {
        Map<String, Integer> map = new WeakValueMap<>(42);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("nonexistent_key1"));
        assertFalse(map.containsValue(42));
        assertNull(map.get("nonexistent_key2"));
        assertEquals(new Integer(42), map.getOrDefault("nonexistent_key2", 42));

        assertEquals(0, map.values().size());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.entrySet().size());
    }

    @Test
    public void testMapConstructor() {
        Map<String, Integer> data = new HashMap<>();
        data.put("key_1", 123);
        data.put("key_2", 42);
        data.put("key_3", null);

        Map<String, Integer> map = new WeakValueMap<>(data);

        assertFalse(map.isEmpty());
        assertEquals(data.size(), map.size());
        assertFalse(map.containsKey("nonexistent_key1"));
        assertTrue(map.containsKey("key_3"));
        assertFalse(map.containsValue(321));
        assertTrue(map.containsValue(42));
        assertNull(map.get("nonexistent_key2"));
        assertNull(map.get("key_3"));
        assertEquals(new Integer(123), map.getOrDefault("key_1", 42));

        assertEquals(data.size(), map.values().size());
        assertEquals(data.size(), map.keySet().size());
        assertEquals(data.size(), map.entrySet().size());

        assertTrue(map.values().containsAll(data.values()));
        assertTrue(map.keySet().containsAll(data.keySet()));
        assertTrue(map.entrySet().containsAll(data.entrySet()));
    }

    @Test
    public void testSimpleOperations() {
        Map<String, Integer> data = new HashMap<>();
        data.put("key_1", 123);
        data.put("key_2", 42);
        data.put("key_3", null);

        Map<String, Integer> map = new WeakValueMap<>(data);

        map.put("key_4", 44);
        assertEquals(new Integer(44), map.get("key_4"));
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
    public void testEntrySetUpdateValue() {
        Map<String, Integer> map = new WeakValueMap<>();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", null);
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
        assertEquals(new Integer(24), map.get("key_2"));
    }

    @Test
    public void testSerializationSupport() throws Exception {
        WeakValueMap<String, Integer> map = new WeakValueMap<>();
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", null);
        assertEquals(3, map.size());

        WeakValueMap<String, Integer> clone = Util.cloneViaSerialization(map);

        assertEquals(3, clone.size());
        assertEquals(new Integer(42), clone.get("key_2"));
        assertTrue(clone.containsKey("key_3"));
        assertTrue(clone.containsValue(123));
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        Map<String, Integer> map1 = new WeakValueMap<>();
        map1.put("key_1", 123);
        map1.put("key_2", 42);
        map1.put("key_3", null);
        assertEquals(3, map1.size());

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("key_1", 123);
        map2.put("key_2", 42);
        map2.put("key_3", null);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testConcurrentModification() {
        Map<String, Integer> map = new WeakValueMap<>(3);
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", null);
        assertEquals(3, map.size());

        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            if("key_2".equals(entry.getKey())) {
                map.remove("key_2");
            }
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnsupportedEntryIteratorRemoval() {
        Map<String, Integer> map = new WeakValueMap<>(3);
        map.put("key_1", 123);
        map.put("key_2", 42);
        map.put("key_3", null);
        assertEquals(3, map.size());

        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()) {
            iterator.remove();
        }
    }
}