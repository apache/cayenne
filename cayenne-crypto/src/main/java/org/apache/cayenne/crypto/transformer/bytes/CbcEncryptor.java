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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * A {@link BytesEncryptor} that encrypts the provided bytes. The first block in
 * the encrypted bytes is the value of IV used to seed the CBC transformation.
 * It will be needed for decryption. The object is stateful and is not
 * thread-safe.
 * 
 * @since 3.2
 */
class CbcEncryptor implements BytesEncryptor {

    private Cipher cipher;
    private byte[] iv;
    private Key key;
    private int blockSize;

    public CbcEncryptor(Cipher cipher, Key key, byte[] seedIv) {
        this.key = key;
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();

        if (seedIv.length != blockSize) {

            // TODO: perhaps we should truncate/expand it if there's a mismatch
            throw new CayenneCryptoException("IV size is expected to be the same as block size. Was " + seedIv.length
                    + "; block size was: " + blockSize);
        }

        // making a copy - we are modifying this array, something that should
        // not be visible oustide this object.
        this.iv = Arrays.copyOf(seedIv, blockSize);

    }

    @Override
    public byte[] encrypt(byte[] input, int outputOffset) {

        try {
            return doEncrypt(input, outputOffset);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error on encryption", e);
        }
    }

    private byte[] doEncrypt(byte[] plain, int outputOffset) throws InvalidKeyException,
            InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {

        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encrypted = new byte[outputOffset + blockSize + cipher.getOutputSize(plain.length)];

        // copy IV in the first block
        System.arraycopy(iv, 0, encrypted, outputOffset, blockSize);

        int encBytes = cipher.doFinal(plain, 0, plain.length, encrypted, outputOffset + blockSize);

        // store the last block of ciphertext to use as an IV for the next round
        // of encryption...
        System.arraycopy(encrypted, outputOffset + encBytes, iv, 0, blockSize);

        return encrypted;
    }

}
