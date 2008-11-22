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
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.SQLException;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * A Clob implementation that stores contents in memory.
 * <p>
 * <i>This implementation is based on jdbcClob from HSQLDB (copyright HSQL Development
 * Group).</i>
 * </p>
 * 
 * @since 1.2
 */
public class MemoryClob implements Clob {

    volatile String data;

    /**
     * Constructs a new jdbcClob object wrapping the given character sequence.
     * <p>
     * This constructor is used internally to retrieve result set values as Clob objects,
     * yet it must be public to allow access from other packages. As such (in the interest
     * of efficiency) this object maintains a reference to the given String object rather
     * than making a copy and so it is gently suggested (in the interest of effective
     * memory management) that extenal clients using this constructor either take pause to
     * consider the implications or at least take care to provide a String object whose
     * internal character buffer is not much larger than required to represent the value.
     * 
     * @param data the character sequence representing the Clob value
     * @throws SQLException if the argument is null
     */
    public MemoryClob(String data) {

        if (data == null) {
            throw new CayenneRuntimeException("Null data");
        }

        this.data = data;
    }

    /**
     * Retrieves the number of characters in the <code>CLOB</code> value designated by
     * this <code>Clob</code> object.
     * 
     * @return length of the <code>CLOB</code> in characters
     * @exception SQLException if there is an error accessing the length of the
     *                <code>CLOB</code> value
     */
    public long length() throws SQLException {

        final String ldata = data;

        return ldata.length();
    }

    /**
     * Retrieves a copy of the specified substring in the <code>CLOB</code> value
     * designated by this <code>Clob</code> object. The substring begins at position
     * <code>pos</code> and has up to <code>length</code> consecutive characters.
     */
    public String getSubString(long pos, final int length) throws SQLException {

        final String ldata = data;
        final int dlen = ldata.length();

        pos--;

        if (pos < 0 || pos > dlen) {
            throw new CayenneRuntimeException("Invalid position: " + (pos + 1L));
        }

        if (length < 0 || length > dlen - pos) {
            throw new CayenneRuntimeException("Invalid length: " + length);
        }

        if (pos == 0 && length == dlen) {
            return ldata;
        }

        return ldata.substring((int) pos, (int) pos + length);
    }

    /**
     * Retrieves the <code>CLOB</code> value designated by this <code>Clob</code>
     * object as a <code>java.io.Reader</code> object (or as a stream of characters).
     * 
     * @return a <code>java.io.Reader</code> object containing the <code>CLOB</code>
     *         data
     * @exception SQLException if there is an error accessing the <code>CLOB</code>
     *                value
     */
    public java.io.Reader getCharacterStream() throws SQLException {

        final String ldata = data;

        return new StringReader(ldata);
    }

    /**
     * Retrieves the <code>CLOB</code> value designated by this <code>Clob</code>
     * object as an ascii stream.
     * 
     * @return a <code>java.io.InputStream</code> object containing the
     *         <code>CLOB</code> data
     * @exception SQLException if there is an error accessing the <code>CLOB</code>
     *                value
     */
    public java.io.InputStream getAsciiStream() throws SQLException {

        final String ldata = data;

        return new AsciiStringInputStream(ldata);
    }

    /**
     * Retrieves the character position at which the specified substring
     * <code>searchstr</code> appears in the SQL <code>CLOB</code> value represented
     * by this <code>Clob</code> object. The search begins at position
     * <code>start</code>.
     * 
     * @param searchstr the substring for which to search
     * @param start the position at which to begin searching; the first position is 1
     * @return the position at which the substring appears or -1 if it is not present; the
     *         first position is 1
     * @exception SQLException if there is an error accessing the <code>CLOB</code>
     *                value
     */
    public long position(final String searchstr, long start) throws SQLException {

        if (searchstr == null || start > Integer.MAX_VALUE) {
            return -1;
        }

        final String ldata = data;
        final int pos = ldata.indexOf(searchstr, (int) --start);

        return (pos < 0) ? -1 : pos + 1;
    }

