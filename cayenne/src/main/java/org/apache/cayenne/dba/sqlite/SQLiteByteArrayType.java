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
package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.ExtendedType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @since 3.0
 */
class SQLiteByteArrayType implements ExtendedType<byte[]> {

    @Override
    public String getClassName() {
        return "byte[]";
    }

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            byte[] val,
            int pos,
            int type,
            int scale) throws Exception {

        if (val != null) {
            st.setBytes(pos, val);
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

    @Override
    public byte[] materializeObject(ResultSet rs, int index, int type) throws Exception {
        return rs.getBytes(index);
    }

    @Override
    public byte[] materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        return rs.getBytes(index);
    }

    @Override
    public String toString(byte[] value) {
        if (value == null) {
            return "NULL";
        }

        StringBuilder buffer = new StringBuilder();
        ByteArrayType.logBytes(buffer, value);
        return buffer.toString();
    }
}