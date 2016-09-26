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
package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between long and byte[] using big-endian encoding.
 *
 * @since 4.0
 */
public class LongConverter implements BytesConverter<Long> {

    public static final BytesConverter INSTANCE = new LongConverter();
    private static final int BYTES = 8;

    static long getLong(byte[] bytes) {
        if (bytes.length < BYTES) {
            return IntegerConverter.getInt(bytes);
        }

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single long value: " + bytes.length);
        }

        return (bytes[0] & 0xFFL) << 56
                | (bytes[1] & 0xFFL) << 48
                | (bytes[2] & 0xFFL) << 40
                | (bytes[3] & 0xFFL) << 32
                | (bytes[4] & 0xFFL) << 24
                | (bytes[5] & 0xFFL) << 16
                | (bytes[6] & 0xFFL) << 8
                | (bytes[7] & 0xFFL);
    }

    static byte[] getBytes(long k) {

        if (k >= Integer.MIN_VALUE && k <= Integer.MAX_VALUE) {
            return IntegerConverter.getBytes((int) k);
        }

        return new byte[]{
                (byte) (k >> 56),
                (byte) (k >> 48),
                (byte) (k >> 40),
                (byte) (k >> 32),
                (byte) (k >> 24),
                (byte) (k >> 16),
                (byte) (k >> 8),
                (byte) k};
    }

    @Override
    public Long fromBytes(byte[] bytes) {
        return getLong(bytes);
    }

    @Override
    public byte[] toBytes(Long value) {
        return getBytes(value);
    }
}
