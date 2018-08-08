package org.apache.cayenne.crypto.transformer.value;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
                ZoneId.systemDefault());
    }


    @Override
    public byte[] toBytes(LocalDateTime value) {

        long epochMilli = value.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        return longConverter.toBytes(epochMilli);
    }
}
