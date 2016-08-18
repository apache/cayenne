package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DoubleConverterTest {

    @Test
    public void testConverter() {
        Double originalValue = 36.6d;
        DoubleConverter converter = new DoubleConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_Negative() {
        Double originalValue = -36.6d;
        DoubleConverter converter = new DoubleConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_MaxValue() {
        Double originalValue = Double.MAX_VALUE;
        DoubleConverter converter = new DoubleConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_MinValue() {
        Double originalValue = Double.MIN_VALUE;
        DoubleConverter converter = new DoubleConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }

    @Test
    public void testConverter_Zero() {
        Double originalValue = 0d;
        DoubleConverter converter = new DoubleConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }
}
