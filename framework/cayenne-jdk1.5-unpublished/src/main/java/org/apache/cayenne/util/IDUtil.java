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

package org.apache.cayenne.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * helper class to generate pseudo-GUID sequences.
 * 
 */
public class IDUtil {

    private static final int BITMASK_0 = 0xff;
    private static final int BITMASK_1 = 0xff << 8;
    private static final int BITMASK_2 = 0xff << 16;
    private static final int BITMASK_3 = 0xff << 24;
    private static final int BITMASK_4 = 0xff << 32;
    private static final int BITMASK_5 = 0xff << 40;
    private static final int BITMASK_6 = 0xff << 48;
    private static final int BITMASK_7 = 0xff << 56;

    // this id sequence needs to be long enough to feel
    // the gap within the same timestamp millisecond
    private static volatile int currentId;

    private static MessageDigest md;
    private static byte[] ipAddress;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e) {
            throw new CayenneRuntimeException("Can't initialize MessageDigest.", e);
        }

        try {
            ipAddress = java.net.InetAddress.getLocalHost().getAddress();
        }
        catch (UnknownHostException e) {
            // use loopback interface
            ipAddress = new byte[] {
                    127, 0, 0, 1
            };
        }
    }

    /**
     * Prints a byte value to a StringBuffer as a double digit hex value.
     * 
     * @since 1.2 Since 3.0 signature has changed to take Appendable argument.
     */
    public static void appendFormattedByte(Appendable buffer, byte byteValue) {
        final String digits = "0123456789ABCDEF";

        try {
            buffer.append(digits.charAt((byteValue >>> 4) & 0xF));
            buffer.append(digits.charAt(byteValue & 0xF));
        }
        catch (IOException e) {
            throw new CayenneRuntimeException("Error appending data to buffer", e);
        }
    }

    /**
     * @param length the length of returned byte[]
     * @return A pseudo-unique byte array of the specified length. Length must be at least
     *         16 bytes, or an exception is thrown.
     * @since 1.0.2
     */
    public synchronized static byte[] pseudoUniqueByteSequence(int length) {
        if (length < 16) {
            throw new IllegalArgumentException(
                    "Can't generate unique byte sequence shorter than 16 bytes: "
                            + length);
        }

        if (length == 16) {
            return pseudoUniqueByteSequence16();
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i <= length - 16; i += 16) {
            byte[] nextSequence = pseudoUniqueByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, i, 16);
        }

        // leftovers?
        int leftoverLen = length % 16;
        if (leftoverLen > 0) {
            byte[] nextSequence = pseudoUniqueByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, length - leftoverLen, leftoverLen);
        }

        return bytes;
    }

    public synchronized static byte[] pseudoUniqueSecureByteSequence(int length) {
        if (length < 16) {
            throw new IllegalArgumentException(
                    "Can't generate unique byte sequence shorter than 16 bytes: "
                            + length);
        }

        if (length == 16) {
            return pseudoUniqueSecureByteSequence16();
        }

        byte[] bytes = new byte[length];
        for (int i = 0; i <= length - 16; i += 16) {
            byte[] nextSequence = pseudoUniqueSecureByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, i, 16);
        }

        // leftovers?
        int leftoverLen = length % 16;
        if (leftoverLen > 0) {
            byte[] nextSequence = pseudoUniqueSecureByteSequence16();
            System.arraycopy(nextSequence, 0, bytes, length - leftoverLen, leftoverLen);
        }

        return bytes;
    }

    public static final byte[] pseudoUniqueByteSequence8() {
        byte[] bytes = new byte[8];

        // bytes 0..2 - incrementing #
        // bytes 3..5 - timestamp high bytes
        // bytes 6..7 - IP address

        int nextInt = nextInt();

        bytes[0] = (byte) ((nextInt & (0xff << 16)) >>> 16);
        bytes[1] = (byte) ((nextInt & (0xff << 8)) >>> 8);
        bytes[2] = (byte) (nextInt & 0xff);

        // append 3 high bytes of timestamp

        long t = System.currentTimeMillis();

        bytes[3] = (byte) ((t & BITMASK_2) >>> 16);
        bytes[4] = (byte) ((t & BITMASK_1) >>> 8);
        bytes[5] = (byte) (t & BITMASK_0);

        // append 2 last bytes of IP address
        System.arraycopy(ipAddress, 2, bytes, 6, 2);
        return bytes;
    }

    /**
     * @return A pseudo unique 16-byte array.
     */
    public static final byte[] pseudoUniqueByteSequence16() {
        byte[] bytes = new byte[16];

        // bytes 0..3 - incrementing #
        // bytes 4..11 - timestamp
        // bytes 12..15 - IP address

        int nextInt = nextInt();

        bytes[0] = (byte) ((nextInt & BITMASK_3) >>> 24);
        bytes[1] = (byte) ((nextInt & BITMASK_2) >>> 16);
        bytes[2] = (byte) ((nextInt & BITMASK_1) >>> 8);
        bytes[3] = (byte) (nextInt & BITMASK_0);

        long t = System.currentTimeMillis();

        bytes[4] = (byte) ((t & BITMASK_7) >>> 56);
        bytes[5] = (byte) ((t & BITMASK_6) >>> 48);
        bytes[6] = (byte) ((t & BITMASK_5) >>> 40);
        bytes[7] = (byte) ((t & BITMASK_4) >>> 32);
        bytes[8] = (byte) ((t & BITMASK_3) >>> 24);
        bytes[9] = (byte) ((t & BITMASK_2) >>> 16);
        bytes[10] = (byte) ((t & BITMASK_1) >>> 8);
        bytes[11] = (byte) (t & BITMASK_0);

        System.arraycopy(ipAddress, 0, bytes, 12, 4);
        return bytes;
    }

    /**
     * @return A pseudo unique digested 16-byte array.
     */
    public static byte[] pseudoUniqueSecureByteSequence16() {
        byte[] bytes = pseudoUniqueByteSequence16();

        synchronized (md) {
            return md.digest(bytes);
        }
    }

    private static final int nextInt() {
        if (currentId == Integer.MAX_VALUE) {
            currentId = 0;
        }

        return currentId++;
    }

    private IDUtil() {
    }
}
