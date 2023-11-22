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
import java.sql.Types;
import java.time.Duration;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;

public class DurationType implements ExtendedType<Duration> {

    @Override
    public String getClassName() {
        return Duration.class.getName();
    }

    protected Object convertToJdbcObject(Duration val, int type) {
        if(type == Types.INTEGER) {
            return Long.valueOf(val.toMillis()).intValue();
        } else if(type == Types.NUMERIC) {
            return val.toMillis();
        } else if(type == Types.DECIMAL) {
            return val.toMillis();
        } else if (type == Types.BIGINT) {
            return val.toMillis();
        } else if (type == Types.VARCHAR) {
            return val.toString();
        } else if(type == Types.LONGVARCHAR) {
            return val.toString();
        } else {
            throw new IllegalArgumentException(
                    "Only INTEGER, NUMERIC, DECIMAL, BIGINT, VARCHAR, LONGVARCHAR " +
                            "can be mapped as '" + getClassName()
                            + "', got " + TypesMapping.getSqlNameByType(type));
        }
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, Duration value, int pos, int type, int scale) throws Exception {
        if(value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setObject(pos, convertToJdbcObject(value, type), type);
        }
    }

    @Override
    public Duration materializeObject(ResultSet rs, int index, int type) throws Exception {
        Duration val = null;
        switch(type) {
            case Types.INTEGER:
                val = Duration.ofMillis(rs.getInt(index));
                break;
            case Types.NUMERIC:
                val = Duration.ofMillis(rs.getBigDecimal(index).longValue());
                break;
            case Types.DECIMAL:
                val = Duration.ofMillis(rs.getBigDecimal(index).longValue());
                break;
            case Types.BIGINT:
                val = Duration.ofMillis(rs.getLong(index));
                break;
            case Types.VARCHAR:
                val = Duration.parse(rs.getString(index));
                break;
            case Types.LONGVARCHAR:
                val = Duration.parse(rs.getString(index));
                break;
        }

        if(rs.wasNull()) {
            return null;
        } else if(val != null) {
            return val;
        } else {
            throw new CayenneRuntimeException("Can't materialize " + rs.getObject(index) + " of type: " + type);
        }
    }

    @Override
    public Duration materializeObject(CallableStatement rs, int index, int type) throws Exception {
        Duration val = null;
        switch(type) {
            case Types.INTEGER:
                val = Duration.ofMillis(rs.getInt(index));
                break;
            case Types.NUMERIC:
                val = Duration.ofMillis(rs.getBigDecimal(index).longValue());
                break;
            case Types.DECIMAL:
                val = Duration.ofMillis(rs.getBigDecimal(index).longValue());
                break;
            case Types.BIGINT:
                val = Duration.ofMillis(rs.getLong(index));
                break;
            case Types.VARCHAR:
                val = Duration.parse(rs.getString(index));
                break;
            case Types.LONGVARCHAR:
                val = Duration.parse(rs.getString(index));
                break;
        }

        if(rs.wasNull()) {
            return null;
        } else if(val != null) {
            return val;
        } else {
            throw new CayenneRuntimeException("Can't materialize " + rs.getObject(index) + " of type: " + type);
        }
    }

    @Override
    public String toString(Duration value) {
        if(value == null) {
            return "NULL";
        }

        return "\'" + value.toString() + "\'";
    }
}
