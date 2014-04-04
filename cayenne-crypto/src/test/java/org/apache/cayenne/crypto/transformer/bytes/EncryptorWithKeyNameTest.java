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

import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;

import org.apache.cayenne.crypto.unit.SwapBytesTransformer;
import org.junit.Test;

public class EncryptorWithKeyNameTest {

    @Test
    public void testTransform() throws UnsupportedEncodingException {

        byte[] keyName = "mykey".getBytes("UTF-8");

        BytesEncryptor delegate = SwapBytesTransformer.encryptor();

        byte[] input = { 1, 2, 3, 4, 5, 6, 7, 8 };

        // intentionally non-standard block size..
        EncryptorWithKeyName encryptor = new EncryptorWithKeyName(delegate, keyName, 5);

        byte[] output = encryptor.encrypt(input, 1);
        assertArrayEquals(new byte[] { 0, 'm', 'y', 'k', 'e', 'y', 8, 7, 6, 5, 4, 3, 2, 1 }, output);
    }

}
