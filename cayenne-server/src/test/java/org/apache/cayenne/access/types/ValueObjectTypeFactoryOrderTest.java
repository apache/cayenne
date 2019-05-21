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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueObjectTypeFactoryOrderTest {
    ExtendedTypeMap extendedTypeMap;
    ValueObjectType valueObjectType1, valueObjectType2;
    ExtendedType tstType1, tstType2;

    @Before
    public void setUpRegistry(){
        valueObjectType1 = createMockValueType(UUID.class, byte[].class);
        valueObjectType2 = createMockValueType(UUID.class, String.class);

        extendedTypeMap = new ExtendedTypeMap();

        tstType1 = mock(ExtendedType.class);
        when(tstType1.getClassName()).thenReturn("byte[]");
        extendedTypeMap.registerType(tstType1);

        tstType2 = new MockExtendedType(String.class);
        extendedTypeMap.registerType(tstType2);
    }

    private ValueObjectType createMockValueType(Class<?> valueClass, Class<?> targetClass) {
        ValueObjectType valueObjectType = mock(ValueObjectType.class);
        when(valueObjectType.getValueType()).thenReturn(valueClass);
        when(valueObjectType.getTargetType()).thenReturn(targetClass);
        return valueObjectType;
    }

    @Test
    public void testByteFirstOrder(){
        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(valueObjectType1);
        list.add(valueObjectType2);

        DefaultValueObjectTypeRegistry registry = new DefaultValueObjectTypeRegistry(list);
        ValueObjectTypeFactory factory = new ValueObjectTypeFactory(extendedTypeMap,registry);

        ValueObjectTypeFactory.ExtendedTypeConverter converter = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(UUID.class);
        assertNotNull(converter);
        assertNotSame(tstType1, converter.extendedType);
        assertSame(tstType2,converter.extendedType);
    }

    @Test
    public void testStringFirstOrder(){
        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(valueObjectType2);
        list.add(valueObjectType1);

        DefaultValueObjectTypeRegistry registry = new DefaultValueObjectTypeRegistry(list);
        ValueObjectTypeFactory factory = new ValueObjectTypeFactory(extendedTypeMap,registry);

        ValueObjectTypeFactory.ExtendedTypeConverter converter = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(UUID.class);
        assertNotNull(converter);
        assertNotSame(tstType2, converter.extendedType);
        assertSame(tstType1,converter.extendedType);
    }
}
