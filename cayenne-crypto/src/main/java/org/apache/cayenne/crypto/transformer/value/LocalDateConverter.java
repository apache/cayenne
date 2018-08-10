package org.apache.cayenne.crypto.transformer.value;

import java.time.LocalDate;
import java.util.Objects;

/**
 * @since 4.1
 */
public class LocalDateConverter implements BytesConverter<LocalDate> {

    public static final BytesConverter<LocalDate> INSTANCE = new LocalDateConverter(LongConverter.INSTANCE);

    private BytesConverter<Long> longConverter;

    public LocalDateConverter(BytesConverter<Long> longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }

    @Override
    public LocalDate fromBytes(byte[] bytes) {
        return LocalDate.ofEpochDay(longConverter.fromBytes(bytes));
    }

    @Override
    public byte[] toBytes(LocalDate value) {
        return longConverter.toBytes(value.toEpochDay());
    }
}
