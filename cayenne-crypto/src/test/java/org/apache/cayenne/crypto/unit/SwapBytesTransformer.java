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
package org.apache.cayenne.crypto.unit;

import java.security.Key;

import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;
import org.apache.cayenne.crypto.transformer.bytes.BytesEncryptor;

/**
 * A fake "cipher" used for unit tests that does simple bytes swapping.
 */
public class SwapBytesTransformer implements BytesEncryptor, BytesDecryptor {

    private static final SwapBytesTransformer instance = new SwapBytesTransformer();

    public static BytesEncryptor encryptor() {
        return instance;
    }

    public static BytesDecryptor decryptor() {
        return instance;
    }

    private SwapBytesTransformer() {
    }

    @Override
    public byte[] decrypt(byte[] input, int inputOffset, Key key) {

        byte[] output = new byte[input.length];
        System.arraycopy(input, inputOffset, output, 0, input.length);

        swap(output, 0, output.length - 1);
        return output;
    }

    @Override
    public byte[] encrypt(byte[] input, int outputOffset) {

        byte[] output = new byte[input.length + outputOffset];

        System.arraycopy(input, 0, output, outputOffset, input.length);

        swap(output, outputOffset, outputOffset + input.length - 1);

        return output;
    }

    private void swap(byte[] buffer, int start, int end) {

        if (start >= end) {
            return;
        }

        byte b = buffer[end];
        buffer[end] = buffer[start];
        buffer[start] = b;

        swap(buffer, ++start, --end);
    }
}
