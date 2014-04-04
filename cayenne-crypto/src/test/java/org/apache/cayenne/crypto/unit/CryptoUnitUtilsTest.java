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
package org.apache.cayenne.crypto.unit;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class CryptoUnitUtilsTest {

    @Test
    public void testHexToBytes() {

        String hexString = "0506AB";
        byte[] hexByte = { 5, 6, (byte) 0xAB };
        assertArrayEquals(hexByte, CryptoUnitUtils.hexToBytes(hexString));

        String hexString2 = "0591849d87c93414f4405d32f4d69220";
        byte[] hexByte2 = { 5, (byte) 0x91, (byte) 0x84, (byte) 0x9d, (byte) 0x87, (byte) 0xc9, (byte) 0x34,
                (byte) 0x14, (byte) 0xf4, (byte) 0x40, (byte) 0x5d, (byte) 0x32, (byte) 0xf4, (byte) 0xd6, (byte) 0x92,
                (byte) 0x20 };
        assertArrayEquals(hexByte2, CryptoUnitUtils.hexToBytes(hexString2));
    }
}
