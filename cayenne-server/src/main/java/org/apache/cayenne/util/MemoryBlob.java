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

package org.apache.cayenne.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A Blob implementation that stores content in memory.
 * <p>
 * <i>This implementation is based on jdbcBlob from HSQLDB (copyright HSQL Development
 * Group).</i>
 * </p>
 * 
 * @since 1.2
 */
public class MemoryBlob implements Blob {

    volatile byte[] data;

    public MemoryBlob() {
        this(new byte[0]);
    }

    /**
     * Constructs a new MemoryBlob instance wrapping the given octet sequence.
     * 
     * @param data the octet sequence representing the Blob value
     * @throws CayenneRuntimeException if the argument is null
     */
    public MemoryBlob(byte[] data) {

        if (data == null) {
            throw new CayenneRuntimeException("Null data");
        }

        this.data = data;
    }

    /**
     * Returns the number of bytes in the <code>BLOB</code> value designated by this
     * <code>Blob</code> object.
     * 
     * @return length of the <code>BLOB</code> in bytes
     * @exception SQLException if there is an error accessing the length of the
     *                <code>BLOB</code>
     */
    public long length() throws SQLException {
        return data.length;
    }

    /**
     * Retrieves all or part of the <code>BLOB</code> value that this <code>Blob</code>
     * object represents, as an array of bytes. This <code>byte</code> array contains up
     * to <code>length</code> consecutive bytes starting at position <code>pos</code>.
     * <p>
     * The official specification is ambiguous in that it does not precisely indicate the
     * policy to be observed when pos &gt; this.length() - length. One policy would be to
     * retrieve the octets from pos to this.length(). Another would be to throw an
     * exception. This implementation observes the later policy.
     * 
     * @param pos the ordinal position of the first byte in the <code>BLOB</code> value to
     *            be extracted; the first byte is at position 1
     * @param length the number of consecutive bytes to be copied
     * @return a byte array containing up to <code>length</code> consecutive bytes from
     *         the <code>BLOB</code> value designated by this <code>Blob</code> object,
     *         starting with the byte at position <code>pos</code>
     * @exception SQLException if there is an error accessing the <code>BLOB</code> value
     */
    public byte[] getBytes(long pos, final int length) throws SQLException {

        final byte[] ldata = data;
        final int dlen = ldata.length;

        pos--;

        if (pos < 0 || pos > dlen) {
            throw new SQLException("Invalid pos: " + (pos + 1));
        }

        if (length < 0 || length > dlen - pos) {
            throw new SQLException("length: " + length);
        }

        final byte[] out = new byte[length];
        System.arraycopy(ldata, (int) pos, out, 0, length);
        return out;
    }

    /**
     * Retrieves the <code>BLOB</code> value designated by this <code>Blob</code> instance
     * as a stream.
     * 
     * @return a stream containing the <code>BLOB</code> data
     * @exception SQLException if there is an error accessing the <code>BLOB</code> value
     */
    public InputStream getBinaryStream() throws SQLException {
        return new ByteArrayInputStream(data);
    }

    /**
     * Retrieves the byte position at which the specified byte array <code>pattern</code>
     * begins within the <code>BLOB</code> value that this <code>Blob</code> object
     * represents. The search for <code>pattern</code> begins at position
     * <code>start</code>.
     * <p>
     * 
     * @param pattern the byte array for which to search
     * @param start the position at which to begin searching; the first position is 1
     * @return the position at which the pattern appears, else -1
     * @exception SQLException if there is an error accessing the <code>BLOB</code>
     */
    public long position(final byte[] pattern, long start) throws SQLException {

        final byte[] ldata = data;
        final int dlen = ldata.length;

        if (start > dlen || pattern == null) {
            return -1;
        }
        else if (start < 1) {
            start = 0;
        }
        else {
            start--;
        }

        final int plen = pattern.length;

        if (plen == 0 || start > dlen - plen) {
            return -1;
        }

        final int stop = dlen - plen;
        final byte b0 = pattern[0];

        outer_loop: for (int i = (int) start; i <= stop; i++) {
            if (ldata[i] != b0) {
                continue;
            }

            int len = plen;
            int doffset = i;
            int poffset = 0;

            while (len-- > 0) {
                if (ldata[doffset++] != pattern[poffset++]) {
                    continue outer_loop;
                }
            }

            return i + 1;
        }

        return -1;
    }

    /**
     * Retrieves the byte position in the <code>BLOB</code> value designated by this
     * <code>Blob</code> object at which <code>pattern</code> begins. The search begins at
     * position <code>start</code>.
     * 
     * @param pattern the <code>Blob</code> object designating the <code>BLOB</code> value
     *            for which to search
     * @param start the position in the <code>BLOB</code> value at which to begin
     *            searching; the first position is 1
     * @return the position at which the pattern begins, else -1
     * @exception SQLException if there is an error accessing the <code>BLOB</code> value
     */
    public long position(final Blob pattern, long start) throws SQLException {

        final byte[] ldata = data;
        final int dlen = ldata.length;

        if (start > dlen || pattern == null) {
            return -1;
        }
        else if (start < 1) {
            start = 0;
        }
        else {
            start--;
        }

        final long plen = pattern.length();

        if (plen == 0 || start > dlen - plen) {
            return -1;
        }

        // by now, we know plen <= Integer.MAX_VALUE
        final int iplen = (int) plen;
        byte[] bap;

        if (pattern instanceof MemoryBlob) {
            bap = ((MemoryBlob) pattern).data;
        }
        else {
            bap = pattern.getBytes(1, iplen);
        }

        final int stop = dlen - iplen;
        final byte b0 = bap[0];

        outer_loop: for (int i = (int) start; i <= stop; i++) {
            if (ldata[i] != b0) {
                continue;
            }

            int len = iplen;
            int doffset = i;
            int poffset = 0;

            while (len-- > 0) {
                if (ldata[doffset++] != bap[poffset++]) {
                    continue outer_loop;
                }
            }

            return i + 1;
        }

        return -1;
    }

    /**
     * Always throws an exception.
     */
    public int setBytes(long pos, byte[] bytes) throws SQLException {
        throw new SQLException("Not supported");
    }

    /**
     * Always throws an exception.
     */
    public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
        throw new SQLException("Not supported");
    }

    /**
     * Always throws an exception.
     */
    public OutputStream setBinaryStream(long pos) throws SQLException {
        throw new SQLException("Not supported");
    }

    /**
     * Truncates the <code>BLOB</code> value that this <code>Blob</code> object represents
     * to be <code>len</code> bytes in length.
     * 
     * @param len the length, in bytes, to which the <code>BLOB</code> value that this
     *            <code>Blob</code> object represents should be truncated
     * @exception SQLException if there is an error accessing the <code>BLOB</code> value
     */
    public void truncate(final long len) throws SQLException {

        final byte[] ldata = data;

        if (len < 0 || len > ldata.length) {
            throw new SQLException("Invalid length: " + Long.toString(len));
        }

        if (len == ldata.length) {
            return;
        }

        byte[] newData = new byte[(int) len];
        System.arraycopy(ldata, 0, newData, 0, (int) len);
        data = newData;
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public void free() throws SQLException {
    }

    /**
     * @since 3.0
     */
    // JDBC 4 compatibility under Java 1.5
    public InputStream getBinaryStream(long arg0, long arg1) throws SQLException {
        return null;
    }
}
