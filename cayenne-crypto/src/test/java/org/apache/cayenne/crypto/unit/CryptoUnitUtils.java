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

import java.math.BigInteger;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.crypto.key.KeySource;

public class CryptoUnitUtils {

    private static final int DEFAULT_BLOCK_SIZE = 16;

    public static byte[] hexToBytes(String hexString) {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();

        // http://stackoverflow.com/questions/4407779/biginteger-to-byte
        if (bytes.length > 0 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        } else {
            return bytes;
        }
    }

    public static byte[] decrypt_AES_CBC(byte[] source, ServerRuntime runtime) {

        byte[] keyNameBytes = Arrays.copyOfRange(source, 0, DEFAULT_BLOCK_SIZE);
        byte[] ivBytes = Arrays.copyOfRange(source, DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_SIZE * 2);
        byte[] cipherText = Arrays.copyOfRange(source, DEFAULT_BLOCK_SIZE * 2, source.length - DEFAULT_BLOCK_SIZE * 2);

        try {

            Cipher decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // 'trim' is to get rid of 0 padding
            String keyName = new String(keyNameBytes, "UTF-8").trim();
            Key key = runtime.getInjector().getInstance(KeySource.class).getKey(keyName);

            decCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivBytes));

            return decCipher.doFinal(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
