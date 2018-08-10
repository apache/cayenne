package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalTimeConverterTest {

    @Test
    public void testFromBytes() {
        assertEquals(LocalTime.of(11, 0, 2),
                LocalTimeConverter.INSTANCE.fromBytes(new byte[]{0, 0, 36, 4, -113, 36, 116, 0}));

        assertEquals(LocalTime.of(0, 0, 0),
                LocalTimeConverter.INSTANCE.fromBytes(new byte[]{0}));
    }

    @Test
    public void testToBytes() {
        assertArrayEquals(new byte[]{0, 0, 36, 4, -113, 36, 116, 0},
                LocalTimeConverter.INSTANCE.toBytes(LocalTime.of(11, 0, 2)));

        assertArrayEquals(new byte[]{0},
                LocalTimeConverter.INSTANCE.toBytes(LocalTime.of(0, 0, 0)));
    }

}
