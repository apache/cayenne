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
 * Converts between double and byte[]
 *
 * @since 4.0
 */
public class DoubleConverter implements BytesConverter<Double> {

    public static final BytesConverter<Double> INSTANCE = new DoubleConverter();
    private static final int BYTES = 8;

    static double getDouble(byte[] bytes) {

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single double value: " + bytes.length);
        }

        return Double.longBitsToDouble(LongConverter.getLong(bytes));
    }

    static byte[] getBytes(Double d) {
        return LongConverter.getBytes(Double.doubleToLongBits(d));
    }

    @Override
    public Double fromBytes(byte[] bytes) {
        return getDouble(bytes);
    }

    @Override
    public byte[] toBytes(Double value) {
        return getBytes(value);
    }
}
