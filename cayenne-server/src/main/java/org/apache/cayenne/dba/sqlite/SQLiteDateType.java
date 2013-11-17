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

    private DateFormat timestampFormat;
    private DateFormat dateFormat;
    private DateFormat timeFormat;

    public SQLiteDateType() {
        timestampFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        timeFormat = new SimpleDateFormat("kk:mm:ss");
    }

    @Override
    public Date materializeObject(ResultSet rs, int index, int type) throws Exception {

        String string = rs.getString(index);

        if (string == null) {
            return null;
        }

        long ts = getLongTimestamp(string);
        if (ts >= 0) {
            return new Date(ts);
        }

        switch (type) {
            case Types.TIMESTAMP:
                return getTimestamp(string);
            case Types.DATE:
                return getDate(string);
            case Types.TIME:
                return rs.getTime(index);
            default:
                return getTimestamp(string);
        }
    }

    @Override
    public Date materializeObject(CallableStatement rs, int index, int type)
            throws Exception {

        String string = rs.getString(index);

        if (string == null) {
            return null;
        }

        long ts = getLongTimestamp(string);
        if (ts >= 0) {
            return new Date(ts);
        }

        switch (type) {
            case Types.TIMESTAMP:
                return getTimestamp(string);
            case Types.DATE:
                return getDate(string);
            case Types.TIME:
                return getTime(string);
            default:
                return getTimestamp(string);
        }
    }

    protected Date getTimestamp(String string) throws SQLException {
        try {
            synchronized (timestampFormat) {
                return timestampFormat.parse(string);
            }
        }
        catch (ParseException e) {
            // also try date format...
            try {
                synchronized (dateFormat) {
                    return dateFormat.parse(string);
                }
            }
            catch (ParseException e1) {
                throw new SQLException("Unparsable timestamp string: " + string);
            }
        }
    }

    protected Date getDate(String string) throws SQLException {
        try {
            synchronized (dateFormat) {
                return dateFormat.parse(string);
            }
        }
        catch (ParseException e) {
            throw new SQLException("Unparsable date string: " + string);
        }
    }

    protected Date getTime(String string) throws SQLException {
        try {
            synchronized (timeFormat) {
                return timeFormat.parse(string);
            }
        }
        catch (ParseException e) {
            throw new SQLException("Unparsable time string: " + string);
        }
    }

    protected long getLongTimestamp(String string) {
        try {
            return Long.parseLong(string);
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }
}
