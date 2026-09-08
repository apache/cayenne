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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * An ExtendedType for {@code java.util.UUID} that picks the storage format from the JDBC type of the mapped
 * column:
 * <ul>
 *     <li>character columns store the canonical 36-char string;</li>
 *     <li>binary columns store the 16-byte big-endian form, most significant bits first;</li>
 *     <li>anything else is passed to the driver as an object, which is how a PostgreSQL native
 *     {@code uuid} column works.</li>
 * </ul>
 *
 * @since 5.0
 */
public class UUIDType implements ExtendedType<UUID> {

    /**
     * The length of the binary form of a UUID.
     */
    public static final int UUID_BYTES = 2 * Long.BYTES;

    @Override
    public String getClassName() {
        return UUID.class.getName();
    }

    @Override
    public UUID materializeObject(ResultSet rs, int index, int type) throws Exception {
        return switch (format(type)) {
            case CHAR -> fromString(rs.getString(index));
            case BINARY -> fromBytes(rs.getBytes(index));
            case OTHER -> fromObject(rs.getObject(index));
        };
    }

    @Override
    public UUID materializeObject(CallableStatement rs, int index, int type) throws Exception {
        return switch (format(type)) {
            case CHAR -> fromString(rs.getString(index));
            case BINARY -> fromBytes(rs.getBytes(index));
            case OTHER -> fromObject(rs.getObject(index));
        };
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, UUID value, int pos, int type, int scale)
            throws Exception {

        if (value == null) {
            statement.setNull(pos, type);
            return;
        }

        switch (format(type)) {
            case CHAR -> statement.setString(pos, value.toString());
            case BINARY -> statement.setBytes(pos, toBytes(value));
            case OTHER -> statement.setObject(pos, value);
        }
    }

    @Override
    public String toString(UUID value) {
        return value == null ? "NULL" : "'" + value + "'";
    }

    /**
     * Converts a UUID to its 16-byte big-endian form, most significant bits first.
     */
    public static byte[] toBytes(UUID value) {
        return ByteBuffer.allocate(UUID_BYTES)
                .putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits())
                .array();
    }

    /**
     * Converts a 16-byte big-endian value, most significant bits first, to a UUID.
     */
    public static UUID fromBytes(byte[] value) {
        if (value == null) {
            return null;
        }
        if (value.length != UUID_BYTES) {
            throw new CayenneRuntimeException("Invalid UUID length: %d, expected %d", value.length, UUID_BYTES);
        }

        ByteBuffer buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    protected static UUID fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new CayenneRuntimeException("Invalid UUID value: %s", e, value);
        }
    }

    /**
     * Converts whatever the driver returned for a column of an unrecognized JDBC type. Drivers with native UUID
     * support return a UUID; the rest fall back to the string or binary form.
     */
    protected static UUID fromObject(Object value) {
        return switch (value) {
            case null -> null;
            case UUID uuid -> uuid;
            case String string -> fromString(string);
            case byte[] bytes -> fromBytes(bytes);
            default -> throw new CayenneRuntimeException("Can't convert to UUID: %s", value.getClass().getName());
        };
    }

    private static Format format(int jdbcType) {
        return switch (jdbcType) {
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                 Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> Format.CHAR;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> Format.BINARY;
            default -> Format.OTHER;
        };
    }

    private enum Format {
        CHAR, BINARY, OTHER
    }
}
