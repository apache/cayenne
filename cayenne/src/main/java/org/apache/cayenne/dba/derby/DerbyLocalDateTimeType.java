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

import org.apache.cayenne.access.types.LocalDateTimeType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Derby JDBC driver does not support {@code java.time} in {@code getObject} / {@code setObject}, so the value goes
 * through {@link Timestamp}. Its fields are computed in a UTC calendar rather than in the JVM default zone, so the
 * wall-clock value is preserved even when it falls into a DST gap of the default zone.
 *
 * @since 5.0
 */
class DerbyLocalDateTimeType extends LocalDateTimeType {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Override
    public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        return fromTimestamp(rs.getTimestamp(index, Calendar.getInstance(UTC)));
    }

    @Override
    public LocalDateTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return fromTimestamp(rs.getTimestamp(index, Calendar.getInstance(UTC)));
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDateTime value, int pos, int type, int scale)
            throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setTimestamp(pos, Timestamp.from(value.toInstant(ZoneOffset.UTC)), Calendar.getInstance(UTC));
        }
    }

    private static LocalDateTime fromTimestamp(Timestamp value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }
}
