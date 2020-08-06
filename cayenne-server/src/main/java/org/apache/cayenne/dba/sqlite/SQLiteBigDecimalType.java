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

import org.apache.cayenne.access.types.ExtendedType;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @since 3.0
 */
class SQLiteBigDecimalType implements ExtendedType<BigDecimal> {

    @Override
    public String getClassName() {
        return BigDecimal.class.getName();
    }

    @Override
    public BigDecimal materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
        // BigDecimals are not supported by the zentus driver... in addition the driver
        // throws an NPE on 'getDouble' if the value is null, and also there are rounding
        // errors. So will read it as a String...
        String string = rs.getString(index);
        return string == null ? null : new BigDecimal(string);
    }

    @Override
    public BigDecimal materializeObject(ResultSet rs, int index, int type) throws Exception {
        // BigDecimals are not supported by the zentus driver... in addition the driver
        // throws an NPE on 'getDouble' if the value is null, and also there are rounding
        // errors. So will read it as a String...
        String string = rs.getString(index);
        return string == null ? null : new BigDecimal(string);
    }

    @Override
    public void setJdbcObject(
            PreparedStatement st,
            BigDecimal val,
            int pos,
            int type,
            int scale) throws Exception {

        if (scale != -1) {
            st.setObject(pos, val, type, scale);
        }
        else {
            st.setObject(pos, val, type);
        }
    }

    @Override
    public String toString(BigDecimal value) {
        if (value == null) {
            return "NULL";
        }

        return value.toString();
    }
}
