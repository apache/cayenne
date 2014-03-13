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
import java.util.Date;

import org.apache.cayenne.dba.TypesMapping;

/**
 * Maps <code>java.util.Date</code> to any of the three database date/time types: TIME,
 * DATE, TIMESTAMP.
 */
public class UtilDateType implements ExtendedType {

    /**
     * Returns "java.util.Date".
     */
    @Override
    public String getClassName() {
        return Date.class.getName();
    }

    protected Object convertToJdbcObject(Object val, int type) throws Exception {
        if (type == Types.DATE)
            return new java.sql.Date(((Date) val).getTime());
        else if (type == Types.TIME)
            return new java.sql.Time(((Date) val).getTime());
        else if (type == Types.TIMESTAMP)
            return new java.sql.Timestamp(((Date) val).getTime());
        else
            throw new IllegalArgumentException(
                    "Only DATE, TIME or TIMESTAMP can be mapped as '"
                            + getClassName()
                            + "', got "
                            + TypesMapping.getSqlNameByType(type));
    }

    @Override
    public Date materializeObject(ResultSet rs, int index, int type) throws Exception {
        Date val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = rs.getTimestamp(index);
                break;
            case Types.DATE:
                val = rs.getDate(index);
                break;
            case Types.TIME:
                val = rs.getTime(index);
                break;
            default:
                val = rs.getTimestamp(index);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public Date materializeObject(CallableStatement cs, int index, int type)
            throws Exception {
        Date val = null;

        switch (type) {
            case Types.TIMESTAMP:
                val = cs.getTimestamp(index);
                break;
            case Types.DATE:
                val = cs.getDate(index);
                break;
            case Types.TIME:
                val = cs.getTime(index);
                break;
            default:
                val = cs.getTimestamp(index);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        }
        else {
            statement.setObject(pos, convertToJdbcObject(value, type), type);
        }
    }
}
