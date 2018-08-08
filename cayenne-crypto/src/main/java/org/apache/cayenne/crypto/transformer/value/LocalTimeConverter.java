package org.apache.cayenne.crypto.transformer.value;

import java.time.LocalTime;
import java.util.Objects;

/**
 * @since 4.1.M3
 */
public class LocalTimeConverter implements BytesConverter<LocalTime> {
    public static final BytesConverter<LocalTime> INSTANCE = new LocalTimeConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    public LocalTimeConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }

    @Override
    public LocalTime fromBytes(byte[] bytes) {
        return LocalTime.ofNanoOfDay(longConverter.fromBytes(bytes));
    }

    @Override
    public byte[] toBytes(LocalTime value) {
        return longConverter.toBytes(value.toNanoOfDay());
    }
}
