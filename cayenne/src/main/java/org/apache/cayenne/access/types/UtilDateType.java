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

import org.apache.cayenne.dba.TypesMapping;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;

/**
 * Maps <code>java.util.Date</code> to any of the three database date/time types: TIME,
 * DATE, TIMESTAMP.
 */
public class UtilDateType implements ExtendedType<Date> {

    private final Calendar calendar;
    private final boolean useCalendar;

    /**
     * @since 4.2
     */
    public UtilDateType() {
        this(false);
    }

    /**
     * @since 4.2
     */
    public UtilDateType(boolean useCalendar) {
        this.useCalendar = useCalendar;
        if(this.useCalendar) {
            this.calendar = Calendar.getInstance();
        } else {
            this.calendar = null;
        }
    }

    /**
     * Returns "java.util.Date".
     */
    @Override
    public String getClassName() {
        return Date.class.getName();
    }

    @Override
    public Date materializeObject(ResultSet rs, int index, int type) throws Exception {
        Date val;
        switch (type) {
            case Types.TIMESTAMP:
                val = useCalendar
                        ? rs.getTimestamp(index, calendar)
                        : rs.getTimestamp(index);
                break;
            case Types.DATE:
                val = useCalendar
                        ? rs.getDate(index, calendar)
                        : rs.getDate(index);
                break;
            case Types.TIME:
                val = useCalendar
                        ? rs.getTime(index, calendar)
                        : rs.getTime(index);
                break;
            default:
                val = useCalendar
                        ? rs.getTimestamp(index, calendar)
                        : rs.getTimestamp(index);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public Date materializeObject(CallableStatement cs, int index, int type) throws Exception {
        Date val;
        switch (type) {
            case Types.TIMESTAMP:
                val = useCalendar
                        ? cs.getTimestamp(index, calendar)
                        : cs.getTimestamp(index);
                break;
            case Types.DATE:
                val = useCalendar
                        ? cs.getDate(index, calendar)
                        : cs.getDate(index);
                break;
            case Types.TIME:
                val = useCalendar
                        ? cs.getTime(index, calendar)
                        : cs.getTime(index);
                break;
            default:
                val = useCalendar
                        ? cs.getTimestamp(index, calendar)
                        : cs.getTimestamp(index);
                break;
        }

        // return java.util.Date instead of subclass
        return val == null ? null : new Date(val.getTime());
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Date value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        } else {
            if (type == Types.DATE) {
                if(useCalendar) {
                    statement.setDate(pos, new java.sql.Date(value.getTime()), calendar);
                } else {
                    statement.setDate(pos, new java.sql.Date(value.getTime()));
                }
            } else if (type == Types.TIME) {
                Time time = new Time(value.getTime());
                if(useCalendar) {
                    statement.setTime(pos, time, calendar);
                } else {
                    statement.setTime(pos, time);
                }
            } else if (type == Types.TIMESTAMP) {
                if(useCalendar) {
                    statement.setTimestamp(pos, new java.sql.Timestamp(value.getTime()), calendar);
                } else {
                    statement.setTimestamp(pos, new java.sql.Timestamp(value.getTime()));
                }
            } else {
                throw new IllegalArgumentException(
                        "Only DATE, TIME or TIMESTAMP can be mapped as '" + getClassName()
                                + "', got " + TypesMapping.getSqlNameByType(type));
            }
        }
    }

    @Override
    public String toString(Date value) {
        if (value == null) {
            return "NULL";
        }

        long time = value.getTime();
        return '\'' + new java.sql.Timestamp(time).toString() + '\'';
    }
}
