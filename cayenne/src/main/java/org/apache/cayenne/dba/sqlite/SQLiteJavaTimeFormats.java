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
package org.apache.cayenne.dba.sqlite;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * String formats of the {@code java.time} values stored by the SQLite adapter. SQLite has no date/time column types,
 * so values are stored as text: "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd" and "HH:mm:ss", with a seconds fraction appended
 * only when it is non-zero, so that stored values compare equal to the same values written as SQL literals. Values
 * are parsed back leniently (space or "T" separator, optional seconds and fraction).
 *
 * @since 5.0
 */
final class SQLiteJavaTimeFormats {

    static final DateTimeFormatter TIMESTAMP_OUT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();
    static final DateTimeFormatter DATE_OUT = DateTimeFormatter.ISO_LOCAL_DATE;
    static final DateTimeFormatter TIME_OUT = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();

    static final DateTimeFormatter TIMESTAMP_IN = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd")
            .optionalStart().appendLiteral(' ').optionalEnd()
            .optionalStart().appendLiteral('T').optionalEnd()
            .appendPattern("HH:mm")
            .optionalStart().appendPattern(":ss").optionalEnd()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .toFormatter();

    private SQLiteJavaTimeFormats() {
    }

    static LocalDateTime parseTimestamp(String value) {
        return LocalDateTime.parse(value, TIMESTAMP_IN);
    }
}
