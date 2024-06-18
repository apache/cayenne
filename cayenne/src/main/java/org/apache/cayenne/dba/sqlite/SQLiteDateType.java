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

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cayenne.access.types.UtilDateType;

/**
 * Implements special date handling for SQLite. As SQLite has no native date type and the
 * JDBC driver does not standardize it either.
 * 
 * @since 3.0
 */
// see http://www.zentus.com/sqlitejdbc/usage.html for some examples of the SQLite date
// handling fun.
class SQLiteDateType extends UtilDateType {

    private final DateFormat timestampFormat;
    private final DateFormat dateFormat;
    private final DateFormat timeFormat;

    public SQLiteDateType() {
        timestampFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss.SSS");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        timeFormat = new SimpleDateFormat("kk:mm:ss");
    }

    @Override
    public Date materializeObject(ResultSet rs, int index, int type) throws Exception {
        return parseDate(rs.getString(index), type);
    }

    @Override
    public Date materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return parseDate(rs.getString(index), type);
    }

    protected Date parseDate(String string, int type) throws SQLException {
        if (string == null) {
            return null;
        }

        long ts = getLongTimestamp(string);
        if (ts >= 0) {
            return new Date(ts);
        }

        switch (type) {
            case Types.TIME:
                return getTime(string);
            case Types.DATE:
            case Types.TIMESTAMP:
            default:
                return getTimestamp(string);
        }
    }

    protected Date getTimestamp(String string) throws SQLException {
        try {
            synchronized (timestampFormat) {
                return timestampFormat.parse(string);
            }
        } catch (ParseException e) {
            // also try date format...
            return getDate(string);
        }
    }

    protected Date getDate(String string) throws SQLException {
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(string);
            }
        } catch (ParseException e) {
            throw new SQLException("Unparsable date/time string: " + string);
        }
    }

    protected Date getTime(String string) throws SQLException {
        try {
            synchronized (timeFormat) {
                return timeFormat.parse(string);
            }
        } catch (ParseException e) {
            return getTimestamp(string);
        }
    }

    protected long getLongTimestamp(String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
