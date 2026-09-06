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

import org.apache.cayenne.access.types.LocalDateType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Stores {@link LocalDate} as text, see {@link SQLiteJavaTimeFormats}. Reads both a plain date and a full timestamp
 * (the form the driver stores for {@code java.util.Date}).
 *
 * @since 5.0
 */
class SQLiteLocalDateType extends LocalDateType {

    @Override
    public LocalDate materializeObject(ResultSet rs, int index, int type) throws Exception {
        return parse(rs.getString(index));
    }

    @Override
    public LocalDate materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return parse(rs.getString(index));
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDate value, int pos, int type, int scale)
            throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            statement.setString(pos, SQLiteJavaTimeFormats.DATE_OUT.format(value));
        }
    }

    private static LocalDate parse(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 10
                ? SQLiteJavaTimeFormats.parseTimestamp(value).toLocalDate()
                : LocalDate.parse(value, SQLiteJavaTimeFormats.DATE_OUT);
    }
}
