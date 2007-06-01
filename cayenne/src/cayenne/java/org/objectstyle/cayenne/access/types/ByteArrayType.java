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
package org.objectstyle.cayenne.access.types;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.validation.BeanValidationFailure;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * @author Andrei Adamchik
 */
public class ByteArrayType extends AbstractType {
    private static Logger logObj = Logger.getLogger(ByteArrayType.class);

	private static final int BUF_SIZE = 8 * 1024;
	private static final byte[] EMPTY_BYTES = new byte[0];

	protected boolean trimmingBytes;
	protected boolean usingBlobs;

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

	public String getClassName() {
		return "byte[]";
	}
    
    /**
     * Validates byte[] property.
     * 
     * @since 1.1
     */
    public boolean validateProperty(
        Object source,
        String property,
        Object value,
        DbAttribute dbAttribute,
        ValidationResult validationResult) {

        if (!(value instanceof byte[])) {
            return true;
        }

        if (dbAttribute.getMaxLength() <= 0) {
            return true;
        }

        byte[] bytes = (byte[]) value;
        if (bytes.length > dbAttribute.getMaxLength()) {
            String message =
                "\""
                    + property
                    + "\" exceeds maximum allowed length ("
                    + dbAttribute.getMaxLength()
                    + " bytes): "
                    + bytes.length;
            validationResult.addFailure(
                new BeanValidationFailure(source, property, message));
            return false;
        }

        return true;
    }

	public Object materializeObject(ResultSet rs, int index, int type)
		throws Exception {

		byte[] bytes = null;

		if (type == Types.BLOB) {
			bytes =
				(isUsingBlobs())
					? readBlob(rs.getBlob(index))
					: readBinaryStream(rs, index);
		} else {
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
				throw new CayenneException("Binary streams are not supported in stored procedure parameters.");
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

	public void setJdbcObject(
		PreparedStatement st,
		Object val,
		int pos,
		int type,
		int precision)
		throws Exception {

		// if this is a BLOB column, set the value as "bytes"
		// instead. This should work with most drivers
		if (type == Types.BLOB) {
			st.setBytes(pos, (byte[]) val);
		} else {
            try {
			super.setJdbcObject(st, val, pos, type, precision);
            }
            catch(Exception ex) {
                logObj.warn("bad type: " + TypesMapping.getSqlNameByType(type), ex);
                throw ex;
            }
		}
	}

	protected byte[] readBlob(Blob blob) throws IOException, SQLException {
		if (blob == null) {
			return null;
		}

		// sanity check on size
		if (blob.length() > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
				"BLOB is too big to be read as byte[] in memory: "
					+ blob.length());
		}

		int size = (int) blob.length();
		if(size == 0) {
			return EMPTY_BYTES;
		}
		
		int bufSize = (size < BUF_SIZE) ? size : BUF_SIZE;
		InputStream in = blob.getBinaryStream();
		return (in != null)
			? readValueStream(
				new BufferedInputStream(in, bufSize),
				size,
				bufSize)
			: null;
	}

	protected byte[] readBinaryStream(ResultSet rs, int index)
		throws IOException, SQLException {
		InputStream in = rs.getBinaryStream(index);
		return (in != null) ? readValueStream(in, -1, BUF_SIZE) : null;
	}

	protected byte[] readValueStream(
		InputStream in,
		int streamSize,
		int bufSize)
		throws IOException {

		byte[] buf = new byte[bufSize];
		int read;
		ByteArrayOutputStream out =
			(streamSize > 0)
				? new ByteArrayOutputStream(streamSize)
				: new ByteArrayOutputStream();

		try {
			while ((read = in.read(buf, 0, bufSize)) >= 0) {
				out.write(buf, 0, read);
			}
			return out.toByteArray();
		} finally {
			in.close();
		}
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
