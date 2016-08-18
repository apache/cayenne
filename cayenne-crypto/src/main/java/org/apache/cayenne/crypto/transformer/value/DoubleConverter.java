package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between double and byte[]
 *
 * @since 4.0
 */
public class DoubleConverter implements BytesConverter<Double> {

    public static final BytesConverter<Double> INSTANCE = new DoubleConverter();
    private static final int BYTES = 8;

    static double getDouble(byte[] bytes) {

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single double value: " + bytes.length);
        }

        return Double.longBitsToDouble(LongConverter.getLong(bytes));
    }

    static byte[] getBytes(Double d) {
        return LongConverter.getBytes(Double.doubleToLongBits(d));
    }

    @Override
    public Double fromBytes(byte[] bytes) {
        return getDouble(bytes);
    }

    @Override
    public byte[] toBytes(Double value) {
        return getBytes(value);
    }
}
