package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class BooleanConverterTest {

    @Test
    public void testFromBytes() {
        assertEquals(true, BooleanConverter.INSTANCE.fromBytes(new byte[]{1}));
        assertEquals(false, BooleanConverter.INSTANCE.fromBytes(new byte[]{0}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromBytes_InvalidArray() {
        BooleanConverter.INSTANCE.fromBytes(new byte[]{1, 0});
    }

    @Test
    public void testToBytes() throws ParseException {
        assertArrayEquals(new byte[]{0}, BooleanConverter.INSTANCE.toBytes(false));
        assertArrayEquals(new byte[]{1}, BooleanConverter.INSTANCE.toBytes(true));
    }
}
