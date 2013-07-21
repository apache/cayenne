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
package org.apache.cayenne.dba.sqlite;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.cayenne.access.types.CalendarType;
import org.apache.cayenne.access.types.ExtendedType;

/**
 * @since 3.0
 */
class SQLiteCalendarType implements ExtendedType {

    protected ExtendedType delegateCalendarType;
    protected ExtendedType delegateDateType;

    public <T extends Calendar> SQLiteCalendarType(Class<T> calendarClass) {
        this.delegateCalendarType = new CalendarType<T>(calendarClass);
        this.delegateDateType = new SQLiteDateType();
    }

    public String getClassName() {
        return delegateCalendarType.getClassName();
    }

    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {

        Date date = (Date) delegateDateType.materializeObject(rs, index, type);
        if (date == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {
        Date date = (Date) delegateDateType.materializeObject(rs, index, type);
        if (date == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {
        delegateCalendarType.setJdbcObject(statement, value, pos, type, precision);
    }

}
