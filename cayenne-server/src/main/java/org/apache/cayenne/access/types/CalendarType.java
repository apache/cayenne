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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;

/**
 * ExtendedType that handles {@link java.util.Calendar} fields.
 * 
 * @since 3.0
 */
public class CalendarType<T extends Calendar> implements ExtendedType {

    protected Class<T> calendarClass;

    public CalendarType(Class<T> calendarClass) {
        if (calendarClass == null) {
            throw new IllegalArgumentException("Null calendar class");
        }

        if (!Calendar.class.isAssignableFrom(calendarClass)) {
            throw new IllegalArgumentException(
                    "Must be a java.util.Calendar or a subclass: " + calendarClass);
        }

        this.calendarClass = calendarClass;
    }

    @Override
    public String getClassName() {
        return calendarClass.getName();
    }

    @Override
    public Object materializeObject(ResultSet rs, int index, int type) throws Exception {

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
                // here the driver can "surprise" us
                // check the type of returned value...
                Object object = rs.getObject(index);

                if (object != null && !(object instanceof Date)) {
                    throw new CayenneRuntimeException(
                            "Expected an instance of java.util.Date, instead got "
                                    + object.getClass().getName()
                                    + ", column index: "
                                    + index);
                }

                val = (Date) object;
                break;
        }

        if (val == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(val);
        return calendar;
    }

    @Override
    public Object materializeObject(CallableStatement rs, int index, int type)
            throws Exception {
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
                // here the driver can "surprise" us
                // check the type of returned value...
                Object object = rs.getObject(index);

                if (object != null && !(object instanceof Date)) {
                    throw new CayenneRuntimeException(
                            "Expected an instance of java.util.Date, instead got "
                                    + object.getClass().getName()
                                    + ", column index: "
                                    + index);
                }

                val = (Date) object;
                break;
        }

        if (val == null) {
            return null;
        }

        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(val);
        return calendar;
    }

    @Override
    public void setJdbcObject(
            PreparedStatement statement,
            Object value,
            int pos,
            int type,
            int precision) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        }
        else if (value instanceof Calendar) {

            Calendar calendar = (Calendar) value;
            statement.setObject(pos, convertToJdbcObject(calendar, type));
        }
        else {
            throw new IllegalArgumentException("Expected java.util.Calendar, got "
                    + value.getClass().getName());
        }
    }

    protected Object convertToJdbcObject(Calendar value, int type) throws Exception {
        Calendar calendar = value;

        if (type == Types.DATE)
            return new java.sql.Date(calendar.getTimeInMillis());
        else if (type == Types.TIME)
            return new java.sql.Time(calendar.getTimeInMillis());
        else if (type == Types.TIMESTAMP)
            return new java.sql.Timestamp(calendar.getTimeInMillis());
        else
            throw new IllegalArgumentException(
                    "Only DATE, TIME or TIMESTAMP can be mapped as '"
                            + getClassName()
                            + "', got "
                            + TypesMapping.getSqlNameByType(type));
    }

}
