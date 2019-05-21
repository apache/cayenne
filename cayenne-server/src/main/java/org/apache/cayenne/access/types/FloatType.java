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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @since 3.0
 */
public class FloatType implements ExtendedType<Float> {

    @Override
    public String getClassName() {
        return Float.class.getName();
    }

    @Override
    public Float materializeObject(ResultSet rs, int index, int type) throws Exception {
        float f = rs.getFloat(index);
        return rs.wasNull() ? null : f;
    }

    @Override
    public Float materializeObject(CallableStatement rs, int index, int type) throws Exception {
        float f = rs.getFloat(index);
        return rs.wasNull() ? null : f;
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Float value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setFloat(pos, value);
        }
    }

    @Override
    public String toString(Float value) {
        if (value == null) {
            return "NULL";
        }

        return value.toString();
    }
}
