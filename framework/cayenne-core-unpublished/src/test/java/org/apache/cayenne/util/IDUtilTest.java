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


package org.apache.cayenne.util;

import junit.framework.TestCase;

import org.apache.cayenne.access.types.ByteArrayTypeTest;

/**
 */
public class IDUtilTest extends TestCase {

    public void testPseudoUniqueByteSequence1() throws Exception {
        try {
            IDUtil.pseudoUniqueByteSequence(10);
            fail("must throw an exception on short sequences");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testPseudoUniqueByteSequence2() throws Exception {
        byte[] byte16 = IDUtil.pseudoUniqueByteSequence(16);
        assertNotNull(byte16);
        assertEquals(16, byte16.length);

        try {
            ByteArrayTypeTest.assertByteArraysEqual(
                byte16,
                IDUtil.pseudoUniqueByteSequence(16));
            fail("Same byte array..");
        } catch (Throwable th) {

        }
    }

    public void testPseudoUniqueByteSequence3() throws Exception {
        byte[] byte17 = IDUtil.pseudoUniqueByteSequence(17);
        assertNotNull(byte17);
        assertEquals(17, byte17.length);

        byte[] byte123 = IDUtil.pseudoUniqueByteSequence(123);
        assertNotNull(byte123);
        assertEquals(123, byte123.length);
    }

}
