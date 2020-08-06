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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class ShortConverterTest {

    @Test
    public void testFromBytes_InByteRange() {
        assertEquals(new Short((short) 6), ShortConverter.INSTANCE.fromBytes(new byte[]{6}));
    }

    @Test
    public void testFromBytes_InShortRange() {
        assertEquals(new Short((short) 1287), ShortConverter.INSTANCE.fromBytes(new byte[]{5, 7}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytes_TooLong() {
        ShortConverter.INSTANCE.fromBytes(new byte[]{6, 5, 4});
    }

    @Test
    public void testToBytes() {
        assertArrayEquals(new byte[]{127, -2}, ShortConverter.INSTANCE.toBytes((short) (Short.MAX_VALUE - 1)));
        assertArrayEquals(new byte[]{-7}, ShortConverter.INSTANCE.toBytes((short) -7));
    }
}
