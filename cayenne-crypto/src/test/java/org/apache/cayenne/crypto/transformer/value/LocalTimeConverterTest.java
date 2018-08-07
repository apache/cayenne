package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.text.ParseException;
import java.time.LocalTime;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalTimeConverterTest {

    private LocalTime localTime(String dateString) {
        return LocalTime.parse(dateString);
    }

    @Test
    public void testFromBytes() throws ParseException {
        assertEquals(localTime("11:00:02"), LocalTimeConverter.INSTANCE.fromBytes(new byte[]{0, 0, 36, 4, -113, 36, 116, 0}));
    }

    @Test
    public void testToBytes() throws ParseException {
        assertArrayEquals(new byte[]{0, 0, 36, 4, -113, 36, 116, 0},
                LocalTimeConverter.INSTANCE.toBytes(localTime("11:00:02")));
    }

}
