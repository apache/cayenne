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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Converts between {@link java.math.BigDecimal} and byte[]
 *
 * @since 4.0
 */
public class BigDecimalConverter implements BytesConverter<BigDecimal> {

    public static final BytesConverter<BigDecimal> INSTANCE = new BigDecimalConverter();

    private static final int INTEGER_BYTES = Integer.SIZE / Byte.SIZE;
    private static final int MIN_BYTES = INTEGER_BYTES + 1;

    static BigDecimal getBigDecimal(byte[] bytes) {

        byte[] unscaledBytes, scaleBytes;

        if (bytes.length < MIN_BYTES) {
            throw new IllegalArgumentException("byte[] is too small for a BigDecimal value: " + bytes.length);
        }

        scaleBytes = Arrays.copyOfRange(bytes, 0, INTEGER_BYTES);
        unscaledBytes = Arrays.copyOfRange(bytes, INTEGER_BYTES, bytes.length);

        return new BigDecimal(new BigInteger(unscaledBytes), IntegerConverter.getInt(scaleBytes));
    }

    static byte[] getBytes(BigDecimal bigDecimal) {

        byte[] result, unscaledBytes, scaleBytes;

        unscaledBytes = bigDecimal.unscaledValue().toByteArray();
        scaleBytes = IntegerConverter.getBytes(bigDecimal.scale());

        result = new byte[INTEGER_BYTES + unscaledBytes.length];
        System.arraycopy(scaleBytes, 0, result, INTEGER_BYTES - scaleBytes.length, scaleBytes.length);
        System.arraycopy(unscaledBytes, 0, result, INTEGER_BYTES, unscaledBytes.length);

        return result;
    }

    @Override
    public BigDecimal fromBytes(byte[] bytes) {
        return getBigDecimal(bytes);
    }

    @Override
    public byte[] toBytes(BigDecimal value) {
        return getBytes(value);
    }
}
