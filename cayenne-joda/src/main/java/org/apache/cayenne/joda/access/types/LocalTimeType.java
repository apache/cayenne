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

package org.apache.cayenne.joda.access.types;

import org.apache.cayenne.access.types.ExtendedType;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Handles <code>org.joda.time.LocalTime</code> type mapping.
 *
 * @since 4.0
 */
public class LocalTimeType implements ExtendedType<LocalTime> {

    private static final LocalDate EPOCH = new LocalDate(0, DateTimeZone.UTC);

    @Override
    public String getClassName() {
        return LocalTime.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalTime value, int pos, int type, int scale) throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
        } else {
            long millis = EPOCH.toDateTime(value).getMillis();

            if (type == Types.TIME) {
                statement.setTime(pos, new Time(millis));
            } else {
                statement.setTimestamp(pos, new Timestamp(millis));
            }
        }
    }

    @Override
    public LocalTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (type == Types.TIME && rs.getTime(index) != null) {
            return new LocalTime(rs.getTime(index).getTime());
        } else if (type == Types.TIMESTAMP && rs.getTimestamp(index) != null) {
            return new LocalTime(rs.getTimestamp(index).getTime());
        } else {
            return null;
        }
    }

    @Override
    public LocalTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        if (type == Types.TIME && rs.getTime(index) != null) {
            return new LocalTime(rs.getTime(index).getTime());
        } else if (type == Types.TIMESTAMP && rs.getTimestamp(index) != null) {
            return new LocalTime(rs.getTimestamp(index).getTime());
        } else {
            return null;
        }
    }

    @Override
    public String toString(LocalTime value) {
        if (value == null) {
            return "NULL";
        }

        return '\'' + value.toString() + '\'';
    }
}
