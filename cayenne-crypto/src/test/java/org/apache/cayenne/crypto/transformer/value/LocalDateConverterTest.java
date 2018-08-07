package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.text.ParseException;
import java.time.LocalDate;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class LocalDateConverterTest {

    private LocalDate localDate(String dateString) {
        return LocalDate.parse(dateString);
    }

    @Test
    public void testFromBytes() throws ParseException {
        assertEquals(localDate("2015-01-07"), LocalDateConverter.INSTANCE.fromBytes(new byte[]{64, 58}));
    }

    @Test
    public void testToBytes() throws ParseException {
        assertArrayEquals(new byte[]{64, 58},
                LocalDateConverter.INSTANCE.toBytes(localDate("2015-01-07")));
    }
}
