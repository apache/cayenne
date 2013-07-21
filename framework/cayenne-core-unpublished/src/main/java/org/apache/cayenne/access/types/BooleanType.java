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
import java.sql.Types;

/**
 * Handles <code>java.lang.Boolean</code> mapping. Note that "materialize*" methods return
 * either Boolean.TRUE or Boolean.FALSE, instead of creating new Boolean instances using
 * constructor. This makes possible identity comparison such as
 * <code>object.getBooleanProperty() == Boolean.TRUE</code>.
 * 
 * @since 1.2
 */
public class BooleanType implements ExtendedType {

    public String getClassName() {
        return Boolean.class.getName();
    }

    public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision) throws Exception {

        if (val == null) {
            st.setNull(pos, type);
        }
        else if (type == Types.BIT || type == Types.BOOLEAN) {
            boolean flag = Boolean.TRUE.equals(val);
            st.setBoolean(pos, flag);
        }
        else {
            st.setObject(pos, val, type);
        }
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        boolean b = rs.getBoolean(index);
        return (rs.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
    }

    public Object materializeObject(CallableStatement st, int index, int type)
            throws Exception {
        boolean b = st.getBoolean(index);
        return (st.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
    }
}
