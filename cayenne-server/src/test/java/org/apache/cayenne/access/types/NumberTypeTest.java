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

package org.apache.cayenne.access.types;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Types;

import com.mockrunner.mock.jdbc.MockResultSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
public class NumberTypeTest {

    @Test
    public void testClassName() {
        NumberType type = new NumberType();
        assertEquals(Number.class.getName(), type.getClassName());
    }

    @Test
    public void testMaterializeObjectFromResultSet() throws Exception {
        MockResultSet rs = new MockResultSet("") {

            @Override
            public short getShort(int columnIndex) throws SQLException{
                return (short)10;
            }

            @Override
            public int getInt(int columnIndex) throws SQLException{
                return 10;
            }

            @Override
            public float getFloat(int columnIndex) throws SQLException{
                return 10f;
            }

            @Override
            public double getDouble(int columnIndex) throws SQLException{
                return 10d;
            }

            @Override
            public long getLong(int columnIndex) throws SQLException{
                return 10L;
            }

            @Override
            public byte getByte(int columnIndex) throws SQLException{
                return (byte)10;
            }

            @Override
            public BigDecimal getBigDecimal(int columnIndex) throws SQLException{
                return new BigDecimal(10);
            }
        };

        NumberType type = new NumberType();

        assertEquals(10, type.materializeObject(rs, 1, Types.INTEGER));
        assertEquals((short)10, type.materializeObject(rs, 1, Types.SMALLINT));
        assertEquals(10L, type.materializeObject(rs, 1, Types.BIGINT));
        assertEquals(10F, type.materializeObject(rs, 1, Types.REAL));
        assertEquals(10F, type.materializeObject(rs, 1, Types.FLOAT));
        assertEquals(10D, type.materializeObject(rs, 1, Types.DOUBLE));
        assertEquals(new BigDecimal(10), type.materializeObject(rs, 1, Types.DECIMAL));
        assertEquals(new BigDecimal(10), type.materializeObject(rs, 1, Types.NUMERIC));
        assertEquals((byte)10, type.materializeObject(rs, 1, Types.TINYINT));
    }
}