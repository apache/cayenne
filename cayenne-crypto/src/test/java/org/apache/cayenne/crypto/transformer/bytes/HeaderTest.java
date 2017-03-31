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
package org.apache.cayenne.crypto.transformer.bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.junit.Test;

public class HeaderTest {

    @Test
    public void testSetCompressed() {
        assertEquals(1, Header.setCompressed((byte) 0, true));
        assertEquals(0, Header.setCompressed((byte) 0, false));

        assertEquals(1, Header.setCompressed((byte) 1, true));
        assertEquals(0, Header.setCompressed((byte) 1, false));

        assertEquals(2, Header.setHaveHMAC((byte) 0, true));
        assertEquals(0, Header.setHaveHMAC((byte) 0, false));

        assertEquals(3, Header.setHaveHMAC((byte) 3, true));
        assertEquals(0, Header.setHaveHMAC((byte) 2, false));
    }

    @Test
    public void testCreate_WithKeyName() {

        Header h1 = Header.create("bcd", false, false);
        Header h2 = Header.create("bc", true, false);
        Header h3 = Header.create("b", false, false);
        Header h4 = Header.create("e", false, true);

        assertEquals("bcd", h1.getKeyName());
        assertFalse(h1.isCompressed());

        assertEquals("bc", h2.getKeyName());
        assertTrue(h2.isCompressed());

        assertEquals("b", h3.getKeyName());
        assertFalse(h3.isCompressed());
        assertFalse(h3.haveHMAC());

        assertEquals("e", h4.getKeyName());
        assertFalse(h4.isCompressed());
        assertTrue(h4.haveHMAC());
    }

    @Test(expected = CayenneCryptoException.class)
    public void testCreate_WithKeyName_TooLong() {

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < Byte.MAX_VALUE + 1; i++) {
            buf.append("a");
        }

        Header.create(buf.toString(), false, false);
    }

    @Test
    public void testCreate_WithData() {
        byte[] input1 = { 'C', 'C', '1', 9, 0, 'a', 'b', 'c', 'd', 'e' };
        Header h1 = Header.create(input1, 0);
        assertEquals("abcd", h1.getKeyName());
        assertFalse(h1.isCompressed());

        byte[] input2 = { 0, 'C', 'C', '1', 9, 1, 'a', 'b', 'c', 'd', 'e' };
        Header h2 = Header.create(input2, 1);
        assertEquals("abcd", h2.getKeyName());
        assertTrue(h2.isCompressed());

        byte[] input3 = { 0, 0, 'C', 'C', '1', 9, 2, 'a', 'b', 'c', 'd', 'e' };
        Header h3 = Header.create(input3, 2);
        assertEquals("abcd", h3.getKeyName());
        assertFalse(h3.isCompressed());
        assertTrue(h3.haveHMAC());

        byte[] input4 = { 0, 0, 0, 'C', 'C', '1', 9, 3, 'a', 'b', 'c', 'd', 'e' };
        Header h4 = Header.create(input4, 3);
        assertEquals("abcd", h4.getKeyName());
        assertTrue(h4.isCompressed());
        assertTrue(h4.haveHMAC());
    }
}
