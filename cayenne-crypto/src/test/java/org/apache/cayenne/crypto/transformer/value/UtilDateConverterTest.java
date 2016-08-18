package org.apache.cayenne.crypto.transformer.value;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class UtilDateConverterTest {

    private Date date(String dateString) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f.parse(dateString);
    }

    @Test
    public void testFromBytes() throws ParseException {
        assertEquals(date("2015-01-07 11:00:02"), UtilDateConverter.INSTANCE.fromBytes(new byte[]{0, 0, 1, 74, -60, 13, 31, 80}));
    }

    @Test
    public void testToBytes() throws ParseException {
        assertArrayEquals(new byte[]{0, 0, 1, 74, -60, 13, 31, 80},
                UtilDateConverter.INSTANCE.toBytes(date("2015-01-07 11:00:02")));
    }
}
