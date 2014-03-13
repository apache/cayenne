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
 * Handles <code>java.lang.Short</code> type mapping. Can be configured to recast
 * java.lang.Short to java.lang.Integer when binding values to PreparedStatement. This is
 * a workaround for bugs in certain drivers. Drivers that are proven to have issues with
 * short values are Sybase and Oracle (Mac OS X only).
 * 
 * @since 1.0.2
 */
public class ShortType implements ExtendedType {

    protected boolean widenShorts;

    public ShortType(boolean widenShorts) {
        this.widenShorts = widenShorts;
    }

    @Override
    public String getClassName() {
        return Short.class.getName();
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        short s = rs.getShort(index);
        return (rs.wasNull()) ? null : s;
    }

    @Override
    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        short s = st.getShort(index);
        return (st.wasNull()) ? null : s;
    }

    @Override
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

            Short s = (Short) value;
            if (widenShorts) {
                statement.setInt(pos, s.intValue());
            }
            else {
                statement.setShort(pos, s.shortValue());
            }
        }
    }
}
