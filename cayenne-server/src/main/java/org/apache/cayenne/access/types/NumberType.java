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
 * @since 4.2
 */
public class NumberType implements ExtendedType<Number> {

    @Override
    public String getClassName() {
        return Number.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, Number value, int pos, int type, int scale) throws Exception {
        if(value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setObject(pos, value, type);
        }
    }

    @Override
    public Number materializeObject(ResultSet rs, int index, int type) throws Exception {
        Number val = null;
        switch (type) {
            case Types.SMALLINT:
                val = rs.getShort(index);
                break;
            case Types.INTEGER:
                val = rs.getInt(index);
                break;
            case Types.BIGINT:
                val = rs.getLong(index);
                break;
            case Types.REAL:
            case Types.FLOAT:
                val = rs.getFloat(index);
                break;
            case Types.DOUBLE:
                val = rs.getDouble(index);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                val = rs.getBigDecimal(index);
                break;
            case Types.TINYINT:
                val = rs.getByte(index);
                break;
        }

        return val;
    }

    @Override
    public Number materializeObject(CallableStatement rs, int index, int type) throws Exception {
        Number val = null;
        switch (type) {
            case Types.SMALLINT:
                val = rs.getShort(index);
                break;
            case Types.INTEGER:
                val = rs.getInt(index);
                break;
            case Types.BIGINT:
                val = rs.getLong(index);
                break;
            case Types.REAL:
            case Types.FLOAT:
                val = rs.getFloat(index);
                break;
            case Types.DOUBLE:
                val = rs.getDouble(index);
                break;
            case Types.DECIMAL:
            case Types.NUMERIC:
                val = rs.getBigDecimal(index);
                break;
            case Types.TINYINT:
                val = rs.getByte(index);
                break;
        }

        return val;
    }

    @Override
    public String toString(Number value) {
        if(value == null) {
            return "NULL";
        }

        return value.toString();
    }
}
