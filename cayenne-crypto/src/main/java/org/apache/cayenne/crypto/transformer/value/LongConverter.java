package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between long and byte[] using big-endian encoding.
 *
 * @since 4.0
 */
public class LongConverter implements BytesConverter {

    public static final BytesConverter INSTANCE = new LongConverter();
    private static final int BYTES = 8;

    static long getLong(byte[] bytes) {
        if (bytes.length < BYTES) {
            return IntegerConverter.getInt(bytes);
        }

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single long value: " + bytes.length);
        }

        return (bytes[0] & 0xFFL) << 56
                | (bytes[1] & 0xFFL) << 48
                | (bytes[2] & 0xFFL) << 40
                | (bytes[3] & 0xFFL) << 32
                | (bytes[4] & 0xFFL) << 24
                | (bytes[5] & 0xFFL) << 16
                | (bytes[6] & 0xFFL) << 8
                | (bytes[7] & 0xFFL);
    }

    static byte[] getBytes(long k) {

        if (k <= Integer.MAX_VALUE) {
            return IntegerConverter.getBytes((int) k);
        }

        return new byte[]{
                (byte) (k >> 56),
                (byte) (k >> 48),
                (byte) (k >> 40),
                (byte) (k >> 32),
                (byte) (k >> 24),
                (byte) (k >> 16),
                (byte) (k >> 8),
                (byte) k};
    }

    @Override
    public Object fromBytes(byte[] bytes) {
        return getLong(bytes);
    }

    @Override
    public byte[] toBytes(Object value) {
        return getBytes(((Number) value).longValue());
    }
}
