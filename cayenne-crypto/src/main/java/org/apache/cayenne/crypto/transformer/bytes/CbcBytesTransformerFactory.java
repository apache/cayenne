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

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.crypto.Cipher;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.key.KeySource;

/**
 * @since 3.2
 */
class CbcBytesTransformerFactory implements BytesTransformerFactory {

    private static final String KEY_NAME_CHARSET = "UTF-8";

    private CipherFactory cipherFactory;
    private Key key;
    private byte[] keyName;
    private int blockSize;
    private Queue<SecureRandom> randoms;

    public CbcBytesTransformerFactory(CipherFactory cipherFactory, KeySource keySource, String keyName) {

        this.randoms = new ConcurrentLinkedQueue<SecureRandom>();
        this.cipherFactory = cipherFactory;
        this.blockSize = cipherFactory.blockSize();

        byte[] keyNameBytes;
        try {
            keyNameBytes = keyName.getBytes(KEY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't encode in " + KEY_NAME_CHARSET, e);
        }

        if (keyNameBytes.length == blockSize) {
            this.keyName = keyNameBytes;
        } else if (keyNameBytes.length < blockSize) {
            this.keyName = new byte[blockSize];
            System.arraycopy(keyNameBytes, 0, this.keyName, 0, keyNameBytes.length);
        } else {
            throw new CayenneCryptoException("Key name '" + keyName + "' is too long. Its byte form should not exceed "
                    + blockSize + " bytes");
        }
    }

    protected byte[] generateSeedIv() {

        byte[] iv = new byte[blockSize];

        // the idea of a queue of SecureRandoms for concurrency is taken from
        // Tomcat's SessionIdGenerator. Also some code...

        SecureRandom random = randoms.poll();
        if (random == null) {
            random = createSecureRandom();
        }

        random.nextBytes(iv);
        randoms.add(random);

        return iv;
    }

    /**
     * Create a new random number generator instance we should use for
     * generating session identifiers.
     */
    private SecureRandom createSecureRandom() {

        // TODO: allow to customize provider?
        SecureRandom result = new SecureRandom();

        // Force seeding to take place
        result.nextInt();
        return result;
    }

    @Override
    public BytesTransformer encryptor() {
        Cipher cipher = cipherFactory.cipher();

        BytesTransformer cbcEncryptor = new CbcEncryptor(cipher, key, generateSeedIv());

        // TODO: make adding key name for versioning an optional property
        return new EncryptorWithKeyName(cbcEncryptor, keyName, blockSize);
    }

    @Override
    public BytesTransformer decryptor() {
        throw new UnsupportedOperationException("TODO");
    }

}
