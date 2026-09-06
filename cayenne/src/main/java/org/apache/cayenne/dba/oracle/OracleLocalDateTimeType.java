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
package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.types.LocalDateTimeType;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Oracle driver converts a {@link LocalDateTime} passed to {@code setObject} through the JVM default time zone,
 * shifting values that fall into a DST gap. Binding a {@link Timestamp} with a UTC calendar preserves the wall-clock
 * value. Reads use the default {@code getObject} path, which is exact.
 *
 * @since 5.0
 */
class OracleLocalDateTimeType extends LocalDateTimeType {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDateTime value, int pos, int type, int scale)
            throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setTimestamp(pos, Timestamp.from(value.toInstant(ZoneOffset.UTC)), Calendar.getInstance(UTC));
        }
    }
}
