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

    private static final DataRowLayout ABC = DataRowLayout.of("a", "b", "c");

    private static DataRow rowOf(Object... values) {
        return DataRow.of("E", ABC, values, SnapshotVersion.next());
    }

    private static DataRow rowOf(DataRowLayout layout, Object... values) {
        return DataRow.of("E", layout, values, 1);
    }

    @Test
    public void version() {
        DataRow s1 = rowOf(1, 2, 3);
        DataRow s2 = rowOf(1, 2, 3);
        DataRow s3 = rowOf(1, 2, 3);

        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    @Test
    public void explicitVersion() {
        long version = SnapshotVersion.next();
        DataRow s1 = DataRow.of("E", ABC, new Object[]{1, 2, 3}, version);
        DataRow s2 = DataRow.of("E2", ABC, new Object[]{1, 2, 3}, version);

        assertEquals(version, s1.getVersion());
        assertEquals(version, s2.getVersion());
        assertEquals("E", s1.getEntityName());
        assertEquals("E2", s2.getEntityName());
        assertEquals(1, s2.get("a"));
    }

    @Test
    public void immutable() {
        DataRow row = rowOf(1, 2, 3);

        assertThrows(UnsupportedOperationException.class, () -> row.put("a", 2));
        assertThrows(UnsupportedOperationException.class, () -> row.putAll(Map.of("a", 2)));
        assertThrows(UnsupportedOperationException.class, () -> row.remove("a"));
        assertThrows(UnsupportedOperationException.class, row::clear);

        // the views are read-only too
        assertThrows(UnsupportedOperationException.class, () -> row.keySet().remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> row.entrySet().iterator().next().setValue(5));
    }

    @Test
    public void mapView() {
        DataRow row = rowOf(1, null, "x");

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
    public void equalsAndHashCode() {
        DataRow row = rowOf(1, null, "x");
        DataRow sameLayout = rowOf(1, null, "x");
        DataRow otherLayout = rowOf(DataRowLayout.of("c", "a", "b"), "x", 1, null);
        DataRow narrower = rowOf(DataRowLayout.of("a", "c"), 1, "x");
        HashMap<String, Object> plainMap = new HashMap<>(Map.of("a", 1, "c", "x"));
        plainMap.put("b", null);

        assertEquals(row, sameLayout);
        assertEquals(row, otherLayout);
        assertEquals(otherLayout, row);
        assertEquals(row, plainMap);
        assertEquals(plainMap, row);
        assertEquals(row.hashCode(), plainMap.hashCode());
        assertEquals(row.hashCode(), otherLayout.hashCode());

        // "b" is absent in the narrower row, null in the other
        assertNotEquals(row, narrower);
        assertNotEquals(narrower, row);

        assertNotEquals(row, rowOf(1, null, "y"));
        assertNotEquals(row, rowOf(1, 2, "x"));
        assertNotEquals(row, rowOf(DataRowLayout.of("a"), 1));
    }

    @Test
    public void merged() {
        DataRow row = rowOf(1, 2, 3);
        DataRow merged = row.merged(Map.of("b", 20), 7);

        assertEquals(7, merged.getVersion());
        assertEquals("E", merged.getEntityName());
        assertEquals(Map.of("a", 1, "b", 20, "c", 3), merged);
        assertSame(row.layout, merged.layout);
        assertEquals(2, row.get("b"), "the original must not change");

        DataRow widened = row.merged(Map.of("d", 4), 8);
        assertEquals(4, widened.size());
        assertEquals(4, widened.get("d"));
        assertEquals(3, widened.get("c"));
        assertEquals(List.of("a", "b", "c", "d"), List.copyOf(widened.keySet()));
    }

    @Test
    public void withEntityName() {
        DataRow row = rowOf(1, 2, 3);
        DataRow renamed = row.withEntityName("E2");
        assertEquals("E2", renamed.getEntityName());
        assertEquals(row.getVersion(), renamed.getVersion());
        assertEquals(row, renamed);
        assertSame(row.layout, renamed.layout);
    }

    @Test
    public void serialization() throws Exception {
        DataRow row1 = rowOf(1, null, "x");
        DataRow row2 = rowOf(2, null, "y");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(List.of(row1, row2));
        }

        List<?> read;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            read = (List<?>) in.readObject();
        }

        DataRow read1 = (DataRow) read.get(0);
        DataRow read2 = (DataRow) read.get(1);
        assertEquals(row1, read1);
        assertEquals(row2, read2);
        assertEquals(row1.getVersion(), read1.getVersion());
        assertEquals("E", read1.getEntityName());

        // the layout is read back once, with its index rebuilt
        assertSame(read1.layout, read2.layout);
        assertEquals(List.of("a", "b", "c"), List.copyOf(read1.keySet()));
        assertEquals("x", read1.get("c"));
        assertEquals("y", read1.layout.reader().get(read2, 2));
    }

    @Test
    public void lengthMismatch() {
        assertThrows(IllegalArgumentException.class, () -> DataRow.of("E", ABC, new Object[]{1}, 1));
    }
}
