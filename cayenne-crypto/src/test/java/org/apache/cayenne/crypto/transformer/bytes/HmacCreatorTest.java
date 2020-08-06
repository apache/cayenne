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
import javax.crypto.spec.SecretKeySpec;

import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @since 4.0
 */
public class HmacCreatorTest {

    /**
     * Sample output from https://en.wikipedia.org/wiki/Hash-based_message_authentication_code
     */
    @Test
    public void createHmac() {
        final byte[] headerData = "The quick".getBytes();
        Header header = mock(Header.class);

        doReturn(headerData.length).when(header).size();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                byte[] input = (byte[])invocation.getArguments()[0];
                System.arraycopy(headerData, 0, input, 0, headerData.length);
                return null;
            }
        }).when(header).store(any(byte[].class), anyInt(), anyByte());

        Key key = new SecretKeySpec("key".getBytes(), "AES");
        HmacCreator creator = new HmacCreator(header, key) {};

        byte[] hmac = creator.createHmac(" brown fox jumps over the lazy dog".getBytes());
        byte[] hmacExpected = CryptoUnitUtils.hexToBytes("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");

        assertArrayEquals(hmacExpected, hmac);
    }

}