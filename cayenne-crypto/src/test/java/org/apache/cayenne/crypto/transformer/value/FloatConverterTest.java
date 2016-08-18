package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatConverterTest {

    @Test
    public void testConverter() {
        Float originalValue = 36.6f;
        FloatConverter converter = new FloatConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_Negative() {
        Float originalValue = -36.6f;
        FloatConverter converter = new FloatConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_MaxValue() {
        Float originalValue = Float.MAX_VALUE;
        FloatConverter converter = new FloatConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_MinValue() {
        Float originalValue = Float.MIN_VALUE;
        FloatConverter converter = new FloatConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_Zero() {
        Float originalValue = 0f;
        FloatConverter converter = new FloatConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }
}
