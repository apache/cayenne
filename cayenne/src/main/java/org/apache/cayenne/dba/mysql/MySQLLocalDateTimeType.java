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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.types.ExtendedType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

/**
 * An extended type for DST-safe LocalDateTime handling that bypasses timezone resolution by reading and writing
 * timestamps directly as LocalDateTime.
 *
 * @since 5.0
 */
// TODO: only used by MySQL now. Test compatibility on other engines, and make it a universal approach
class MySQLLocalDateTimeType implements ExtendedType<LocalDateTime> {

    @Override
    public String getClassName() {
        return LocalDateTime.class.getName();
    }

    @Override
    public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        return rs.getObject(index, LocalDateTime.class);
    }

    @Override
    public LocalDateTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return rs.getObject(index, LocalDateTime.class);
    }

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            LocalDateTime val,
            int pos,
            int type,
            int precision) throws Exception {

        if (val == null) {
            st.setNull(pos, type);
        } else {
            st.setObject(pos, val, type);
        }
    }

    @Override
    public String toString(LocalDateTime value) {
        return String.valueOf(value);
    }
}
