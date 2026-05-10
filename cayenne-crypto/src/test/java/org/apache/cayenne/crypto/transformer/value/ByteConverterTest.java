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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ByteConverterTest {

    @Test
    public void fromBytes() {
        assertEquals(Byte.valueOf((byte) 6), ByteConverter.INSTANCE.fromBytes(new byte[]{6}));
        assertEquals(Byte.valueOf((byte) -7), ByteConverter.INSTANCE.fromBytes(new byte[]{-7}));
    }

    @Test
    public void fromBytes_TooLong() {
        assertThrows(IllegalArgumentException.class, () -> ByteConverter.INSTANCE.fromBytes(new byte[]{6, 5, 4}));
    }

    @Test
    public void toBytes() {
        assertArrayEquals(new byte[]{6}, ByteConverter.INSTANCE.toBytes((byte) 6));
        assertArrayEquals(new byte[]{-7}, ByteConverter.INSTANCE.toBytes((byte) -7));
    }
}
