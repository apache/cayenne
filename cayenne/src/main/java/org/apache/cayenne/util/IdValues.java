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

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Equality, hashing and printing of raw PK, FK and row values, that may be arrays (e.g. a binary key is a byte[],
 * which Java compares by identity). "id" methods compare key values like {@link org.apache.cayenne.ObjectId}:
 * numbers by long value, arrays by content. "value" methods compare arrays by content and the rest with equals().
 * "key" methods return hash keys consistent with the corresponding equality. Internal API.
 *
 * @since 5.0
 */
public final class IdValues {

    private static final int MAX_PRINTED_BYTES = 32;
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

    private IdValues() {
    }

    /**
     * Compares key values: numbers by long value, arrays by content.
     */
    public static boolean idEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }

        if (a instanceof Number n1) {
            return b instanceof Number n2 && n1.longValue() == n2.longValue();
        }

        return valueEquals(a, b);
    }

    /**
     * Hash code consistent with {@link #idEquals(Object, Object)}.
     */
    public static int idHashCode(Object value) {
        return value instanceof Number n ? Long.hashCode(n.longValue()) : valueHashCode(value);
    }

    /**
     * Hash key consistent with {@link #idEquals(Object, Object)}.
     */
    public static Object idKey(Object value) {
        if (value instanceof Number n) {
            return n instanceof Long ? n : Long.valueOf(n.longValue());
        }
        return valueKey(value);
    }

    /**
     * Hash key for a compound key, consistent with {@link #idEquals(Object, Object)} per position. Copies the array
     * only if it has non-Long numbers.
     */
    public static Object idTupleKey(Object[] values) {
        Object[] normalized = values;
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number n && !(n instanceof Long)) {
                if (normalized == values) {
                    normalized = values.clone();
                }
                normalized[i] = n.longValue();
            }
        }
        return new ArrayKey(normalized);
    }

    /**
     * Compares id snapshots per {@link #idEquals(Object, Object)}.
     */
    public static boolean idSnapshotEquals(Map<String, ?> a, Map<String, ?> b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null || a.size() != b.size()) {
            return false;
        }

        for (Map.Entry<String, ?> entry : a.entrySet()) {
            Object value = entry.getValue();
            Object other = b.get(entry.getKey());
            if (value == null) {
                if (other != null || !b.containsKey(entry.getKey())) {
                    return false;
                }
            } else if (!idEquals(value, other)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Hash code consistent with {@link #idSnapshotEquals(Map, Map)}.
     */
    public static int idSnapshotHashCode(Map<String, ?> snapshot) {
        int h = 0;
        for (Map.Entry<String, ?> entry : snapshot.entrySet()) {
            h += Objects.hashCode(entry.getKey()) ^ idHashCode(entry.getValue());
        }
        return h;
    }

    /**
     * Compares values, arrays by content.
     */
    public static boolean valueEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b) || a.getClass().isArray() && Objects.deepEquals(a, b);
    }

    /**
     * Hash code consistent with {@link #valueEquals(Object, Object)}.
     */
    public static int valueHashCode(Object value) {
        if (value == null || !value.getClass().isArray()) {
            return Objects.hashCode(value);
        }

        return switch (value) {
            case byte[] a -> Arrays.hashCode(a);
            case Object[] a -> Arrays.deepHashCode(a);
            case int[] a -> Arrays.hashCode(a);
            case long[] a -> Arrays.hashCode(a);
            case short[] a -> Arrays.hashCode(a);
            case char[] a -> Arrays.hashCode(a);
            case float[] a -> Arrays.hashCode(a);
            case double[] a -> Arrays.hashCode(a);
            case boolean[] a -> Arrays.hashCode(a);
            default -> value.hashCode();
        };
    }

    /**
     * Hash key consistent with {@link #valueEquals(Object, Object)}.
     */
    public static Object valueKey(Object value) {
        return value != null && value.getClass().isArray() ? new ArrayKey(value) : value;
    }

    /**
     * Hash key for a row, consistent with {@link #valueEquals(Object, Object)} per position. Doesn't copy the array.
     */
    public static Object valueTupleKey(Object[] values) {
        return new ArrayKey(values);
    }

    /**
     * Appends a value, printing a byte[] as hex (e.g. 01020AFF), truncated after 32 bytes.
     */
    public static StringBuilder append(StringBuilder buffer, Object value) {
        switch (value) {
            case byte[] bytes -> {
                appendHex(buffer, bytes, Math.min(bytes.length, MAX_PRINTED_BYTES));
                if (bytes.length > MAX_PRINTED_BYTES) {
                    buffer.append("...(").append(bytes.length).append(" bytes)");
                }
            }
            case Object[] values -> {
                buffer.append('[');
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        buffer.append(", ");
                    }
                    append(buffer, values[i]);
                }
                buffer.append(']');
            }
            case null, default -> buffer.append(value);
        }
        return buffer;
    }

    /**
     * Appends a key value, printing a byte[] as hex in full, so that equal ids print the same (ObjectId.toString is
     * used in query cache keys).
     */
    public static StringBuilder appendId(StringBuilder buffer, Object value) {
        if (value instanceof byte[] bytes) {
            appendHex(buffer, bytes, bytes.length);
        } else {
            buffer.append(value);
        }
        return buffer;
    }

    private static void appendHex(StringBuilder buffer, byte[] bytes, int count) {
        HEX.formatHex(buffer, bytes, 0, count);
    }

    private record ArrayKey(Object array) {

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ArrayKey(Object arrayOther) && Objects.deepEquals(array, arrayOther);
        }

        @Override
        public int hashCode() {
            return valueHashCode(array);
        }

        @Override
        public String toString() {
            return append(new StringBuilder(), array).toString();
        }
    }
}
