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

package org.apache.cayenne.java8.access.types;

import org.apache.cayenne.access.types.ExtendedType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LocalDateTimeType implements ExtendedType<LocalDateTime> {

    @Override
    public String getClassName() {
        return LocalDateTime.class.getName();
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, LocalDateTime value, int pos, int type, int scale) throws Exception {
        statement.setTimestamp(pos, Timestamp.valueOf(value));
    }

    @Override
    public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        Timestamp timestamp = rs.getTimestamp(index);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    @Override
    public LocalDateTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        Timestamp timestamp = rs.getTimestamp(index);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    @Override
    public String toString(LocalDateTime value) {
        if (value == null) {
            return "NULL";
        }

        return '\'' + value.toString() + '\'';
    }
}
