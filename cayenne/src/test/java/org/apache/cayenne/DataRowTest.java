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

package org.apache.cayenne;

import org.apache.cayenne.access.SnapshotVersion;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataRowTest {

    private static final RowLayout LAYOUT = new RowLayout(new String[]{"a", "b", "c"});

    private static DataRow arrayRow(Object... values) {
        return DataRow.of("E", LAYOUT, values, SnapshotVersion.next());
    }

    @Test
    public void version() throws Exception {
        DataRow s1 = DataRow.of("E", new HashMap<>(10), SnapshotVersion.next());
        DataRow s2 = DataRow.of("E", new HashMap<>(10), SnapshotVersion.next());
        DataRow s3 = DataRow.of("E", new HashMap<>(10), SnapshotVersion.next());

        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    @Test
    public void explicitVersion() {
        long version = SnapshotVersion.next();
        DataRow s1 = DataRow.of("E", Map.of("a", 1), version);
        DataRow s2 = DataRow.of("E2", new HashMap<>(s1), version);

        assertEquals(version, s1.getVersion());
        assertEquals(version, s2.getVersion());
        assertEquals("E", s1.getEntityName());
        assertEquals("E2", s2.getEntityName());
        assertEquals(1, s2.get("a"));
    }

    @Test
    public void immutable() {
        DataRow map = DataRow.of("E", new HashMap<>(Map.of("a", 1)), 1);
        DataRow array = arrayRow(1, 2, 3);

        for (DataRow row : List.of(map, array)) {
            assertThrows(UnsupportedOperationException.class, () -> row.put("a", 2));
            assertThrows(UnsupportedOperationException.class, () -> row.putAll(Map.of("a", 2)));
            assertThrows(UnsupportedOperationException.class, () -> row.remove("a"));
            assertThrows(UnsupportedOperationException.class, row::clear);
        }

        // the views of an array row are read-only. The views of a map row are those of the map, not wrapped
        assertThrows(UnsupportedOperationException.class, () -> array.keySet().remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> array.entrySet().iterator().next().setValue(5));
    }

    @Test
    public void arrayRow_MapView() {
        DataRow row = arrayRow(1, null, "x");

        assertSame(LAYOUT, row.getLayout());
        assertEquals(3, row.size());
        assertFalse(row.isEmpty());
        assertEquals(1, row.get("a"));
        assertNull(row.get("b"));
        assertNull(row.get("d"));
        assertNull(row.get(5));
        assertTrue(row.containsKey("b"));
        assertFalse(row.containsKey("d"));
        assertTrue(row.containsValue(null));
        assertTrue(row.containsValue("x"));
        assertFalse(row.containsValue("y"));
        assertEquals("x", row.valueAt(2));
        assertEquals(7, row.getOrDefault("d", 7));
        assertNull(row.getOrDefault("b", 7));

        assertEquals(List.of("a", "b", "c"), List.copyOf(row.keySet()));
        assertEquals(3, row.values().size());
        assertEquals(3, row.entrySet().size());

        Map<String, Object> copy = new HashMap<>(row);
        assertEquals(3, copy.size());
        assertNull(copy.get("b"));
        assertTrue(copy.containsKey("b"));
    }

    @Test
    public void mapRow_NoLayout() {
        DataRow row = DataRow.of("E", Map.of("a", 1), 1);
        assertNull(row.getLayout());
        assertThrows(UnsupportedOperationException.class, () -> row.valueAt(0));
    }

    @Test
    public void equalsAndHashCode() {
        DataRow array = arrayRow(1, null, "x");
        DataRow sameLayout = arrayRow(1, null, "x");
        DataRow otherLayout = DataRow.of("E", new RowLayout(new String[]{"c", "a", "b"}), new Object[]{"x", 1, null}, 1);
        DataRow map = DataRow.of("E", new HashMap<>(Map.of("a", 1, "c", "x")), 1);
        HashMap<String, Object> plainMap = new HashMap<>(Map.of("a", 1, "c", "x"));
        plainMap.put("b", null);

        assertEquals(array, sameLayout);
        assertEquals(array, otherLayout);
        assertEquals(otherLayout, array);
        assertEquals(array, plainMap);
        assertEquals(plainMap, array);
        assertEquals(array.hashCode(), plainMap.hashCode());
        assertEquals(array.hashCode(), otherLayout.hashCode());

        // "b" is absent in the map row, null in the array row
        assertNotEquals(array, map);
        assertNotEquals(map, array);

        assertNotEquals(array, arrayRow(1, null, "y"));
        assertNotEquals(array, arrayRow(1, 2, "x"));
        assertNotEquals(array, DataRow.of("E", Map.of("a", 1), 1));
    }

    @Test
    public void merged() {
        DataRow array = arrayRow(1, 2, 3);
        DataRow merged = array.merged(Map.of("b", 20), 7);

        assertSame(LAYOUT, merged.getLayout());
        assertEquals(7, merged.getVersion());
        assertEquals("E", merged.getEntityName());
        assertEquals(20, merged.valueAt(1));
        assertEquals(1, merged.valueAt(0));
        assertEquals(2, array.valueAt(1), "the original must not change");

        DataRow widened = array.merged(Map.of("d", 4), 8);
        assertNull(widened.getLayout());
        assertEquals(4, widened.size());
        assertEquals(4, widened.get("d"));
        assertEquals(3, widened.get("c"));

        DataRow map = DataRow.of("E", new HashMap<>(Map.of("a", 1)), 1).merged(Map.of("b", 2), 9);
        assertNull(map.getLayout());
        assertEquals(9, map.getVersion());
        assertEquals(Map.of("a", 1, "b", 2), map);
    }

    @Test
    public void withEntityName() {
        DataRow array = arrayRow(1, 2, 3);
        DataRow renamed = array.withEntityName("E2");
        assertEquals("E2", renamed.getEntityName());
        assertEquals(array.getVersion(), renamed.getVersion());
        assertSame(LAYOUT, renamed.getLayout());
        assertEquals(array, renamed);

        DataRow map = DataRow.of("E", Map.of("a", 1), 1).withEntityName("E2");
        assertEquals("E2", map.getEntityName());
        assertEquals(1, map.get("a"));
    }

    @Test
    public void serialization() throws Exception {
        DataRow array = arrayRow(1, null, "x");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(array);
        }

        DataRow read;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            read = (DataRow) in.readObject();
        }

        assertNotNull(read);
        assertNull(read.getLayout(), "the layout is not serialized");
        assertEquals(array, read);
        assertEquals(array.getVersion(), read.getVersion());
        assertEquals("E", read.getEntityName());
    }

    @Test
    public void arrayRow_LengthMismatch() {
        assertThrows(IllegalArgumentException.class, () -> DataRow.of("E", LAYOUT, new Object[]{1}, 1));
    }
}
