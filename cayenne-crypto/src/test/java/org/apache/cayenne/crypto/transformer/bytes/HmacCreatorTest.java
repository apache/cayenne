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

package org.apache.cayenne.crypto.transformer.bytes;

import java.security.Key;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;

import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class HmacCreatorTest {

    /**
     * Sample output from https://en.wikipedia.org/wiki/Hash-based_message_authentication_code
     */
    @Test
    public void createHmac() {
        // a header is any byte sequence whose 4th byte is its size: here the first 32 bytes of the sample input,
        // as the 4th byte of the sample input is a space, i.e. 32
        byte[] sample = "The quick brown fox jumps over the lazy dog".getBytes();
        Header header = Header.create(Arrays.copyOf(sample, 32), 0);

        Key key = new SecretKeySpec("key".getBytes(), "AES");
        HmacCreator creator = new HmacCreator(header, key) {};

        byte[] hmac = creator.createHmac(Arrays.copyOfRange(sample, 32, sample.length));
        byte[] hmacExpected = CryptoUnitUtils.hexToBytes("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");

        assertArrayEquals(hmacExpected, hmac);
    }

}