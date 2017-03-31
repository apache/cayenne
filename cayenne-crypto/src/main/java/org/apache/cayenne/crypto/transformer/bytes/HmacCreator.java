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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Actual authentication code generation logic that is used
 * by both {@link HmacEncryptor} and {@link HmacDecryptor}.
 *
 * @since 4.0
 */
abstract class HmacCreator {

    /**
     * Default algorithm for authentication code creation.
     */
    public static final String DEFAULT_HMAC_ALGORITHM = "HmacSHA256";

    private Header header;
    private Mac mac;

    HmacCreator(Header header, Key key) {
        this.header = header;
        try {
            // Currently algorithm is hardcoded, but can be easily transformed into configurable parameter
            mac = Mac.getInstance(DEFAULT_HMAC_ALGORITHM);
            mac.init(key);
        } catch (NoSuchAlgorithmException nsae) {
            throw new CayenneRuntimeException("Algorithm %s not supported for HMAC generation", nsae, DEFAULT_HMAC_ALGORITHM);
        } catch (InvalidKeyException ike) {
            throw new CayenneRuntimeException("Invalid key for HMAC generation", ike);
        }
    }

    byte[] createHmac(byte[] input) {
        byte[] rawHeader = new byte[header.size()];
        header.store(rawHeader, 0, header.getFlags());
        mac.update(rawHeader);
        return mac.doFinal(input);
    }
}
