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
package org.apache.cayenne.crypto.transformer.value;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import org.junit.Before;
import org.junit.Test;

public class JceValueEncryptorTest {

    private Cipher encCipher;
    private Cipher decCipher;
    private SecretKey key;

    @Before
    public void before() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);

        this.key = keyGen.generateKey();

        this.encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        this.encCipher.init(Cipher.ENCRYPT_MODE, key);

        this.decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        this.decCipher.init(Cipher.DECRYPT_MODE, key);
    }

    @Test
    public void testTransform_BytesToBytes() throws IllegalBlockSizeException, BadPaddingException {

        JceValueEncryptor e = new JceValueEncryptor(BytesToBytesConverter.INSTANCE);

        byte[] b1 = new byte[] { 1, 2 };
        byte[] b2 = new byte[] { 2, 3 };

        byte[] b1_t = (byte[]) e.transform(encCipher, b1);

        assertNotNull(b1_t);
        assertEquals(encCipher.getBlockSize(), b1_t.length);
        assertArrayEquals(b1, decCipher.doFinal(b1_t));

        byte[] b2_t = (byte[]) e.transform(encCipher, b2);

        assertNotNull(b2_t);
        assertEquals(encCipher.getBlockSize(), b2_t.length);
        assertArrayEquals(b2, decCipher.doFinal(b2_t));
    }

    @Test
    public void testTransform_BytesToBytes_DifferentSizes() {

        JceValueEncryptor e = new JceValueEncryptor(BytesToBytesConverter.INSTANCE);

        int blockSize = encCipher.getBlockSize();

        byte[] b1 = new byte[] {};
        byte[] b2 = new byte[] { 1 };
        byte[] b3 = new byte[] { 1, 2 };

        byte[] b4 = new byte[blockSize];
        for (int i = 0; i < blockSize; i++) {
            b4[i] = (byte) i;
        }

        byte[] b5 = new byte[blockSize + 5];
        for (int i = 0; i < blockSize + 5; i++) {
            b5[i] = (byte) i;
        }

        byte[] b1_t = (byte[]) e.transform(encCipher, b1);
        assertEquals(encCipher.getBlockSize(), b1_t.length);

        byte[] b2_t = (byte[]) e.transform(encCipher, b2);
        assertEquals(encCipher.getBlockSize(), b2_t.length);

        byte[] b3_t = (byte[]) e.transform(encCipher, b3);
        assertEquals(encCipher.getBlockSize(), b3_t.length);

        byte[] b4_t = (byte[]) e.transform(encCipher, b4);
        assertEquals(encCipher.getBlockSize() * 2, b4_t.length);

        byte[] b5_t = (byte[]) e.transform(encCipher, b5);
        assertEquals(encCipher.getBlockSize() * 2, b5_t.length);
    }

    @Test
    public void testTransform_StringToBytes() throws UnsupportedEncodingException, IllegalBlockSizeException,
            BadPaddingException {

        JceValueEncryptor e = new JceValueEncryptor(StringToBytesConverter.INSTANCE);

        String s1 = "ab";
        String s2 = "cd";

        byte[] b1_t = (byte[]) e.transform(encCipher, s1);

        assertNotNull(b1_t);
        assertEquals(encCipher.getBlockSize(), b1_t.length);
        assertEquals(s1, new String(decCipher.doFinal(b1_t), "UTF-8"));

        byte[] b2_t = (byte[]) e.transform(encCipher, s2);

        assertNotNull(b2_t);
        assertEquals(encCipher.getBlockSize(), b2_t.length);
        assertEquals(s2, new String(decCipher.doFinal(b2_t), "UTF-8"));
    }
}
