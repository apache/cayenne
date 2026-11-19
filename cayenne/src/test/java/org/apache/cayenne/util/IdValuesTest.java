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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdValuesTest {

    private static final UUID UUID1 = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    // equal values are distinct instances
    private static List<Object> values() {
        return Arrays.asList(
                1, 1L, (short) 1, new BigDecimal("1.5"), new BigDecimal("1.7"), 2, 2L,
                new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 4}, new byte[0],
                new int[]{1, 2}, new int[]{1, 2},
                new Object[]{new byte[]{1}, "a"}, new Object[]{new byte[]{1}, "a"},
                UUID1, UUID.fromString(UUID1.toString()),
                "a", new String("a"), "b",
                null);
    }

    @Test
    public void idEquals() {
        assertTrue(IdValues.idEquals(1, 1L));
        assertTrue(IdValues.idEquals((short) 1, 1L));
        assertTrue(IdValues.idEquals(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}));
        assertTrue(IdValues.idEquals(null, null));
        assertFalse(IdValues.idEquals(new byte[]{1, 2, 3}, new byte[]{1, 2, 4}));
        assertFalse(IdValues.idEquals(1, null));
        assertFalse(IdValues.idEquals(null, 1));
        assertFalse(IdValues.idEquals(1, "1"));
        assertFalse(IdValues.idEquals("1", 1));
    }

    @Test
    public void valueEquals() {
        assertTrue(IdValues.valueEquals(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}));
        assertTrue(IdValues.valueEquals(new Object[]{new byte[]{1}}, new Object[]{new byte[]{1}}));
        assertFalse(IdValues.valueEquals(1, 1L));
        assertFalse(IdValues.valueEquals(new BigDecimal("1.5"), new BigDecimal("1.7")));
    }

    @Test
    public void idKeyConsistentWithIdEquals() {
        for (Object a : values()) {
            for (Object b : values()) {
                boolean equal = IdValues.idEquals(a, b);
                assertEquals(equal, IdValues.idEquals(b, a), () -> "symmetry: " + a + ", " + b);
                assertEquals(equal, Objects.equals(IdValues.idKey(a), IdValues.idKey(b)),
                        () -> "idKey: " + a + ", " + b);
                if (equal) {
                    assertEquals(IdValues.idHashCode(a), IdValues.idHashCode(b), () -> "idHashCode: " + a + ", " + b);
                    assertEquals(Objects.hashCode(IdValues.idKey(a)), Objects.hashCode(IdValues.idKey(b)));
                }
            }
        }
    }

    @Test
    public void valueKeyConsistentWithValueEquals() {
        for (Object a : values()) {
            for (Object b : values()) {
                boolean equal = IdValues.valueEquals(a, b);
                assertEquals(equal, IdValues.valueEquals(b, a), () -> "symmetry: " + a + ", " + b);
                assertEquals(equal, Objects.equals(IdValues.valueKey(a), IdValues.valueKey(b)),
                        () -> "valueKey: " + a + ", " + b);
                if (equal) {
                    assertEquals(IdValues.valueHashCode(a), IdValues.valueHashCode(b),
                            () -> "valueHashCode: " + a + ", " + b);
                }
            }
        }
    }

    @Test
    public void tupleKeys() {
        Object[] t1 = {new byte[]{1, 2}, 5, null};
        Object[] t2 = {new byte[]{1, 2}, 5L, null};
        Object[] t3 = {new byte[]{1, 2}, 6, null};

        assertEquals(IdValues.idTupleKey(t1), IdValues.idTupleKey(t2));
        assertEquals(IdValues.idTupleKey(t1).hashCode(), IdValues.idTupleKey(t2).hashCode());
        assertFalse(IdValues.idTupleKey(t1).equals(IdValues.idTupleKey(t3)));

        assertEquals(IdValues.valueTupleKey(t1), IdValues.valueTupleKey(new Object[]{new byte[]{1, 2}, 5, null}));
        assertEquals(IdValues.valueTupleKey(t1).hashCode(),
                IdValues.valueTupleKey(new Object[]{new byte[]{1, 2}, 5, null}).hashCode());
        // no numeric conversion for values
        assertFalse(IdValues.valueTupleKey(t1).equals(IdValues.valueTupleKey(t2)));
    }

    @Test
    public void idSnapshots() {
        Map<String, Object> s1 = new HashMap<>();
        s1.put("A", new byte[]{1, 2});
        s1.put("B", 5);
        Map<String, Object> s2 = new HashMap<>();
        s2.put("A", new byte[]{1, 2});
        s2.put("B", 5L);
        Map<String, Object> s3 = Map.of("A", new byte[]{1, 2}, "C", 5);

        assertTrue(IdValues.idSnapshotEquals(s1, s2));
        assertEquals(IdValues.idSnapshotHashCode(s1), IdValues.idSnapshotHashCode(s2));
        assertFalse(IdValues.idSnapshotEquals(s1, s3));
        assertFalse(IdValues.idSnapshotEquals(s1, Map.of("A", new byte[]{1, 2})));

        Map<String, Object> withNull = new HashMap<>();
        withNull.put("A", null);
        assertTrue(IdValues.idSnapshotEquals(withNull, new HashMap<>(withNull)));
        assertFalse(IdValues.idSnapshotEquals(withNull, Map.of("B", 1)));

        // same as Map.hashCode() for non-arrays
        Map<String, Object> plain = Map.of("A", "x", "B", 7L);
        assertEquals(plain.hashCode(), IdValues.idSnapshotHashCode(plain));
    }

    @Test
    public void idKeyNull() {
        assertNull(IdValues.idKey(null));
        assertNull(IdValues.valueKey(null));
    }

    @Test
    public void append() {
        assertEquals("01020AFF", IdValues.append(new StringBuilder(), new byte[]{1, 2, 10, -1}).toString());
        assertEquals("[01, x, null]",
                IdValues.append(new StringBuilder(), new Object[]{new byte[]{1}, "x", null}).toString());
        assertEquals("null", IdValues.append(new StringBuilder(), null).toString());
        assertEquals("5", IdValues.append(new StringBuilder(), 5).toString());
        assertEquals("0102", IdValues.valueKey(new byte[]{1, 2}).toString());
    }

    @Test
    public void appendTruncatesLongArrays() {
        byte[] bytes = new byte[100];
        Arrays.fill(bytes, (byte) 1);

        assertEquals("01".repeat(32) + "...(100 bytes)", IdValues.append(new StringBuilder(), bytes).toString());
        assertEquals("01".repeat(32), IdValues.append(new StringBuilder(), Arrays.copyOf(bytes, 32)).toString());
    }

    @Test
    public void appendIdDoesNotTruncate() {
        byte[] bytes = new byte[100];
        Arrays.fill(bytes, (byte) 1);

        assertEquals("01".repeat(100), IdValues.appendId(new StringBuilder(), bytes).toString());
        assertEquals("5", IdValues.appendId(new StringBuilder(), 5).toString());
        assertEquals("null", IdValues.appendId(new StringBuilder(), null).toString());
    }

    @Test
    public void idTupleKeyDoesNotChangeInput() {
        Object[] tuple = {5, new byte[]{1}};
        IdValues.idTupleKey(tuple);
        assertEquals(5, tuple[0]);
    }
}
