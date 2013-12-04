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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.cayenne.CayenneException;

/**
 * Handles <code>java.lang.String</code>, mapping it as either of JDBC types - CLOB or
 * (VAR)CHAR. Can be configured to trim trailing spaces.
 */
public class CharType implements ExtendedType {

    private static final int BUF_SIZE = 8 * 1024;

    protected boolean trimmingChars;
    protected boolean usingClobs;

    public CharType(boolean trimingChars, boolean usingClobs) {
        this.trimmingChars = trimingChars;
        this.usingClobs = usingClobs;
    }

    /**
     * Returns "java.lang.String".
     */
    @Override
    public String getClassName() {
        return String.class.getName();
    }

    /** Return trimmed string. */
    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {

        String val = null;

        // CLOB handling
        if (type == Types.CLOB) {
            val = (isUsingClobs()) ? readClob(rs.getClob(index)) : readCharStream(
                    rs,
                    index);
        }
        else {

            val = rs.getString(index);

            // trim CHAR type
            if (val != null && type == Types.CHAR && isTrimmingChars()) {
                val = rtrim(val);
            }
        }

        return val;
    }

    /** Return trimmed string. */
    @Override
    public Object materializeObject(CallableStatement cs, int index, int type)
            throws Exception {

        String val = null;

        // CLOB handling
        if (type == Types.CLOB) {
            if (!isUsingClobs()) {
                throw new CayenneException(
                        "Character streams are not supported in stored procedure parameters.");
            }

            val = readClob(cs.getClob(index));
        }
        else {

            val = cs.getString(index);

            // trim CHAR type
            if (val != null && type == Types.CHAR && isTrimmingChars()) {
                val = rtrim(val);
            }
        }

        return val;
    }

    /** Trim right spaces. */
    protected String rtrim(String value) {
        int end = value.length() - 1;
        int count = end;
        while ((end >= 0) && (value.charAt(end) <= ' ')) {
            end--;
        }
        return (end == count) ? value : value.substring(0, end + 1);
    }

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            Object value,
            int pos,
            int type,
            int scale) throws Exception {

        // if this is a CLOB column, set the value as "String"
        // instead. This should work with most drivers
        if (type == Types.CLOB) {
            st.setString(pos, (String) value);
        }
        else if (scale != -1) {
            st.setObject(pos, value, type, scale);
        }
        else {
            st.setObject(pos, value, type);
        }
    }

    protected String readClob(Clob clob) throws IOException, SQLException {
        if (clob == null) {
            return null;
        }

        // sanity check on size
        if (clob.length() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "CLOB is too big to be read as String in memory: " + clob.length());
        }

        int size = (int) clob.length();
        if (size == 0) {
            return "";
        }

        return clob.getSubString(1, size);
    }

    protected String readCharStream(ResultSet rs, int index) throws IOException,
            SQLException {
        Reader in = rs.getCharacterStream(index);

        return (in != null) ? readValueStream(in, -1, BUF_SIZE) : null;
    }

    protected String readValueStream(Reader in, int streamSize, int bufSize)
            throws IOException {
        char[] buf = new char[bufSize];
        int read;
        StringWriter out = (streamSize > 0)
                ? new StringWriter(streamSize)
                : new StringWriter();

        try {
            while ((read = in.read(buf, 0, bufSize)) >= 0) {
                out.write(buf, 0, read);
            }
            return out.toString();
        }
        finally {
            in.close();
        }
    }

    /**
     * Returns <code>true</code> if 'materializeObject' method should trim trailing spaces
     * from the CHAR columns. This addresses an issue with some JDBC drivers (e.g.
     * Oracle), that return Strings for CHAR columsn padded with spaces.
     */
    public boolean isTrimmingChars() {
        return trimmingChars;
    }

    public void setTrimmingChars(boolean trimingChars) {
        this.trimmingChars = trimingChars;
    }

    public boolean isUsingClobs() {
        return usingClobs;
    }

    public void setUsingClobs(boolean usingClobs) {
        this.usingClobs = usingClobs;
    }
}
