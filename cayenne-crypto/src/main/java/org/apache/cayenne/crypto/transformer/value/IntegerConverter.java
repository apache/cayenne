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
package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between integer and byte[] using big-endian encoding.
 *
 * @since 4.0
 */
public class IntegerConverter implements BytesConverter<Integer> {

    public static final BytesConverter<Integer> INSTANCE = new IntegerConverter();
    private static final int BYTES = 4;

    static int getInt(byte[] bytes) {
        if (bytes.length < BYTES) {
            return ShortConverter.getShort(bytes);
        }

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single int value: " + bytes.length);
        }

        return (bytes[0] & 0xFF) << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    static byte[] getBytes(int k) {

        if (k >= Short.MIN_VALUE && k <= Short.MAX_VALUE) {
            return ShortConverter.getBytes((short) k);
        }

        return new byte[]{
                (byte) (k >> 24),
                (byte) (k >> 16),
                (byte) (k >> 8),
                (byte) k};
    }

    @Override
    public Integer fromBytes(byte[] bytes) {
        return getInt(bytes);
    }

    @Override
    public byte[] toBytes(Integer value) {
        return getBytes(value);
    }
}
