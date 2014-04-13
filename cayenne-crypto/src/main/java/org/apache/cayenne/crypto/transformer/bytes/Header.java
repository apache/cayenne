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

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * Represents a header with metadata about the encrypted data. A header is
 * prependend to each encrypted value, and itself is not encrypted.
 * 
 * @since 3.2
 */
public class Header {

    private static final String KEY_NAME_CHARSET = "UTF-8";

    /**
     * The size of a header byte[] block.
     */
    public static final int HEADER_SIZE = 16;

    /**
     * The size of a key name within the header block.
     */
    public static final int KEY_NAME_SIZE = 8;

    /**
     * Position of the key name within the header block.
     */
    public static final int KEY_NAME_OFFSET = 8;

    /**
     * Position of the "flags" byte in the header.
     */
    public static final int FLAGS_OFFSET = 0;

    private byte[] data;
    private int offset;

    public static Header create(String keyName) {
        byte[] keyNameBytes;
        try {
            keyNameBytes = keyName.getBytes(KEY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't encode in " + KEY_NAME_CHARSET, e);
        }

        byte[] data = new byte[HEADER_SIZE];

        if (keyNameBytes.length <= KEY_NAME_SIZE) {
            System.arraycopy(keyNameBytes, 0, data, KEY_NAME_OFFSET, keyNameBytes.length);
        } else {
            throw new CayenneCryptoException("Key name '" + keyName
                    + "' is too long. Its UTF8-encoded form should not exceed " + KEY_NAME_SIZE + " bytes");
        }

        return create(data, 0);
    }

    public static Header create(byte[] data, int offset) {

        if (data.length - offset < HEADER_SIZE) {
            throw new CayenneCryptoException("Unexpected header data size: " + data.length + ", expected size is "
                    + HEADER_SIZE);
        }

        return new Header(data, offset);
    }

    // private constructor... construction is done via factory methods...
    private Header(byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    public void store(byte[] output, int outputOffset) {
        System.arraycopy(data, offset, output, outputOffset, Header.HEADER_SIZE);
    }

    public String getKeyName() {
        try {
            // 'trim' is to get rid of 0 padding
            return new String(data, offset + KEY_NAME_OFFSET, KEY_NAME_SIZE, KEY_NAME_CHARSET).trim();
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't decode with " + KEY_NAME_CHARSET, e);
        }
    }
}
