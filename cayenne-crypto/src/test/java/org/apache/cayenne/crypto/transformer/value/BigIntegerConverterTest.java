package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class BigIntegerConverterTest {

    private BigInteger positiveInt = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(Long.MAX_VALUE));
    private BigInteger negativeInt = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.valueOf(Long.MIN_VALUE));

    @Test
    public void testConverter() {
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(positiveInt, converter.fromBytes(converter.toBytes(positiveInt)));
    }

    @Test
    public void testConverter_Negative() {
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(negativeInt, converter.fromBytes(converter.toBytes(negativeInt)));
    }

    @Test
    public void testConverter_Zero() {
        BigInteger originalValue = BigInteger.ZERO;
        BigIntegerConverter converter = new BigIntegerConverter();
        assertEquals(originalValue, converter.fromBytes(converter.toBytes(originalValue)));
    }
}
