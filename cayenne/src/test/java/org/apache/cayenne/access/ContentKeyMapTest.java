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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentKeyMapTest {

    @Test
    public void byteArrayKeys() {
        byte[] key1 = {1, 2, 3};
        Map<Object, String> map = new ContentKeyMap<>();
        assertNull(map.put(key1, "a"));
        assertNull(map.put(new byte[]{4, 5}, "b"));

        assertEquals("a", map.get(new byte[]{1, 2, 3}));
        assertTrue(map.containsKey(new byte[]{4, 5}));
        assertFalse(map.containsKey(new byte[]{1, 2}));
        assertNull(map.get(new byte[]{1, 2}));

        // replaces the value, keeps the original key
        assertEquals("a", map.put(new byte[]{1, 2, 3}, "c"));
        assertEquals(2, map.size());
        assertTrue(map.keySet().stream().anyMatch(k -> k == key1));
        assertEquals("c", map.get(key1));

        assertEquals("b", map.remove(new byte[]{4, 5}));
        assertEquals(1, map.size());
        assertFalse(map.containsKey(new byte[]{4, 5}));
    }

    @Test
    public void iteratorRemove() {
        Map<Object, String> map = new ContentKeyMap<>(2);
        map.put(new byte[]{1}, "a");
        map.put(new byte[]{2}, "b");

        Iterator<Map.Entry<Object, String>> it = map.entrySet().iterator();
        Map.Entry<Object, String> first = it.next();
        it.remove();

        assertEquals(1, map.size());
        assertFalse(map.containsKey(first.getKey()));
    }

    @Test
    public void entrySetValue() {
        Map<Object, String> map = new ContentKeyMap<>();
        map.put(new byte[]{1}, "a");

        map.entrySet().iterator().next().setValue("b");
        assertEquals("b", map.get(new byte[]{1}));
    }

    @Test
    public void sameAsHashMapForRegularKeys() {
        Map<Object, String> map = new ContentKeyMap<>();
        Map<Object, String> expected = new HashMap<>();
        for (Map<Object, String> m : List.of(map, expected)) {
            m.put("x", "1");
            m.put(5, "2");
            m.put(null, "3");
            m.put("x", "4");
            m.remove(5);
        }

        assertEquals(expected, map);
        assertEquals(map, expected);
        assertEquals(expected.hashCode(), map.hashCode());
        assertEquals("4", map.get("x"));
        assertEquals("3", map.get(null));
        assertTrue(map.containsKey(null));
        assertSame(null, map.get(5));
    }

    @Test
    public void equalsAndHashCodeByContent() {
        Map<Object, String> m1 = new ContentKeyMap<>();
        m1.put(new byte[]{1, 2}, "a");
        Map<Object, String> m2 = new ContentKeyMap<>();
        m2.put(new byte[]{1, 2}, "a");

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}
