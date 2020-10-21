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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class GzipEncryptorTest {

    @Test
    public void testGzip() throws IOException {

        byte[] input1 = "Hello Hello Hello".getBytes(StandardCharsets.UTF_8);
        byte[] output1 = GzipEncryptor.gzip(input1);
        byte[] expectedOutput1 = CryptoUnitUtils.hexToBytes("1f8b0800000000000000f348cdc9c957f0409000a91a078c11000000");

        // since JDK 16, gzip call sets OS flag to 255, that means "unknown", previous versions set this byte to 0
        // see http://www.zlib.org/rfc-gzip.html spec for gzip header bytes details
        if(output1.length > 8 && output1[9] == -1) {
            output1[9] = 0;
        }

        assertArrayEquals(expectedOutput1, output1);
    }

    @Test
    public void testEncrypt() {

        BytesEncryptor delegate = mock(BytesEncryptor.class);
        when(delegate.encrypt(any(byte[].class), anyInt(), any(byte[].class))).thenAnswer((Answer<byte[]>) invocation -> {
            Object[] args = invocation.getArguments();

            byte[] answer = (byte[]) args[0];
            int offset = (Integer) args[1];

            assertEquals(1, offset);

            return answer;
        });

        GzipEncryptor e = new GzipEncryptor(delegate);

        byte[] input1 = "Hello Hello Hello".getBytes(StandardCharsets.UTF_8);
        byte[] output1 = e.encrypt(input1, 1, new byte[1]);
        byte[] expectedOutput1 = input1;

        assertArrayEquals(expectedOutput1, output1);

        byte[] input2 = ("Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello "
                + "Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello AAAAA Hello").getBytes(StandardCharsets.UTF_8);
        byte[] output2 = e.encrypt(input2, 1, new byte[1]);

        // somehow 'gzip -c' fills bytes 3..9 with values... the rest of the
        // gzip string is identical...
        byte[] expectedOutput2 = CryptoUnitUtils
                .hexToBytes("1f8b0800000000000000f348cdc9c957700401050f8ad9949b80c40600bbec62509b000000");
        // since JDK 16, gzip call sets OS flag to 255, that means "unknown", previous versions set this byte to 0
        // see http://www.zlib.org/rfc-gzip.html spec for gzip header bytes details
        if(output2.length > 8 && output2[9] == -1) {
            output2[9] = 0;
        }

        assertArrayEquals(expectedOutput2, output2);

    }

}
