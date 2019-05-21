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


public class IntegerConverterTest {

    @Test
    public void testFromBytes_InByteRange() {
        assertEquals(new Integer(6), new IntegerConverter().fromBytes(new byte[]{6}));
    }

    @Test
    public void testFromBytes_InShortRange() {
        assertEquals(new Integer(1542), new IntegerConverter().fromBytes(new byte[]{6, 6}));
    }

    @Test
    public void testFromBytes_InIntRange() {
        // 16 + 256*7 + 256*256* 6
        assertEquals(new Integer(395024), new IntegerConverter().fromBytes(new byte[]{0, 6, 7, 16}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytes_TooLong() {
        IntegerConverter.INSTANCE.fromBytes(new byte[]{6, 5, 4});
    }

    @Test
    public void testToBytes() {
        assertArrayEquals(new byte[]{127, -1, -1, -2}, new IntegerConverter().toBytes(Integer.MAX_VALUE - 1));
        assertArrayEquals(new byte[]{127, -2}, new IntegerConverter().toBytes(Short.MAX_VALUE - 1));
        assertArrayEquals(new byte[]{-7}, new IntegerConverter().toBytes(-7));
    }
}
