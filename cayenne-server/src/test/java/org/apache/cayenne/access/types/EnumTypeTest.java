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
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class EnumTypeTest {

    @Test
    public void testConstructor() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        assertEquals(MockEnum.class.getName(), type.getClassName());
        assertEquals(MockEnum.values().length, type.values.length);
        
        for(int i = 0; i < MockEnum.values().length; i++) {
            assertSame(MockEnum.values()[i], type.values[i]);
        }
    }

    @Test
    public void testInvalidConstructor1() throws Exception {
        try {
            new EnumType(Object.class);
            fail("Non-enum class went through");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testInvalidConstructor2() throws Exception {
        try {
            new EnumType(null);
            fail("Null class went through");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testMaterializeStringObject() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {"b"});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.VARCHAR);
        assertSame(MockEnum.b, o);
    }

    @Test
    public void testMaterializeNumericObject() throws Exception {
        EnumType type = new EnumType(MockEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {2});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.NUMERIC);
        assertSame(MockEnum.c, o);
    }

    @Test
    public void testMaterializeStringObjectInnerEnum() throws Exception {
        EnumType type = new EnumType(InnerEnumHolder.InnerEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {"b"});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.VARCHAR);
        assertSame(InnerEnumHolder.InnerEnum.b, o);
    }

    @Test
    public void testMaterializeNumericObjectInnerEnum() throws Exception {
        EnumType type = new EnumType(InnerEnumHolder.InnerEnum.class);
        
        MockResultSet rs = new MockResultSet("Test");
        rs.addColumn("Enum");
        rs.addRow(new Object[] {2});
        rs.next();
        
        Object o = type.materializeObject(rs, 1, Types.NUMERIC);
        assertSame(InnerEnumHolder.InnerEnum.c, o);
    }
}
