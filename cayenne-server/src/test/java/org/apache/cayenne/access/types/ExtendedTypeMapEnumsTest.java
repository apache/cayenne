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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ExtendedTypeMapEnumsTest {

    @Test
    public void testCreateType1_5() {
        ExtendedTypeMap map = new ExtendedTypeMap();

        assertNull(map.createType(Object.class.getName()));

        ExtendedType type = map.createType(MockEnum.class.getName());
        assertTrue(type instanceof EnumType);
        assertEquals(MockEnum.class, ((EnumType) type).enumClass);

        ExtendedType type2 = map.createType(MockEnum2.class.getName());
        assertNotSame(type, type2);
    }

    @Test
    public void testCreateType1_5InnerEnum() {
        ExtendedTypeMap map = new ExtendedTypeMap();

        ExtendedType type = map.createType(InnerEnumHolder.InnerEnum.class.getName());
        assertTrue(type instanceof EnumType);
        assertEquals(InnerEnumHolder.InnerEnum.class, ((EnumType) type).enumClass);

        // use a string name with $
        ExtendedType type1 = map.createType(InnerEnumHolder.class.getName()
                + "$InnerEnum");
        assertNotNull(type1);
        assertSame(type.getClassName(), type1.getClassName());

        // use a string name with .
        ExtendedType type2 = map.createType(InnerEnumHolder.class.getName()
                + ".InnerEnum");
        assertNotNull(type2);
        assertSame(type.getClassName(), type2.getClassName());
    }

    @Test
    public void testGetDefaultType1_4() {
        ExtendedTypeMap map = new ExtendedTypeMap();
        map.internalTypeFactories.clear();

        assertNull(map.createType(Object.class.getName()));
        assertNull(map.createType(MockEnum.class.getName()));
        assertNull(map.createType(MockEnum2.class.getName()));
    }

    @Test
    public void testGetType() {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ExtendedType type = map.getRegisteredType(MockEnum.class.getName());
        assertNotNull(type);
        assertTrue(type instanceof EnumType);
    }

    @Test
    public void testGetType1_5InnerEnum() {
        ExtendedTypeMap map = new ExtendedTypeMap();
        String innerEnumClassName = InnerEnumHolder.class.getName() + "$InnerEnum";
        String innerEnumClassDotName = InnerEnumHolder.class.getName() + ".InnerEnum";

        // register inner enum type using "$" name notation
        ExtendedType type = map.getRegisteredType(innerEnumClassName);
        assertNotNull(type);
        assertTrue(type instanceof EnumType);
        // register same inner enum type using "." name notation
        ExtendedType type1 = map.getRegisteredType(innerEnumClassDotName);
        assertNotNull(type1);
        assertSame(type, type1);

        // check type is registered
        type1 = map.getExplictlyRegisteredType(innerEnumClassName);
        assertNotNull(type1);
        assertSame(type, type1);
        type1 = map.getExplictlyRegisteredType(innerEnumClassDotName);
        assertNotNull(type1);
        assertSame(type, type1);

        // check registries size
        assertEquals(1, map.getRegisteredTypeNames().length);
        assertEquals(1, map.extendedTypeFactories.size());
    }
}
