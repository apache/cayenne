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
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * ExtendedType for <code>java.util.UUID</code> that supports string and binary JDBC columns. <br>
 * For char columns, stores UUID as canonical string. <br>
 * For binary columns, stores UUID in 16-byte big-endian form. <br>
 * For other columns (e.g. PostgreSQL native UUID), delegates to driver's native handling where possible.
 *
 * @since 4.2
 */
public class UUIDType implements ExtendedType<UUID> {

    private static final int UUID_BYTES = 2 * Long.BYTES;

    @Override
    public String getClassName() {
        return UUID.class.getName();
    }

    @Override
    public UUID materializeObject(ResultSet rs, int index, int type) throws Exception {
        switch (type) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR: {
                String s = rs.getString(index);
                return s != null ? UUID.fromString(s) : null;
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY: {
                byte[] b = rs.getBytes(index);
                return b != null ? fromBytes(b) : null;
            }
            case Types.BLOB: {
                return fromBlob(rs.getBlob(index));
            }
            default: {
                return fromObject(rs.getObject(index));
            }
        }
    }

    @Override
    public UUID materializeObject(CallableStatement cs, int index, int type) throws Exception {
        switch (type) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR: {
                String s = cs.getString(index);
                return s != null ? UUID.fromString(s) : null;
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY: {
                byte[] b = cs.getBytes(index);
                return b != null ? fromBytes(b) : null;
            }
            case Types.BLOB: {
                return fromBlob(cs.getBlob(index));
            }
            default: {
                Object o = cs.getObject(index);
                return fromObject(o);
            }
        }
    }

    @Override
    public void setJdbcObject(PreparedStatement statement, UUID value, int pos, int type, int precision) throws Exception {
        if (value == null) {
            statement.setNull(pos, type);
            return;
        }

        switch (type) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR: {
                statement.setString(pos, value.toString());
                return;
            }
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY: {
                statement.setBytes(pos, toBytes(value));
                return;
            }
            case Types.BLOB: {
                byte[] b = toBytes(value);
                if (precision != -1) {
                    statement.setObject(pos, b, type, precision);
                } else {
                    statement.setObject(pos, b, type);
                }
                return;
            }
            case Types.OTHER: {
                // native UUID
                statement.setObject(pos, value);
                return;
            }
            default: {
                if (precision != -1) {
                    statement.setObject(pos, value.toString(), type, precision);
                } else {
                    statement.setObject(pos, value.toString(), type);
                }
            }
        }
    }

    @Override
    public String toString(UUID value) {
        return value == null ? "NULL" : value.toString();
    }

    private static UUID fromBlob(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }
        long len = blob.length();
        if (len == 0) {
            return null;
        }
        if (len != UUID_BYTES) {
            invalidUuidLength(len);
        }
        byte[] b = blob.getBytes(1, (int) len);
        return fromBytes(b);
    }

    private static UUID fromObject(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof UUID) {
            return (UUID) o;
        }
        if (o instanceof byte[]) {
            return fromBytes((byte[]) o);
        }
        return UUID.fromString(o.toString());
    }

    private static UUID fromBytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if (bytes.length != UUID_BYTES) {
            invalidUuidLength(bytes.length);
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }

    private static byte[] toBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return ByteBuffer.allocate(Long.BYTES * 2)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();
    }

    private static void invalidUuidLength(long length) {
        throw new CayenneRuntimeException("Invalid UUID length, expected " + UUID_BYTES + " bytes: " + length);
    }
}
