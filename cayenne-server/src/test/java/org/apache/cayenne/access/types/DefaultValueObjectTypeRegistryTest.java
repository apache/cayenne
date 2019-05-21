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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 4.0
 */
public class DefaultValueObjectTypeRegistryTest {

    DefaultValueObjectTypeRegistry registry;
    ValueObjectType valueObjectType1, valueObjectType2;

    @Before
    public void setUpRegistry() {
        valueObjectType1 = mock(ValueObjectType.class);
        when(valueObjectType1.getValueType()).thenReturn(Integer.class);
        when(valueObjectType1.getTargetType()).thenReturn(Integer.class);

        valueObjectType2 = mock(ValueObjectType.class);
        when(valueObjectType2.getValueType()).thenReturn(Number.class);
        when(valueObjectType2.getTargetType()).thenReturn(Integer.class);

        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(valueObjectType1);
        list.add(valueObjectType2);

        registry = new DefaultValueObjectTypeRegistry(list);
    }

    @Test
    public void testInitialState() {
        assertEquals(2, registry.typeCache.size());
        assertTrue(registry.typeCache.containsKey(Integer.class.getName()));
        assertTrue(registry.typeCache.containsKey(Number.class.getName()));
        assertFalse(registry.typeCache.containsKey(String.class.getName()));
        assertFalse(registry.typeCache.containsKey(Float.class.getName()));
    }

    @Test
    public void getValueType() throws Exception {
        ValueObjectType<?,?> valueObjectType = registry.getValueType(Integer.class);
        assertSame(valueObjectType1, valueObjectType);

        valueObjectType = registry.getValueType(Float.class);
        assertNull(valueObjectType);

        valueObjectType = registry.getValueType(String.class);
        assertNull(valueObjectType);

        assertEquals(2, registry.typeCache.size());
        assertFalse(registry.typeCache.containsKey(String.class.getName()));
        assertFalse(registry.typeCache.containsKey(Float.class.getName()));
    }

}