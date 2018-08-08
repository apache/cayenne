package org.apache.cayenne.crypto.transformer.value;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * @since 4.1
 */
public class LocalDateTimeConverter implements BytesConverter<LocalDateTime> {
    public static final BytesConverter<LocalDateTime> INSTANCE = new LocalDateTimeConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    public LocalDateTimeConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }


    @Override
    public LocalDateTime fromBytes(byte[] bytes) {
        int dateLength = 2;
        int timeLength = 8;

        byte[] date = new byte[dateLength];
        byte[] time = new byte[timeLength];

        System.arraycopy(bytes, 0, date, 0, dateLength);
        System.arraycopy(bytes, dateLength, time, 0, timeLength);

        LocalDate localDate = LocalDate.ofEpochDay(longConverter.fromBytes(date));
        LocalTime localTime = LocalTime.ofNanoOfDay(longConverter.fromBytes(time));
        return LocalDateTime.of(localDate, localTime);
    }


    @Override
    public byte[] toBytes(LocalDateTime value) {
        byte[] date = longConverter.toBytes(value.toLocalDate().toEpochDay());
        byte[] time = longConverter.toBytes(value.toLocalTime().toNanoOfDay());

        byte [] datetime = new byte[date.length + time.length];
        System.arraycopy(date,0, datetime, 0, date.length);
        System.arraycopy(time,0, datetime, date.length, time.length);

        return datetime;
    }
}
