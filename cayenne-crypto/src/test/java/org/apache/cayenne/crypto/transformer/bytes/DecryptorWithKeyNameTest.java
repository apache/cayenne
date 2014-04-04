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
import static org.mockito.Mockito.mock;

import org.apache.cayenne.crypto.key.KeySource;
import org.junit.Test;

public class DecryptorWithKeyNameTest {

    @Test
    public void testKeyName() {

        byte[] input1 = { 'a', 'b', 'c', 'd', 'e' };
        byte[] input2 = { 'a', 'b', 'c', 0, 'e' };
        byte[] input3 = { 'a', 'b', 0, 0, 'e' };

        DecryptorWithKeyName decryptor = new DecryptorWithKeyName(mock(BytesDecryptor.class), mock(KeySource.class), 3);
        assertEquals("bcd", decryptor.keyName(input1, 1));
        assertEquals("bc", decryptor.keyName(input2, 1));
        assertEquals("b", decryptor.keyName(input3, 1));

    }

}
