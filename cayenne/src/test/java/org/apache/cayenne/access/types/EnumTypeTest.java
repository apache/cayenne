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

import com.mockrunner.mock.jdbc.MockResultSet;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumTypeTest {

    @Test
    public void constructor() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        assertEquals(MockEnum.class.getName(), type.getClassName());
        assertEquals(MockEnum.values().length, type.values.length);
        
        for(int i = 0; i < MockEnum.values().length; i++) {
            assertSame(MockEnum.values()[i], type.values[i]);
        }
    }

    @Test
    public void invalidConstructor1() {
        assertThrows(IllegalArgumentException.class, () -> new EnumType(Object.class));
    }

    @Test
    public void invalidConstructor2() {
        assertThrows(IllegalArgumentException.class, () -> new EnumType(null));
    }

    @Test
    public void materializeStringObject() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {"b"});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.VARCHAR);
        assertSame(MockEnum.b, o);
    }

    @Test
    public void materializeNumericObject() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {2});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.NUMERIC);
        assertSame(MockEnum.c, o);
    }

    @Test
    public void materializeStringObjectInnerEnum() throws Exception {
        EnumType type = new EnumType(InnerEnumHolder.InnerEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {"b"});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.VARCHAR);
        assertSame(InnerEnumHolder.InnerEnum.b, o);
    }

    @Test
    public void materializeNumericObjectInnerEnum() throws Exception {
        EnumType type = new EnumType(InnerEnumHolder.InnerEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {2});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.NUMERIC);
        assertSame(InnerEnumHolder.InnerEnum.c, o);
    }
}
