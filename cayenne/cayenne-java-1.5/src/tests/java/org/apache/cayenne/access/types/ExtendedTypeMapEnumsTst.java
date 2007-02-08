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

import junit.framework.TestCase;

public class ExtendedTypeMapEnumsTst extends TestCase {

    public void testSupportsEnums() {
        ExtendedTypeMap map = new ExtendedTypeMap();
        assertNotNull(map.enumTypeConstructor);
    }

    public void testGetDefaultType1_5() {
        ExtendedTypeMap map = new ExtendedTypeMap();

        assertNull(map.getDefaultType(Object.class.getName()));

        ExtendedType type = map.getDefaultType(MockEnum.class.getName());
        assertTrue(type instanceof EnumType);
        assertEquals(MockEnum.class, ((EnumType) type).enumClass);

        ExtendedType type2 = map.getDefaultType(MockEnum2.class.getName());
        assertNotSame(type, type2);
    }

    public void testGetDefaultType1_5InnerEnum() {
        ExtendedTypeMap map = new ExtendedTypeMap();

        ExtendedType type = map.getDefaultType(InnerEnumHolder.InnerEnum.class.getName());
        assertTrue(type instanceof EnumType);
        assertEquals(InnerEnumHolder.InnerEnum.class, ((EnumType) type).enumClass);
        
        // use a string name with $
        ExtendedType type1 = map.getDefaultType(InnerEnumHolder.class.getName() + "$InnerEnum");
        assertNotNull(type1);
        assertSame(type.getClassName(), type1.getClassName());
        
        // use a string name with .
        ExtendedType type2 = map.getDefaultType(InnerEnumHolder.class.getName() + ".InnerEnum");
        assertNotNull(type2);
        assertSame(type.getClassName(), type2.getClassName());
    }

    public void testGetDefaultType1_4() {
        ExtendedTypeMap map = new ExtendedTypeMap();
        map.enumTypeConstructor = null;

        assertNull(map.getDefaultType(Object.class.getName()));
        assertNull(map.getDefaultType(MockEnum.class.getName()));
        assertNull(map.getDefaultType(MockEnum2.class.getName()));
    }
}
