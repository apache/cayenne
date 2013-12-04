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

package org.apache.cayenne.access.types;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.util.MemoryBlob;

/**
 * Handles <code>byte[]</code>, mapping it as either of JDBC types - BLOB or (VAR)BINARY.
 * Can be configured to trim trailing zero bytes.
 */
public class ByteArrayType implements ExtendedType {

    private static final int BUF_SIZE = 8 * 1024;

    protected boolean trimmingBytes;
    protected boolean usingBlobs;

    /**
     * Strips null bytes from the byte array, returning a potentially smaller array that
     * contains no trailing zero bytes.
     */
    public static byte[] trimBytes(byte[] bytes) {
        int bytesToTrim = 0;
        for (int i = bytes.length - 1; i >= 0; i--) {
            if (bytes[i] != 0) {
                bytesToTrim = bytes.length - 1 - i;
                break;
            }
        }

        if (bytesToTrim == 0) {
            return bytes;
        }

        byte[] dest = new byte[bytes.length - bytesToTrim];
        System.arraycopy(bytes, 0, dest, 0, dest.length);
        return dest;
    }

    public ByteArrayType(boolean trimmingBytes, boolean usingBlobs) {
        this.usingBlobs = usingBlobs;
        this.trimmingBytes = trimmingBytes;
    }

    public String getClassName() {
        return "byte[]";
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {

        byte[] bytes = null;

        if (type == Types.BLOB) {
            bytes = (isUsingBlobs()) ? readBlob(rs.getBlob(index)) : readBinaryStream(
                    rs,
                    index);
        }
        else {
            bytes = rs.getBytes(index);

            // trim BINARY type
            if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
                bytes = trimBytes(bytes);
            }
        }

        return bytes;
    }

    public Object materializeObject(CallableStatement cs, int index, int type)
            throws Exception {

        byte[] bytes = null;

        if (type == Types.BLOB) {
            if (!isUsingBlobs()) {
                throw new CayenneException(
                        "Binary streams are not supported in stored procedure parameters.");
            }
            bytes = readBlob(cs.getBlob(index));
        }
        else {

            bytes = cs.getBytes(index);

            // trim BINARY type
            if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
                bytes = trimBytes(bytes);
            }
        }

        return bytes;
    }

    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int scale) throws Exception {

        // if this is a BLOB column, set the value as "bytes"
        // instead. This should work with most drivers
        if (type == Types.BLOB) {
            if (isUsingBlobs()) {
                st.setBlob(pos, writeBlob((byte[]) val));
            }
            else {
                st.setBytes(pos, (byte[]) val);
            }
        }
        else {
            if (scale != -1) {
                st.setObject(pos, val, type, scale);
            }
            else {
                st.setObject(pos, val, type);
            }
        }
    }

    protected Blob writeBlob(byte[] bytes) {
        return bytes != null ? new MemoryBlob(bytes) : null;
    }

    protected byte[] readBlob(Blob blob) throws IOException, SQLException {
        if (blob == null) {
            return null;
        }

        // sanity check on size
        if (blob.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "BLOB is too big to be read as byte[] in memory: " + blob.length());
        }

        int size = (int) blob.length();
        if (size == 0) {
            return new byte[0];
        }
        
        return blob.getBytes(1, size);
    }

    protected byte[] readBinaryStream(ResultSet rs, int index) throws IOException,
            SQLException {
        InputStream in = rs.getBinaryStream(index);
        return (in != null) ? readValueStream(in, -1, BUF_SIZE) : null;
    }

    protected byte[] readValueStream(InputStream in, int streamSize, int bufSize)
            throws IOException {

        byte[] buf = new byte[bufSize];
        int read;
        ByteArrayOutputStream out = (streamSize > 0) ? new ByteArrayOutputStream(
                streamSize) : new ByteArrayOutputStream();

        try {
            while ((read = in.read(buf, 0, bufSize)) >= 0) {
                out.write(buf, 0, read);
            }
            return out.toByteArray();
        }
        finally {
            in.close();
        }
    }

    /**
     * Returns <code>true</code> if byte columns are handled as BLOBs internally.
     */
    public boolean isUsingBlobs() {
        return usingBlobs;
    }

    public void setUsingBlobs(boolean usingBlobs) {
        this.usingBlobs = usingBlobs;
    }

    public boolean isTrimmingBytes() {
        return trimmingBytes;
    }

    public void setTrimmingBytes(boolean trimingBytes) {
        this.trimmingBytes = trimingBytes;
    }
}
