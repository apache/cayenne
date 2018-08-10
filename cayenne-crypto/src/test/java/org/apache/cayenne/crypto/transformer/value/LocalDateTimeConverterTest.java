package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalDateTimeConverterTest {

    @Test
    public void testFromBytes() {
        assertEquals(LocalDateTime.of(2015, 1, 7, 11, 0, 2),
                LocalDateTimeConverter.INSTANCE.fromBytes(new byte[]{2, 64, 58, 0, 0, 36, 4, -113, 36, 116, 0}));
    }

    @Test
    public void testToBytes() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.of(2015, 1, 7, 11, 0, 2));
        assertArrayEquals(new byte[]{2, 64, 58, 0, 0, 36, 4, -113, 36, 116, 0}, bytes);
    }

    @Test
    public void testToBytesBig() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.of(123456, 12, 31, 23, 59, 59));
        assertArrayEquals(new byte[]{4, 2, -91, 16, -9, 0, 0, 78, -108, 85, -76, 54, 0}, bytes);
    }

    @Test
    public void testFromBytesBig() {
        LocalDateTime localDateTime = LocalDateTimeConverter.INSTANCE
                .fromBytes(new byte[]{4, 2, -91, 16, -9, 0, 0, 78, -108, 85, -76, 54, 0});
        assertEquals(LocalDateTime.of(123456, 12, 31, 23, 59, 59), localDateTime);
    }

    @Test
    public void testToBytesSmall() {
        byte[] bytes = LocalDateTimeConverter.INSTANCE
                .toBytes(LocalDateTime.of(0, 1, 1, 0, 0, 0));
        assertArrayEquals(new byte[]{4, -1, -11, 5, 88, 0}, bytes);
    }

    @Test
    public void testFromBytesSmall() {
        LocalDateTime localDateTime = LocalDateTimeConverter.INSTANCE
                .fromBytes(new byte[]{4, -1, -11, 5, 88, 0});
        assertEquals(LocalDateTime.of(0, 1, 1, 0, 0, 0), localDateTime);
    }
}
