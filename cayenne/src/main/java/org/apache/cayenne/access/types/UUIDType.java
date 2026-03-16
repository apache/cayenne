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

package org.apache.cayenne.access.types;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * ExtendedType for <code>java.util.UUID</code> that supports string and binary JDBC columns. <br>
 * For char columns, stores UUID as canonical string. <br>
 * For binary columns, stores UUID in 16-byte big-endian form.
 *
 * @since 4.2
 */
public class UUIDType implements ExtendedType<UUID> {

    public static final int UUID_BYTES = 2 * Long.BYTES;

    @Override
    public String getClassName() {
        return UUID.class.getName();
    }

    @Override
    public UUID materializeObject(ResultSet rs, int index, int type) throws Exception {
        switch (Format.forJdbcType(type)) {
            case CHAR: {
                String s = rs.getString(index);
                return s != null ? UUID.fromString(s) : null;
            }
            case BINARY: {
                byte[] b = rs.getBytes(index);
                return b != null ? fromBytes(b) : null;
            }
            default: {
                throw new CayenneRuntimeException("Unsupported JDBC type: " + JDBCType.valueOf(type));
            }
        }
    }

    @Override
    public UUID materializeObject(CallableStatement cs, int index, int type) throws Exception {
        switch (Format.forJdbcType(type)) {
            case CHAR: {
                String s = cs.getString(index);
                return s != null ? UUID.fromString(s) : null;
            }
            case BINARY: {
                byte[] b = cs.getBytes(index);
                return b != null ? fromBytes(b) : null;
            }
            default: {
                throw new CayenneRuntimeException("Unsupported JDBC type: " + JDBCType.valueOf(type));
            }
        }
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, UUID value, int pos, int type, int precision) throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
            return;
        }

        switch (Format.forJdbcType(type)) {
            case CHAR: {
                statement.setString(pos, value.toString());
                return;
            }
            case BINARY: {
                statement.setBytes(pos, toBytes(value));
                return;
            }
            default: {
                throw new CayenneRuntimeException("Unsupported JDBC type: " + JDBCType.valueOf(type));
            }
        }
    }

    @Override
    public String toString(UUID value) {
        return value == null ? "NULL" : value.toString();
    }

    protected static UUID fromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length != UUID_BYTES) {
            throw new CayenneRuntimeException("Invalid UUID length (" + bytes.length + "), expected " + UUID_BYTES);
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    protected static byte[] toBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return ByteBuffer.allocate(Long.BYTES * 2)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    public enum Format {
        CHAR(List.of(Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR)),
        BINARY(List.of(Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY)),
        OTHER(List.of());

        private final List<Integer> jdbcTypes;

        Format(List<Integer> jdbcTypes) {
            this.jdbcTypes = jdbcTypes;
        }

        public static Format forJdbcType(int jdbcType) {
            for (Format format : values()) {
                if (format.jdbcTypes.contains(jdbcType)) {
                    return format;
                }
            }
            return OTHER;
        }
    }
}
