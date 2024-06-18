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

package org.apache.cayenne.map;

import org.apache.cayenne.util.Util;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ObjAttributeTest {

    @Test
    public void testDbAttribute() {
        ObjAttribute attribute = new ObjAttribute("a1");

        DbAttribute dbAttr = new DbAttribute("tst_name", Types.INTEGER, null);
        attribute.setDbAttributePath(dbAttr.getName());
        assertEquals(dbAttr.getName(), attribute.getDbAttributeName());
    }

    @Test
    public void testDbAttributePath() {
        ObjAttribute attribute = new ObjAttribute("a1");
        attribute.setDbAttributePath("a");
        assertEquals("a", attribute.getDbAttributePath().value());
        assertEquals("a", attribute.getDbAttributeName());
        
        attribute.setDbAttributePath("a.b");
        assertEquals("a.b", attribute.getDbAttributePath().value());
        assertEquals("b", attribute.getDbAttributeName());
    }

    @Test
    public void testType() {
        ObjAttribute attribute = new ObjAttribute("a1");

        String type = "org.aa.zz";
        attribute.setType(type);
        assertEquals(type, attribute.getType());
    }

    @Test
    public void testSerializability() throws Exception {
        ObjAttribute a1 = new ObjAttribute("a1");

        ObjAttribute a2 = Util.cloneViaSerialization(a1);
        assertEquals(a1.getName(), a2.getName());
    }

    @Test
    public void testGetJavaClass() throws Exception {

        ObjAttribute a1 = new ObjAttribute("test");
        a1.setType("byte");
        assertEquals(byte.class.getName(), a1.getJavaClass().getName());

        a1.setType("byte[]");
        assertEquals(byte[].class.getName(), a1.getJavaClass().getName());

        a1.setType("java.lang.Byte");
        assertEquals(Byte.class.getName(), a1.getJavaClass().getName());

        a1.setType("java.lang.Byte[]");
        assertEquals(Byte[].class.getName(), a1.getJavaClass().getName());
    }

}
