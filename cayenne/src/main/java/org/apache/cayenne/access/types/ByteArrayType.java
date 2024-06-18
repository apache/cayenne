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

package org.apache.cayenne.access.types;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.IDUtil;
import org.apache.cayenne.util.MemoryBlob;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Handles <code>byte[]</code>, mapping it as either of JDBC types - BLOB or
 * (VAR)BINARY. Can be configured to trim trailing zero bytes.
 */
public class ByteArrayType implements ExtendedType<byte[]> {

	private static final int BUF_SIZE = 8 * 1024;

	protected boolean trimmingBytes;
	protected boolean usingBlobs;

    public static void logBytes(StringBuilder buffer, byte[] bytes) {
        buffer.append("<");

        int len = bytes.length;
        boolean trimming = false;
        if (len > TRIM_VALUES_THRESHOLD) {
            len = TRIM_VALUES_THRESHOLD;
            trimming = true;
        }

        for (int i = 0; i < len; i++) {
            IDUtil.appendFormattedByte(buffer, bytes[i]);
        }

        if (trimming) {
            buffer.append("...");
        }

        buffer.append('>');
    }

	/**
	 * Strips null bytes from the byte array, returning a potentially smaller
	 * array that contains no trailing zero bytes.
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

	@Override
	public String getClassName() {
		return "byte[]";
	}

	@Override
	public byte[] materializeObject(ResultSet rs, int index, int type) throws Exception {

		byte[] bytes = null;

		if (type == Types.BLOB) {
			bytes = (isUsingBlobs()) ? readBlob(rs.getBlob(index)) : readBinaryStream(rs, index);
		} else {
			bytes = rs.getBytes(index);

			// trim BINARY type
			if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
				bytes = trimBytes(bytes);
			}
		}

		return bytes;
	}

	@Override
	public byte[] materializeObject(CallableStatement cs, int index, int type) throws Exception {

		byte[] bytes = null;

		if (type == Types.BLOB) {
			if (!isUsingBlobs()) {
				throw new CayenneRuntimeException("Binary streams are not supported in stored procedure parameters.");
			}
			bytes = readBlob(cs.getBlob(index));
		} else {

			bytes = cs.getBytes(index);

			// trim BINARY type
			if (bytes != null && type == Types.BINARY && isTrimmingBytes()) {
				bytes = trimBytes(bytes);
			}
		}

		return bytes;
	}

	@Override
	public void setJdbcObject(PreparedStatement st, byte[] val, int pos, int type, int scale) throws Exception {

		// if this is a BLOB column, set the value as "bytes"
		// instead. This should work with most drivers
		if (type == Types.BLOB) {
			if (isUsingBlobs()) {
				st.setBlob(pos, writeBlob(val));
			} else {
				st.setBytes(pos, val);
			}
		} else {
			if (scale != -1) {
				st.setObject(pos, val, type, scale);
			} else {
				st.setObject(pos, val, type);
			}
		}
	}

	@Override
	public String toString(byte[] value) {
		if (value == null) {
			return "NULL";
		}

		StringBuilder buffer = new StringBuilder();
		logBytes(buffer, value);
		return buffer.toString();
	}

	protected Blob writeBlob(byte[] bytes) {
		// TODO: should we use Connection.createBlob() instead? (Like Oracle
		// ByteArrayType does)
		return bytes != null ? new MemoryBlob(bytes) : null;
	}

	protected byte[] readBlob(Blob blob) throws IOException, SQLException {
		if (blob == null) {
			return null;
		}

		// sanity check on size
		if (blob.length() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("BLOB is too big to be read as byte[] in memory: " + blob.length());
		}

		int size = (int) blob.length();
		if (size == 0) {
			return new byte[0];
		}

		return blob.getBytes(1, size);
	}

	protected byte[] readBinaryStream(ResultSet rs, int index) throws IOException, SQLException {
		try (InputStream in = rs.getBinaryStream(index);) {
			return (in != null) ? readValueStream(in, -1, BUF_SIZE) : null;
		}
	}

	protected byte[] readValueStream(InputStream in, int streamSize, int bufSize) throws IOException {

		byte[] buf = new byte[bufSize];
		int read;
		ByteArrayOutputStream out = (streamSize > 0) ? new ByteArrayOutputStream(streamSize)
				: new ByteArrayOutputStream();

		while ((read = in.read(buf, 0, bufSize)) >= 0) {
			out.write(buf, 0, read);
		}

		return out.toByteArray();
	}

	/**
	 * Returns <code>true</code> if byte columns are handled as BLOBs
	 * internally.
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
