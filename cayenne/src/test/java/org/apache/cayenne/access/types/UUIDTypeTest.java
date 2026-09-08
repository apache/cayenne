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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;

import org.apache.cayenne.CayenneRuntimeException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UUIDTypeTest {

    private static final UUID UUID_VALUE = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

    private static final byte[] UUID_BYTES = {
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
    };

    private final UUIDType type = new UUIDType();

    @Test
    public void className() {
        assertEquals("java.util.UUID", type.getClassName());
    }

    @Test
    public void toBytesIsBigEndian() {
        assertArrayEquals(UUID_BYTES, UUIDType.toBytes(UUID_VALUE));
    }

    @Test
    public void fromBytesIsBigEndian() {
        assertEquals(UUID_VALUE, UUIDType.fromBytes(UUID_BYTES));
    }

    @Test
    public void bytesRoundTrip() {
        UUID value = UUID.randomUUID();
        assertEquals(value, UUIDType.fromBytes(UUIDType.toBytes(value)));
    }

    @Test
    public void fromBytesRejectsWrongLength() {
        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> UUIDType.fromBytes(new byte[15]));
        assertEquals("Invalid UUID length: 15, expected 16", e.getUnlabeledMessage());
    }

    @Test
    public void materializeFromCharColumn() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(UUID_VALUE.toString());

        assertEquals(UUID_VALUE, type.materializeObject(rs, 1, Types.VARCHAR));
    }

    @Test
    public void materializeFromCharColumnRejectsMalformedValue() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("not-a-uuid-100%");

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> type.materializeObject(rs, 1, Types.VARCHAR));
        assertEquals("Invalid UUID value: not-a-uuid-100%", e.getUnlabeledMessage());
    }

    @Test
    public void materializeFromBinaryColumn() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBytes(1)).thenReturn(UUID_BYTES);

        assertEquals(UUID_VALUE, type.materializeObject(rs, 1, Types.VARBINARY));
    }

    @Test
    public void materializeFromOtherColumn() throws Exception {
        ResultSet rs = mock(ResultSet.class);

        when(rs.getObject(1)).thenReturn(UUID_VALUE);
        assertEquals(UUID_VALUE, type.materializeObject(rs, 1, Types.OTHER));

        when(rs.getObject(1)).thenReturn(UUID_VALUE.toString());
        assertEquals(UUID_VALUE, type.materializeObject(rs, 1, Types.OTHER));

        when(rs.getObject(1)).thenReturn(UUID_BYTES);
        assertEquals(UUID_VALUE, type.materializeObject(rs, 1, Types.OTHER));
    }

    @Test
    public void materializeFromOtherColumnRejectsUnknownValue() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn(42);

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> type.materializeObject(rs, 1, Types.OTHER));
        assertEquals("Can't convert to UUID: java.lang.Integer", e.getUnlabeledMessage());
    }

    @Test
    public void materializeNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);

        assertNull(type.materializeObject(rs, 1, Types.VARCHAR));
        assertNull(type.materializeObject(rs, 1, Types.VARBINARY));
        assertNull(type.materializeObject(rs, 1, Types.OTHER));
    }

    @Test
    public void setJdbcObjectOnCharColumn() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);
        type.setJdbcObject(st, UUID_VALUE, 1, Types.VARCHAR, -1);

        verify(st).setString(1, UUID_VALUE.toString());
    }

    @Test
    public void setJdbcObjectOnBinaryColumn() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);
        type.setJdbcObject(st, UUID_VALUE, 1, Types.VARBINARY, -1);

        verify(st).setBytes(1, UUID_BYTES);
    }

    @Test
    public void setJdbcObjectOnOtherColumn() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);
        type.setJdbcObject(st, UUID_VALUE, 1, Types.OTHER, -1);

        verify(st).setObject(1, UUID_VALUE);
    }

    @Test
    public void setJdbcObjectNull() throws Exception {
        PreparedStatement st = mock(PreparedStatement.class);
        type.setJdbcObject(st, null, 1, Types.VARBINARY, -1);

        verify(st).setNull(1, Types.VARBINARY);
    }

    @Test
    public void typeToString() {
        assertEquals("NULL", type.toString(null));
        assertEquals("'00112233-4455-6677-8899-aabbccddeeff'", type.toString(UUID_VALUE));
    }
}
