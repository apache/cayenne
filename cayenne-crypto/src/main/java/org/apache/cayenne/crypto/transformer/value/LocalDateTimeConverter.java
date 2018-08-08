package org.apache.cayenne.crypto.transformer.value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * @since 4.1.M3
 */
public class LocalDateTimeConverter implements BytesConverter<LocalDateTime> {
    public static final BytesConverter<LocalDateTime> INSTANCE = new LocalDateTimeConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    public LocalDateTimeConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }


    @Override
    public LocalDateTime fromBytes(byte[] bytes) {

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(longConverter.fromBytes(bytes)),
                ZoneOffset.ofHours(0));
    }


    @Override
    public byte[] toBytes(LocalDateTime value) {
        return longConverter.toBytes(value.toInstant(ZoneOffset.ofHours(0)).toEpochMilli());
    }
}
