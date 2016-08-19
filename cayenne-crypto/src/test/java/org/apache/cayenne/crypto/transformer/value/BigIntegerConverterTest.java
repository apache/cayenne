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

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class BigIntegerConverterTest {

    private BigInteger positiveInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE));
    private BigInteger negativeInt = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(Long.MIN_VALUE));

    @Test
    public void testConverter() {
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(positiveInt, converter.fromBytes(converter.toBytes(positiveInt)));
    }

    @Test
    public void testConverter_Negative() {
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(negativeInt, converter.fromBytes(converter.toBytes(negativeInt)));
    }

    @Test
    public void testConverter_Zero() {
        BigInteger originalValue = BigInteger.ZERO;
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }
}
