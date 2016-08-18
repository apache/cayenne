package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between java.util.Date and byte[], based on the long timestamp encoding.
 *
 * @since 4.0
 */
public class BooleanConverter implements BytesConverter {

    static final BytesConverter INSTANCE = new BooleanConverter();

    @Override
    public Object fromBytes(byte[] bytes) {

        if (bytes.length != 1) {
            throw new IllegalArgumentException("Unexpected number of bytes for boolean: " + bytes.length);
        }

        byte b = bytes[0];
        if (b == 0) {
            return Boolean.FALSE;
        } else if (b == 1) {
            return Boolean.TRUE;
        } else {
            throw new IllegalArgumentException("Unexpected byte value for boolean: " + b);
        }
    }

    @Override
    public byte[] toBytes(Object value) {
        return new byte[]{
                ((Boolean) value) ? (byte) 1 : 0
        };
    }
}
