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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.LocalDateTimeType;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * A type for handling "timestamp" and "timestamptz" columns in PostgreSQL. Generic LocalDateTimeType is noticeably
 * faster but can only handle "timestamp". So use this type we can't tell the two types apart or if we know for a fact
 * that we are dealing with "timestamptz".
 *
 * @since 5.0
 */
class PostgresTimestampTzType extends LocalDateTimeType {

    private static final String TIMESTAMP = "timestamp";
    private static final ExtendedType<LocalDateTime> DIRECT_TYPE = new LocalDateTimeType();

    static RSColumn[] optimizeTimestampColumns(RSColumn[] columns, ResultSet rs) throws SQLException {
        ResultSetMetaData metadata = null;
        RSColumn[] resolved = columns;

        for (int i = 0; i < columns.length; i++) {
            RSColumn column = columns[i];
            if (!(column.reader() instanceof PostgresTimestampTzType)) {
                continue;
            }

            if (metadata == null) {
                metadata = rs.getMetaData();
            }

            if (TIMESTAMP.equals(metadata.getColumnTypeName(i + 1))) {
                if (resolved == columns) {
                    resolved = columns.clone();
                }
                resolved[i] = new RSColumn(column.rsName(), column.rsType(), column.dataRowName(), DIRECT_TYPE, column.attribute());
            }
        }

        return resolved;
    }

    @Override
    public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
        return fromTimestamp(rs.getTimestamp(index));
    }

    @Override
    public LocalDateTime materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return fromTimestamp(rs.getTimestamp(index));
    }

    private static LocalDateTime fromTimestamp(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
