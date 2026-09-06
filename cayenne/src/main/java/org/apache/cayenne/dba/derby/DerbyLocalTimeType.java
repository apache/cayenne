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
package org.apache.cayenne.dba.derby;

import org.apache.cayenne.access.types.LocalTimeType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Derby JDBC driver does not support {@code java.time} in {@code getObject} / {@code setObject}, so the value goes
 * through {@link Time}. Its fields are computed in a UTC calendar rather than in the JVM default zone, so the
 * wall-clock time is preserved regardless of the default zone offset.
 *
 * @since 5.0
 */
class DerbyLocalTimeType extends LocalTimeType {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Override
    public LocalTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        return fromTime(rs.getTime(index, Calendar.getInstance(UTC)));
    }

    @Override
    public LocalTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return fromTime(rs.getTime(index, Calendar.getInstance(UTC)));
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalTime value, int pos, int type, int scale)
            throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            long millis = value.atDate(LocalDate.EPOCH).toInstant(ZoneOffset.UTC).toEpochMilli();
            statement.setTime(pos, new Time(millis), Calendar.getInstance(UTC));
        }
    }

    private static LocalTime fromTime(Time value) {
        // java.sql.Time.toInstant() is unsupported, so go via millis
        return value == null ? null : LocalTime.ofInstant(Instant.ofEpochMilli(value.getTime()), ZoneOffset.UTC);
    }
}
