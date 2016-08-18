package org.apache.cayenne.crypto.transformer.value;

import java.util.Date;
import java.util.Objects;

/**
 * Converts between java.util.Date and byte[], based on the long timestamp encoding.
 *
 * @since 4.0
 */
public class UtilDateConverter implements BytesConverter {

    public static final BytesConverter INSTANCE = new UtilDateConverter(LongConverter.INSTANCE);

    private BytesConverter longConverter;

    public UtilDateConverter(BytesConverter longConverter) {
        this.longConverter = Objects.requireNonNull(longConverter);
    }

    @Override
    public Object fromBytes(byte[] bytes) {
        return new Date((long) longConverter.fromBytes(bytes));
    }

    @Override
    public byte[] toBytes(Object value) {
        return longConverter.toBytes(((Date) value).getTime());
    }
}
