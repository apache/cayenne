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

import java.security.Key;
import java.util.Arrays;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * This class not only parse HMAC but also verifies it
 * and throws {@link org.apache.cayenne.crypto.CayenneCryptoException} in case it is invalid.
 *
 * @since 4.0
 */
class HmacDecryptor extends HmacCreator implements BytesDecryptor {

    BytesDecryptor delegate;

    HmacDecryptor(BytesDecryptor delegate, Header header, Key key) {
        super(header, key);
        this.delegate = delegate;
    }

    @Override
    public byte[] decrypt(byte[] input, int inputOffset, Key key) {
        byte hmacLength = input[inputOffset++];
        if(hmacLength <= 0) {
            throw new CayenneCryptoException("Input is corrupted: invalid HMAC length.");
        }

        byte[] receivedHmac = new byte[hmacLength];
        byte[] decrypted = delegate.decrypt(input, inputOffset + hmacLength, key);
        byte[] realHmac = createHmac(decrypted);

        System.arraycopy(input, inputOffset, receivedHmac, 0, hmacLength);
        if(!Arrays.equals(receivedHmac, realHmac)) {
            throw new CayenneCryptoException("Input is corrupted: wrong HMAC.");
        }
        return decrypted;
    }
}
