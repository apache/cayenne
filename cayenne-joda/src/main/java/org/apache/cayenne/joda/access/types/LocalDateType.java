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
import org.joda.time.LocalDate;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Handles <code>org.joda.time.LocalDate</code> type mapping.
 *
 * @since 4.0
 */
public class LocalDateType implements ExtendedType<LocalDate> {

    @Override
    public String getClassName() {
        return LocalDate.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDate value, int pos, int type, int scale) throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
        } else {
            long time = value.toDate().getTime();

            if (type == Types.DATE) {
                statement.setDate(pos, new Date(time));
            } else {
                statement.setTimestamp(pos, new Timestamp(time));
            }
        }
    }

    @Override
    public LocalDate materializeObject(ResultSet rs, int index, int type) throws Exception {
        if (type == Types.DATE && rs.getDate(index) != null) {
            return new LocalDate(rs.getDate(index).getTime());
        } else if (type == Types.TIMESTAMP && rs.getTimestamp(index) != null) {
            return new LocalDate(rs.getTimestamp(index).getTime());
        } else {
            return null;
        }
    }

    @Override
    public LocalDate materializeObject(CallableStatement rs, int index, int type) throws Exception {
        if (type == Types.DATE && rs.getDate(index) != null) {
            return new LocalDate(rs.getDate(index).getTime());
        } else if (type == Types.TIMESTAMP && rs.getTimestamp(index) != null) {
            return new LocalDate(rs.getTimestamp(index).getTime());
        } else {
            return null;
        }
    }

    @Override
    public String toString(LocalDate value) {
        if (value == null) {
            return "NULL";
        }

        return '\'' + value.toString() + '\'';
    }

}
