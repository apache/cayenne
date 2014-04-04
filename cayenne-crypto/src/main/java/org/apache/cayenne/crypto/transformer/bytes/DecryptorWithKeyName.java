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

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.key.KeySource;

/**
 * @since 3.2
 */
class DecryptorWithKeyName implements BytesDecryptor {

    private static final String KEY_NAME_CHARSET = "UTF-8";

    private KeySource keySource;
    private BytesDecryptor delegate;
    private int blockSize;

    public DecryptorWithKeyName(BytesDecryptor delegate, KeySource keySource, int blockSize) {
        this.delegate = delegate;
        this.blockSize = blockSize;
        this.keySource = keySource;
    }

    @Override
    public byte[] decrypt(byte[] input, int inputOffset, Key key) {

        // ignoring the parameter key... using the key from the first block

        String keyName = keyName(input, inputOffset);
        Key inRecordKey = keySource.getKey(keyName);
        return delegate.decrypt(input, inputOffset + blockSize, inRecordKey);
    }

    String keyName(byte[] input, int inputOffset) {
        try {
            // 'trim' is to get rid of 0 padding
            return new String(input, inputOffset, blockSize, KEY_NAME_CHARSET).trim();
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't decode with " + KEY_NAME_CHARSET, e);
        }
    }

}
