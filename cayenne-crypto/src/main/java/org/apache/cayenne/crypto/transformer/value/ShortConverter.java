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
 * @since 4.0
 */
public class ShortConverter implements BytesConverter<Short> {

    public static final BytesConverter<Short> INSTANCE = new ShortConverter();
    private static final int BYTES = 2;

    static short getShort(byte[] bytes) {

        if (bytes.length < BYTES) {
            return ByteConverter.getByte(bytes);
        }

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single short value: " + bytes.length);
        }

        return (short) ((bytes[0] & 0xFF) << 8 | (bytes[1] & 0xFF));
    }

    static byte[] getBytes(short k) {

        if (k >= Byte.MIN_VALUE && k <= Byte.MAX_VALUE) {
            return ByteConverter.getBytes((byte) k);
        }

        return new byte[]{
                (byte) (k >> 8),
                (byte) k};
    }

    @Override
    public Short fromBytes(byte[] bytes) {
        return getShort(bytes);
    }

    @Override
    public byte[] toBytes(Short value) {
        return getBytes(value);
    }
}
