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

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalDateTimeConverterTest {

    @Test
    public void testFromBytes() {
        assertEquals(LocalDateTime.of(2015, 1, 7, 11, 0, 2),
                LocalDateTimeConverter.INSTANCE.fromBytes(new byte[]{2, 64, 58, 0, 0, 36, 4, -113, 36, 116, 0}));
    }

    @Test
    public void testToBytes() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.of(2015, 1, 7, 11, 0, 2));
        assertArrayEquals(new byte[]{2, 64, 58, 0, 0, 36, 4, -113, 36, 116, 0}, bytes);
    }

    @Test
    public void testToBytesBig() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.MAX);
        assertArrayEquals(new byte[]{8, 0, 0, 0, 85, 10, 27, 72, -9, 0, 0, 78, -108, -111, 78, -1, -1}, bytes);
    }

    @Test
    public void testFromBytesBig() {
        LocalDateTime localDateTime = LocalDateTimeConverter.INSTANCE
                .fromBytes(new byte[]{8, 0, 0, 0, 85, 10, 27, 72, -9, 0, 0, 78, -108, -111, 78, -1, -1});
        assertEquals(LocalDateTime.MAX, localDateTime);
    }

    @Test
    public void testToBytesSmall() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.of(0, 1, 1, 0, 0, 0));
        assertArrayEquals(new byte[]{4, -1, -11, 5, 88, 0}, bytes);
    }

    @Test
    public void testFromBytesSmall() {
        LocalDateTime localDateTime = LocalDateTimeConverter.INSTANCE
                .fromBytes(new byte[]{4, -1, -11, 5, 88, 0});
        assertEquals(LocalDateTime.of(0, 1, 1, 0, 0, 0), localDateTime);
    }
}
