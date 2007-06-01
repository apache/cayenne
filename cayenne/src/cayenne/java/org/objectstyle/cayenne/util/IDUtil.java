/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.objectstyle.cayenne.CayenneRuntimeException;

/**
 * helper class to generate pseudo-GUID sequences.
 *  
 * @author Andrei Adamchik
 */
public class IDUtil {

    private static volatile long currentId = Long.MIN_VALUE;
    private static MessageDigest md;
    private static byte[] ipAddress;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new CayenneRuntimeException("Can't initialize MessageDigest.", e);
        }

        try {
            ipAddress = java.net.InetAddress.getLocalHost().getAddress();
        } catch (UnknownHostException e) {
            // use loopback interface
            ipAddress = new byte[] { 127, 0, 0, 1 };
        }
    }

    /**
      * 
      * @param length the length of returned byte[]
      * @return A pseudo-unique byte array of the specified length. Length must be at least
      * 16 bytes, or an exception is thrown. 
      * 
      * @since 1.0.2
      */
    public synchronized static byte[] pseudoUniqueByteSequence(int length) {
        if (length < 16) {
            throw new IllegalArgumentException(
                "Can't generate unique byte sequence shorter than 16 bytes: " + length);
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

    /**
     * @return A pseudo unique 16-byte array.
     */
    public static byte[] pseudoUniqueByteSequence16() {
        byte[] bytes = new byte[20];

        appendLongBytes(bytes, 0, System.currentTimeMillis());
        appendLongBytes(bytes, 8, currentId++);
        System.arraycopy(ipAddress, 0, bytes, 16, ipAddress.length);

        // spend some time so that the next call would return the different timestamp
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            // ignoring...
        }

        return md.digest(bytes);
    }
    

    private static void appendLongBytes(byte[] bytes, int offset, long value) {
        for (int i = 0; i < 8; ++i) {
            int off = (7 - i) * 8;
            bytes[i + offset] = (byte) ((value & (0xff << off)) >>> off);
        }
    }

    private IDUtil() {
    }

}
