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
 * @author Andrus Adamchik
 * @since 1.0.3
 */
public class ByteType extends AbstractType {

    protected boolean widenBytes;

    public ByteType(boolean widenBytes) {
        this.widenBytes = widenBytes;
    }

    public String getClassName() {
        return Byte.class.getName();
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        byte b = rs.getByte(index);
        return (rs.wasNull()) ? null : new Byte(b);
    }

    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        byte b = st.getByte(index);
        return (st.wasNull()) ? null : new Byte(b);
    }

    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (widenBytes && (val instanceof Byte)) {
            val = Integer.valueOf(((Byte) val).intValue());
        }

        super.setJdbcObject(st, val, pos, type, precision);
    }
}
