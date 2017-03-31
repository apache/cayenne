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

import java.security.Key;

import org.apache.cayenne.crypto.unit.SwapBytesTransformer;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0
 */
public class HmacDecryptorTest {

    @Test
    public void decrypt() throws Exception {
        HmacDecryptor decryptor = mock(HmacDecryptor.class);
        decryptor.delegate = SwapBytesTransformer.decryptor();
        when(decryptor.createHmac(any(byte[].class))).thenReturn(new byte[]{0, 1, 2, 3, 4, 5, 6, 7});
        when(decryptor.decrypt(any(byte[].class), anyInt(), any(Key.class))).thenCallRealMethod();

        byte[] expectedResult = {-1, -2, -3};

        byte[] input1 = {8, 0, 1, 2, 3, 4, 5, 6, 7, -3, -2, -1};
        byte[] result1 = decryptor.decrypt(input1, 0, null);
        assertArrayEquals(expectedResult, result1);

        byte[] input2 = {0, 0, 0, 8, 0, 1, 2, 3, 4, 5, 6, 7, -3, -2, -1};
        byte[] result2 = decryptor.decrypt(input2, 3, null);
        assertArrayEquals(expectedResult, result2);
    }

}