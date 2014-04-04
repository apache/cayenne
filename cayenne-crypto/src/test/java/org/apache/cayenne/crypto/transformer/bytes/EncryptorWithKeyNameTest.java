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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class EncryptorWithKeyNameTest {

    @Test
    public void testGetOutputSize() throws UnsupportedEncodingException {

        byte[] keyName = "mykey".getBytes("UTF-8");
        BytesEncryptor delegate = mock(BytesEncryptor.class);
        when(delegate.getOutputSize(8)).thenReturn(8);

        // try with non-standard block size..
        EncryptorWithKeyName encryptor = new EncryptorWithKeyName(delegate, keyName, 5);
        assertEquals(13, encryptor.getOutputSize(8));
    }

    @Test
    public void testTransform() throws UnsupportedEncodingException {

        byte[] keyName = "mykey".getBytes("UTF-8");

        BytesEncryptor delegate = mock(BytesEncryptor.class);
        when(delegate.getOutputSize(8)).thenReturn(8);

        byte[] input = { 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] output = new byte[16];

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                Object[] args = invocation.getArguments();
                byte[] input = (byte[]) args[0];
                byte[] output = (byte[]) args[1];
                int offset = (Integer) args[2];

                for (int i = 0; i < input.length; i++) {
                    output[i + offset] = 1;
                }

                return null;
            }
        }).when(delegate).encrypt(input, output, 6);

        // intentionally non-standard block size..
        EncryptorWithKeyName encryptor = new EncryptorWithKeyName(delegate, keyName, 5);

        encryptor.encrypt(input, output, 1);

        assertArrayEquals(new byte[] { 0, 'm', 'y', 'k', 'e', 'y', 1, 1, 1, 1, 1, 1, 1, 1, 0, 0 }, output);
    }

}
