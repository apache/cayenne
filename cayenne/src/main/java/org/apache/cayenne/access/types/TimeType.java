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
import java.sql.Time;
import java.util.Calendar;

/**
 * @since 3.0
 */
public class TimeType implements ExtendedType<Time> {

    private final Calendar calendar;
    private final boolean useCalendar;

    /**
     * @since 4.2
     */
    public TimeType() {
        this(false);
    }

    /**
     * @since 4.2
     */
    public TimeType(boolean useCalendar) {
        this.useCalendar = useCalendar;
        if(this.useCalendar) {
            this.calendar = Calendar.getInstance();
        } else {
            this.calendar = null;
        }
    }

    @Override
    public String getClassName() {
        return Time.class.getName();
    }

    @Override
    public Time materializeObject(ResultSet rs, int index, int type) throws Exception {
        return useCalendar ? rs.getTime(index, calendar) : rs.getTime(index);
    }

    @Override
    public Time materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return useCalendar ? rs.getTime(index, calendar) : rs.getTime(index);
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Time value,
            int pos,
            int type,
            int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        } else if(useCalendar) {
            statement.setTime(pos, value, calendar);
        } else {
            statement.setTime(pos, value);
        }
    }

    @Override
    public String toString(Time value) {
        if (value == null) {
            return "NULL";
        }

        return '\'' + value.toString() + '\'';
    }
}
