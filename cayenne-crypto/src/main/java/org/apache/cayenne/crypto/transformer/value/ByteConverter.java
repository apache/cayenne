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
public class ByteConverter implements BytesConverter<Byte> {

    public static final BytesConverter<Byte> INSTANCE = new ByteConverter();
    private static final int BYTES = 1;

    static byte getByte(byte[] bytes) {
        if (bytes.length != BYTES) {
            throw new IllegalArgumentException("Unexpected number of bytes: " + bytes.length);
        }

        return bytes[0];
    }

    static byte[] getBytes(byte b) {
        return new byte[]{b};
    }

    @Override
    public Byte fromBytes(byte[] bytes) {
        return getByte(bytes);
    }

    @Override
    public byte[] toBytes(Byte value) {
        return getBytes(value);
    }
}
