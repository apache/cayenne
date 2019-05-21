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

import java.io.UnsupportedEncodingException;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * Represents a header with metadata about the encrypted data. A header is
 * prependend to each encrypted value, and itself is not encrypted. Header
 * format is the following:
 * <ul>
 * <li>byte 0..2: "magic" number identifying the format as Cayenne-crypto
 * encrypted sequence.
 * <li>byte 3: header length N, i.e. how many bytes the header contains,
 * including magic number and the length indicator. N can be 0..127.
 * <li>byte 4: a bit String representing various flags, such as compression.
 * <li>byte 5..N: UTF8-encoded symbolic name of the encryption key.
 * </ul>
 * 
 * @since 4.0
 */
public class Header {

    private static final String KEY_NAME_CHARSET = "UTF-8";

    // "CC1" is a "magic number" identifying Cayenne-crypto version 1 value
    private static final byte[] MAGIC_NUMBER = { 'C', 'C', '1' };

    /**
     * Position of the "flags" byte in the header.
     */
    private static final int MAGIC_NUMBER_POSITION = 0;

    /**
     * Position of the header size byte in the header.
     */
    private static final int SIZE_POSITION = 3;

    /**
     * Position of the "flags" byte in the header.
     */
    private static final int FLAGS_POSITION = 4;

    /**
     * Position of the key name within the header block.
     */
    private static final int KEY_NAME_OFFSET = 5;

    /**
     * Max size of a key name within a header.
     */
    private static final int KEY_NAME_MAX_SIZE = Byte.MAX_VALUE - KEY_NAME_OFFSET;

    /**
     * A position of the compress bit.
     */
    private static final int COMPRESS_BIT = 0;

    /**
     * A position if the HMAC bit
     */
    private static final int HMAC_BIT = 1;

    private byte[] data;
    private int offset;

    public static Header create(String keyName, boolean compressed, boolean withHMAC) {
        byte[] keyNameBytes;
        try {
            keyNameBytes = keyName.getBytes(KEY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't encode in " + KEY_NAME_CHARSET, e);
        }

        if (keyNameBytes.length > KEY_NAME_MAX_SIZE) {
            throw new CayenneCryptoException("Key name '" + keyName
                    + "' is too long. Its UTF8-encoded form should not exceed " + KEY_NAME_MAX_SIZE + " bytes");
        }

        int n = MAGIC_NUMBER.length + 1 + 1 + keyNameBytes.length;

        byte[] data = new byte[n];
        System.arraycopy(MAGIC_NUMBER, 0, data, MAGIC_NUMBER_POSITION, MAGIC_NUMBER.length);

        // total header size
        data[SIZE_POSITION] = (byte) n;

        // flags
        if (compressed) {
            data[FLAGS_POSITION] = bitOn(data[FLAGS_POSITION], COMPRESS_BIT);
        }
        if (withHMAC) {
            data[FLAGS_POSITION] = bitOn(data[FLAGS_POSITION], HMAC_BIT);
        }

        // key name
        System.arraycopy(keyNameBytes, 0, data, KEY_NAME_OFFSET, keyNameBytes.length);

        return create(data, 0);
    }

    public static Header create(byte[] data, int offset) {
        return new Header(data, offset);
    }

    public static byte setCompressed(byte bits, boolean compressed) {
        return compressed ? bitOn(bits, COMPRESS_BIT) : bitOff(bits, COMPRESS_BIT);
    }

    public static byte setHaveHMAC(byte bits, boolean haveHMAC) {
        return haveHMAC ? bitOn(bits, HMAC_BIT) : bitOff(bits, HMAC_BIT);
    }

    private static byte bitOn(byte bits, int position) {
        return (byte) (bits | (1 << position));
    }

    private static byte bitOff(byte bits, int position) {
        return (byte) (bits & ~(1 << position));
    }

    private static boolean isBitOn(byte bits, int position) {
        return ((bits >> position) & 1) == 1;
    }

    // private constructor... construction is done via factory methods...
    private Header(byte[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    public int size() {
        return data[offset + SIZE_POSITION];
    }

    public boolean isCompressed() {
        return isBitOn(getFlags(), COMPRESS_BIT);
    }

    public boolean haveHMAC() {
        return isBitOn(getFlags(), HMAC_BIT);
    }

    public byte getFlags() {
        return data[offset + FLAGS_POSITION];
    }

    /**
     * Saves the header bytes in the provided buffer at specified offset.
     */
    public void store(byte[] output, int outputOffset, byte flags) {
        System.arraycopy(data, offset, output, outputOffset, size());
        output[outputOffset + FLAGS_POSITION] = flags;
    }

    public String getKeyName() {

        try {
            return new String(data, offset + KEY_NAME_OFFSET, size() - KEY_NAME_OFFSET, KEY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new CayenneCryptoException("Can't decode with " + KEY_NAME_CHARSET, e);
        }
    }
}
