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

package org.apache.cayenne.access.types;

import org.junit.Test;

import static org.junit.Assert.fail;

public class ByteArrayTypeTest {

    @Test
    public void testTrimBytes1() throws Exception {
        byte[] b1 = new byte[] {
                1, 2, 3
        };
        byte[] b2 = ByteArrayType.trimBytes(b1);
        assertByteArraysEqual(b1, b2);
    }

    @Test
    public void testTrimBytes2() throws Exception {
        byte[] ref = new byte[] {
                1, 2, 3
        };
        byte[] b1 = new byte[] {
                1, 2, 3, 0, 0
        };
        byte[] b2 = ByteArrayType.trimBytes(b1);
        assertByteArraysEqual(ref, b2);
    }

    @Test
    public void testTrimBytes3() throws Exception {
        byte[] b1 = new byte[] {
                0, 1, 2, 3
        };
        byte[] b2 = ByteArrayType.trimBytes(b1);
        assertByteArraysEqual(b1, b2);
    }

    @Test
    public void testTrimBytes4() throws Exception {
        byte[] b1 = new byte[] {};
        byte[] b2 = ByteArrayType.trimBytes(b1);
        assertByteArraysEqual(b1, b2);
    }

    public static void assertByteArraysEqual(byte[] b1, byte[] b2) throws Exception {
        if (b1 == b2) {
            return;
        }

        if (b1 == null && b2 == null) {
            return;
        }

        if (b1 == null) {
            fail("byte arrays differ (first one is null)");
        }

        if (b2 == null) {
            fail("byte arrays differ (second one is null)");
        }

        if (b1.length != b2.length) {
            fail("byte arrays differ (length differs: ["
                    + b1.length
                    + ","
                    + b2.length
                    + "])");
        }

        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                fail("byte arrays differ (at position " + i + ")");
            }
        }
    }
}
