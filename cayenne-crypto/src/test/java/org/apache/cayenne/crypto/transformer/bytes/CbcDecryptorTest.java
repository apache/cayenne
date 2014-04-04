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

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Before;
import org.junit.Test;

public class CbcDecryptorTest {

    private Cipher cipher;
    private Key key;

    private byte[] hex(String hexString) {
        byte[] bytes = new BigInteger(hexString, 16).toByteArray();

        // http://stackoverflow.com/questions/4407779/biginteger-to-byte
        if (bytes.length > 0 && bytes[0] == 0) {
            return Arrays.copyOfRange(bytes, 1, bytes.length);
        } else {
            return bytes;
        }
    }

    @Before
    public void before() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        byte[] keyBytes = hex("a4cb499fa31a6a228e16b7e4741d4fa3");
        this.key = new SecretKeySpec(keyBytes, "AES");

        this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        assertEquals(16, cipher.getBlockSize());
    }

    @Test
    public void testIv() {

        CbcDecryptor decryptor = new CbcDecryptor(cipher);

        byte[] input = { 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        byte[] ivBytes = { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };

        IvParameterSpec iv = decryptor.iv(input, 5);
        assertArrayEquals(ivBytes, iv.getIV());
    }

    @Test
    public void testDecrypt() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        CbcDecryptor decryptor = new CbcDecryptor(cipher);

        byte[] plain = { 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };
        byte[] ivBytes = hex("0591849d87c93414f4405d32f4d69220");

        Cipher encCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encCipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ivBytes));

        byte[] encrypted = encCipher.doFinal(plain);

        byte[] encryptedWithIv = new byte[encrypted.length + ivBytes.length];
        System.arraycopy(ivBytes, 0, encryptedWithIv, 0, ivBytes.length);
        System.arraycopy(encrypted, 0, encryptedWithIv, ivBytes.length, encrypted.length);

        byte[] decrypted = decryptor.decrypt(encryptedWithIv, 0, key);
        assertArrayEquals(plain, decrypted);
    }
}