    /**
     * Retrieves the character position at which the specified <code>Clob</code> object
     * <code>searchstr</code> appears in this <code>Clob</code> object. The search
     * begins at position <code>start</code>.
     * 
     * @param searchstr the <code>Clob</code> object for which to search
     * @param start the position at which to begin searching; the first position is 1
     * @return the position at which the <code>Clob</code> object appears or -1 if it is
     *         not present; the first position is 1
     * @exception SQLException if there is an error accessing the <code>CLOB</code>
     *                value
     */
    public long position(final Clob searchstr, long start) throws SQLException {

        if (searchstr == null) {
            return -1;
        }

        final String ldata = data;
        final long dlen = ldata.length();
        final long sslen = searchstr.length();

        // This is potentially much less expensive than materializing a large
        // substring from some other vendor's CLOB. Indeed, we should probably
        // do the comparison piecewise, using an in-memory buffer (or temp-files
        // when available), if it is detected that the input CLOB is very long.
        if (start > dlen - sslen) {
            return -1;
        }

        // by now, we know sslen and start are both < Integer.MAX_VALUE
        String s;

        if (searchstr instanceof MemoryClob) {
            s = ((MemoryClob) searchstr).data;
        }
        else {
            s = searchstr.getSubString(1L, (int) sslen);
        }

        final int pos = ldata.indexOf(s, (int) start);

        return (pos < 0) ? -1 : pos + 1;
    }

    /**
     * Writes the given Java <code>String</code> to the <code>CLOB</code> value that
     * this <code>Clob</code> object designates at the position <code>pos</code>.
     * Calling this method always throws an <code>SQLException</code>.
     */
    public int setString(long pos, String str) throws SQLException {
        throw new CayenneRuntimeException("Not supported");
    }

    /**
     * Writes <code>len</code> characters of <code>str</code>, starting at character
     * <code>offset</code>, to the <code>CLOB</code> value that this
     * <code>Clob</code> represents. Calling this method always throws an
     * <code>SQLException</code>.
     */
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        throw new CayenneRuntimeException("Not supported");
    }

    /**
     * Retrieves a stream to be used to write Ascii characters to the <code>CLOB</code>
     * value that this <code>Clob</code> object represents, starting at position
     * <code>pos</code>.
     * <p>
     * Calling this method always throws an <code>SQLException</code>.
     */
    public java.io.OutputStream setAsciiStream(long pos) throws SQLException {
        throw new CayenneRuntimeException("Not supported");
    }

    /**
     * Retrieves a stream to be used to write a stream of Unicode characters to the
     * <code>CLOB</code> value that this <code>Clob</code> object represents, at
     * position <code>pos</code>.
     * <p>
     * Calling this method always throws an <code>SQLException</code>.
     */
    public java.io.Writer setCharacterStream(long pos) throws SQLException {
        throw new CayenneRuntimeException("Not supported");
    }

    /**
     * Truncates the <code>CLOB</code> value that this <code>Clob</code> designates to
     * have a length of <code>len</code> characters.
     * <p>
     */
    public void truncate(final long len) throws SQLException {

        final String ldata = data;
        final long dlen = ldata.length();
        final long chars = len >> 1;

        if (chars == dlen) {

            // nothing has changed, so there's nothing to be done
        }
        else if (len < 0 || chars > dlen) {
            throw new CayenneRuntimeException("Invalid length: " + len);
        }
        else {

            // use new String() to ensure we get rid of slack
            data = new String(ldata.substring(0, (int) chars));
        }
    }

    class AsciiStringInputStream extends InputStream {

        protected int strOffset = 0;
        protected int charOffset = 0;
        protected int available;
        protected String str;

        public AsciiStringInputStream(String s) {
            str = s;
            available = s.length() * 2;
        }

        public int doRead() throws IOException {

            if (available == 0) {
                return -1;
            }

            available--;

            char c = str.charAt(strOffset);

            if (charOffset == 0) {
                charOffset = 1;

                return (c & 0x0000ff00) >> 8;
            }
            else {
                charOffset = 0;
                strOffset++;
                return c & 0x000000ff;
            }
        }

        @Override
        public int read() throws IOException {
            doRead();
            return doRead();
        }

        @Override
        public int available() throws IOException {
            return available / 2;
        }
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
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
