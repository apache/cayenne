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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ExtendedTypeMapTest {

    @Test
    public void testRegisterType() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ExtendedType tstType = new MockExtendedType();

        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));

        map.registerType(tstType);
        assertSame(tstType, map.getRegisteredType(tstType.getClassName()));

        map.unregisterType(tstType.getClassName());
        assertSame(map.getDefaultType(), map.getRegisteredType(tstType.getClassName()));
    }

    @Test
    public void testRegisterTypeSubclasses() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ExtendedType tstType1 = new MockExtendedType(List.class);
        ExtendedType tstType2 = new MockExtendedType(ArrayList.class);

        // List
        map.registerType(tstType1);
        assertSame(tstType1, map.getRegisteredType(List.class));
        assertNotSame(tstType1, map.getRegisteredType(ArrayList.class));
        
        map = new ExtendedTypeMap();
        
        // ArrayList
        map.registerType(tstType2);
        assertNotSame(tstType2, map.getRegisteredType(List.class));
        assertSame(tstType2, map.getRegisteredType(ArrayList.class));
        
        map = new ExtendedTypeMap();
        
        // both
        map.registerType(tstType1);
        map.registerType(tstType2);
        assertSame(tstType1, map.getRegisteredType(List.class));
        assertSame(tstType2, map.getRegisteredType(ArrayList.class));
        
        
        map = new ExtendedTypeMap();
        
        // both - different order
        map.registerType(tstType2);
        map.registerType(tstType1);
        assertSame(tstType2, map.getRegisteredType(ArrayList.class));
        assertSame(tstType1, map.getRegisteredType(List.class));
        
    }

    @Test
    public void testRegisterArrayType() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ByteArrayType tstType = new ByteArrayType(false, true);

        map.registerType(tstType);
        assertSame(tstType, map.getRegisteredType(tstType.getClassName()));
        assertSame(tstType, map.getRegisteredType(byte[].class));

        map.unregisterType(tstType.getClassName());

        // will return serializable ExtendedType inner class
        assertTrue(map
                .getRegisteredType(tstType.getClassName())
                .getClass()
                .getName()
                .indexOf("SerializableTypeFactory") > 0);
    }

    @Test
    public void testRegisteredTypeName() throws Exception {
        ExtendedTypeMap map = new ExtendedTypeMap();
        ExtendedType tstType = new MockExtendedType();

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(0, map.getRegisteredTypeNames().length);

        map.registerType(tstType);

        assertNotNull(map.getRegisteredTypeNames());
        assertEquals(1, map.getRegisteredTypeNames().length);
        assertEquals(tstType.getClassName(), map.getRegisteredTypeNames()[0]);
    }
}
