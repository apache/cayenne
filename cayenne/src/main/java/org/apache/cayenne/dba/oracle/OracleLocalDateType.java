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

import org.apache.cayenne.access.types.LocalDateType;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Oracle driver converts a {@link LocalDate} passed to {@code setObject} through the JVM default time zone, so a date
 * whose midnight falls into a DST gap gets a non-zero time part in the DATE column. Binding a {@link Date} with a UTC
 * calendar preserves the date. Reads use the default {@code getObject} path, which is exact.
 *
 * @since 5.0
 */
class OracleLocalDateType extends LocalDateType {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDate value, int pos, int type, int scale)
            throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            long millis = value.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            statement.setDate(pos, new Date(millis), Calendar.getInstance(UTC));
        }
    }
}
