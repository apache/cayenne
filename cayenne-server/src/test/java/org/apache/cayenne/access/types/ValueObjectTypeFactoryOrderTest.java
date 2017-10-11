package org.apache.cayenne.access.types;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValueObjectTypeFactoryOrderTest {
    ExtendedTypeMap map;
    DefaultValueObjectTypeRegistry registry;
    ValueObjectTypeFactory factory;
    ValueObjectType valueObjectType1, valueObjectType2;
    ExtendedType tstType1, tstType2;

    @Before
    public void setUpRegistry(){
        valueObjectType1 = mock(ValueObjectType.class);
        when(valueObjectType1.getValueType()).thenReturn(UUID.class);
        when(valueObjectType1.getTargetType()).thenReturn(byte[].class);

        valueObjectType2 = mock(ValueObjectType.class);
        when(valueObjectType2.getValueType()).thenReturn(UUID.class);
        when(valueObjectType2.getTargetType()).thenReturn(String.class);

        map = new ExtendedTypeMap();

        tstType1 = mock(ExtendedType.class);
        when(tstType1.getClassName()).thenReturn("byte[]");
        map.registerType(tstType1);

        tstType2 = new MockExtendedType(String.class);
        map.registerType(tstType2);
    }

    @Test
    public void testByteFirstOrder(){
        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(valueObjectType1);
        list.add(valueObjectType2);

        registry = new DefaultValueObjectTypeRegistry(list);

        factory = new ValueObjectTypeFactory(map,registry);

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

        registry = new DefaultValueObjectTypeRegistry(list);

        factory = new ValueObjectTypeFactory(map,registry);

        ValueObjectTypeFactory.ExtendedTypeConverter converter = (ValueObjectTypeFactory.ExtendedTypeConverter) factory.getType(UUID.class);
        assertNotNull(converter);
        assertNotSame(tstType2, converter.extendedType);
        assertSame(tstType1,converter.extendedType);
    }
}
