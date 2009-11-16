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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Handles <code>java.lang.Byte</code> type mapping. Can be configured to recast
 * java.lang.Byte to java.lang.Integer when binding values to PreparedStatement. This is a
 * workaround for bugs in certain drivers. Drivers that are proven to have issues with
 * byte values are Sybase and Oracle (Mac OS X only).
 * 
 * @since 1.0.3
 */
public class ByteType implements ExtendedType {

    protected boolean widenBytes;

    public ByteType(boolean widenBytes) {
        this.widenBytes = widenBytes;
    }

    public String getClassName() {
        return Byte.class.getName();
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        byte b = rs.getByte(index);
        return (rs.wasNull()) ? null : b;
    }

    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        byte b = st.getByte(index);
        return (st.wasNull()) ? null : b;
    }

    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        }
        else {

            Byte b = (Byte) value;
            if (widenBytes) {
                statement.setInt(pos, b.intValue());
            }
            else {
                statement.setByte(pos, b.byteValue());
            }
        }
    }

}
