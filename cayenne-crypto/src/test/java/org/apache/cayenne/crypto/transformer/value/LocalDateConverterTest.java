package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalDateConverterTest {

    @Test
    public void testFromBytes() {
        assertEquals(LocalDate.of(2015, 1, 7),
                LocalDateConverter.INSTANCE.fromBytes(new byte[]{64, 58}));
    }

    @Test
    public void testToBytes() {
        assertArrayEquals(new byte[]{64, 58},
                LocalDateConverter.INSTANCE.toBytes(LocalDate.of(2015, 1, 7)));
    }
}
