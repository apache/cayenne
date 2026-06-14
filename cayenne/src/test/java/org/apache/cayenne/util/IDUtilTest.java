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


package org.apache.cayenne.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IDUtilTest {

    @Test
    public void pseudoUniqueByteSequence1() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> IDUtil.pseudoUniqueByteSequence(10));
    }

    @Test
    public void pseudoUniqueByteSequence2() throws Exception {
        byte[] byte16 = IDUtil.pseudoUniqueByteSequence(16);
        assertNotNull(byte16);
        assertEquals(16, byte16.length);

        // verify that two calls return different sequences
        byte[] byte16b = IDUtil.pseudoUniqueByteSequence(16);
        assertNotNull(byte16b);
        assertEquals(16, byte16b.length);
        assertNotSameContent(byte16, byte16b);
    }

    @Test
    public void pseudoUniqueByteSequence3() throws Exception {
        byte[] byte17 = IDUtil.pseudoUniqueByteSequence(17);
        assertNotNull(byte17);
        assertEquals(17, byte17.length);

        byte[] byte123 = IDUtil.pseudoUniqueByteSequence(123);
        assertNotNull(byte123);
        assertEquals(123, byte123.length);
    }

    private static void assertNotSameContent(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return; // different lengths, definitely different
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return; // found a difference
            }
        }
        throw new AssertionError("Byte arrays are identical — expected unique sequences");
    }
}
