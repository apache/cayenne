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

    ExtendedTypeMap map;
    DefaultValueObjectTypeRegistry registry;
    ValueObjectType valueObjectType1, valueObjectType2, valueObjectType3, valueObjectType4;
    ExtendedType tstType1, tstType2, tstType3, tstType4;
    ValueObjectTypeFactory factory;

    @Before
    public void setUpRegistry(){
        valueObjectType1 = mock(ValueObjectType.class);
        when(valueObjectType1.getValueType()).thenReturn(UUID.class);
        when(valueObjectType1.getTargetType()).thenReturn(byte[].class);

        valueObjectType2 = mock(ValueObjectType.class);
        when(valueObjectType2.getValueType()).thenReturn(String.class);
        when(valueObjectType2.getTargetType()).thenReturn(String.class);

        valueObjectType3 = mock(ValueObjectType.class);
        when(valueObjectType3.getValueType()).thenReturn(int.class);
        when(valueObjectType3.getTargetType()).thenReturn(int.class);

        valueObjectType4 = mock(ValueObjectType.class);
        when(valueObjectType4.getValueType()).thenReturn(String[].class);
        when(valueObjectType4.getTargetType()).thenReturn(String[].class);

        List<ValueObjectType<?, ?>> list = new ArrayList<>();
        list.add(valueObjectType1);
        list.add(valueObjectType2);
        list.add(valueObjectType3);
        list.add(valueObjectType4);


        registry = new DefaultValueObjectTypeRegistry(list);

        map = new ExtendedTypeMap();

        tstType1 = mock(ExtendedType.class);
        when(tstType1.getClassName()).thenReturn("byte[]");
        map.registerType(tstType1);

        tstType2 = new MockExtendedType(String.class);
        map.registerType(tstType2);

        tstType3 = new MockExtendedType(int.class);
        map.registerType(tstType3);

        tstType4 = mock(ExtendedType.class);
        when(tstType4.getClassName()).thenReturn(String[].class.getCanonicalName());
        map.registerType(tstType4);

        factory = new ValueObjectTypeFactory(map,registry);
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
