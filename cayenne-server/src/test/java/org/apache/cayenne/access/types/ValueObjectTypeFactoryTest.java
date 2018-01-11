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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueObjectTypeFactoryTest {

    ExtendedType tstType1, tstType2, tstType3, tstType4;

    ValueObjectTypeFactory factory;

    @Before
    public void setUpRegistry(){
        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(createMockValueType(UUID.class, byte[].class));
        list.add(createMockValueType(String.class, String.class));
        list.add(createMockValueType(int.class, int.class));
        list.add(createMockValueType(String[].class, String[].class));

        DefaultValueObjectTypeRegistry registry = new DefaultValueObjectTypeRegistry(list);

        ExtendedTypeMap extendedTypeMap = new ExtendedTypeMap();

        tstType1 = mock(ExtendedType.class);
        when(tstType1.getClassName()).thenReturn("byte[]");
        extendedTypeMap.registerType(tstType1);

        tstType2 = new MockExtendedType(String.class);
        extendedTypeMap.registerType(tstType2);

        tstType3 = new MockExtendedType(int.class);
        extendedTypeMap.registerType(tstType3);

        tstType4 = mock(ExtendedType.class);
        when(tstType4.getClassName()).thenReturn(String[].class.getCanonicalName());
        extendedTypeMap.registerType(tstType4);

        factory = new ValueObjectTypeFactory(extendedTypeMap,registry);
    }

    private ValueObjectType createMockValueType(Class<?> valueClass, Class<?> targetClass) {
        ValueObjectType valueObjectType = mock(ValueObjectType.class);
        when(valueObjectType.getValueType()).thenReturn(valueClass);
        when(valueObjectType.getTargetType()).thenReturn(targetClass);
        return valueObjectType;
    }

    @Test
    public void testUUIDtoByteArray(){
        ValueObjectTypeFactory.ExtendedTypeConverter converter1 = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(UUID.class);
        assertNotNull(converter1);
        assertSame(tstType1, converter1.extendedType);
    }

    @Test
    public void testString(){
        ValueObjectTypeFactory.ExtendedTypeConverter converter2 = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(String.class);
        assertNotNull(converter2);
        assertSame(tstType2, converter2.extendedType);
    }

    @Test
    public void testInt(){
        ValueObjectTypeFactory.ExtendedTypeConverter converter3 = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(int.class);
        assertNotNull(converter3);
        assertSame(tstType3, converter3.extendedType);
    }

    @Test
    public void testStringArray(){
        ValueObjectTypeFactory.ExtendedTypeConverter converter4 = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(String[].class);
        assertNotNull(converter4);
        assertSame(tstType4, converter4.extendedType);
    }
}
