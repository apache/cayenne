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
import static org.mockito.Mockito.mock;

import java.io.UnsupportedEncodingException;
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

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.junit.Test;

public class CbcEncryptorTest {

    @Test(expected = CayenneCryptoException.class)
    public void testConstructor() throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException {

        byte[] iv = { 1, 2, 3, 4, 5, 6 };
        Key key = mock(Key.class);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        assertEquals(8, cipher.getBlockSize());

        // must throw as IV sie and block size are different
        new CbcEncryptor(cipher, key, iv);
    }

    @Test
    public void testGetOutputSize_DES() throws UnsupportedEncodingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {

        byte[] iv = { 1, 2, 3, 4, 5, 6, 7, 8 };
        byte[] keyBytes = { 1, 2, 3, 4, 5, 6, 7, 8 };
        Key key = new SecretKeySpec(keyBytes, "DES");

        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        assertEquals(8, cipher.getBlockSize());

        CbcEncryptor encryptor = new CbcEncryptor(cipher, key, iv);
        assertEquals(24, encryptor.getOutputSize(11));
    }

    @Test
    public void testGetOutputSize_AES() throws UnsupportedEncodingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException {

        byte[] ivBytes = CryptoUnitUtils.hexToBytes("0591849d87c93414f4405d32f4d69220");
        byte[] keyBytes = CryptoUnitUtils.hexToBytes("a4cb499fa31a6a228e16b7e4741d4fa3");
        Key key = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        assertEquals(16, cipher.getBlockSize());

        CbcEncryptor encryptor = new CbcEncryptor(cipher, key, ivBytes);
        assertEquals(32, encryptor.getOutputSize(11));
    }

    @Test
    public void testEncrypt_AES() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

        byte[] ivBytes = CryptoUnitUtils.hexToBytes("0591849d87c93414f4405d32f4d69220");
        byte[] keyBytes = CryptoUnitUtils.hexToBytes("a4cb499fa31a6a228e16b7e4741d4fa3");
        Key key = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        assertEquals(16, cipher.getBlockSize());

        byte[] plain = { 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 };

        // copy ivBytes, as they are reset
        CbcEncryptor encryptor = new CbcEncryptor(cipher, key, ivBytes);

        byte[] encrypted = new byte[encryptor.getOutputSize(plain.length)];

        encryptor.encrypt(plain, encrypted, 0);

        assertArrayEquals(ivBytes, Arrays.copyOfRange(encrypted, 0, 16));

        Cipher decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decCipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));
        byte[] newPlain = decCipher.doFinal(encrypted, 16, encrypted.length - 16);
        assertArrayEquals(plain, newPlain);
    }

}
