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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * @since 3.2
 */
class CbcDecryptor implements BytesDecryptor {

    private Cipher cipher;
    private int blockSize;

    CbcDecryptor(Cipher cipher) {
        this.cipher = cipher;
        this.blockSize = cipher.getBlockSize();
    }

    @Override
    public byte[] decrypt(byte[] input, int inputOffset, Key key) {
        try {
            return doDecrypt(input, inputOffset, key);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error on decryption", e);
        }
    }

    private byte[] doDecrypt(byte[] input, int inputOffset, Key key) throws InvalidKeyException,
            InvalidAlgorithmParameterException, ShortBufferException, IllegalBlockSizeException, BadPaddingException {

        IvParameterSpec iv = iv(input, inputOffset);

        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        int offset = inputOffset + blockSize;
        return cipher.doFinal(input, offset, input.length - offset);
    }

    IvParameterSpec iv(byte[] input, int inputOffset) {
        return new IvParameterSpec(input, inputOffset, blockSize);
    }
}
