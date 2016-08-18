package org.apache.cayenne.crypto.transformer.value;

/**
 * Converts between float and byte[]
 *
 * @since 4.0
 */
public class FloatConverter implements BytesConverter<Float> {

    public static final BytesConverter<Float> INSTANCE = new FloatConverter();
    private static final int BYTES = 4;

    static float getFloat(byte[] bytes) {

        if (bytes.length > BYTES) {
            throw new IllegalArgumentException("byte[] is too large for a single float value: " + bytes.length);
        }

        return Float.intBitsToFloat(IntegerConverter.getInt(bytes));
    }

    static byte[] getBytes(float f) {
        return IntegerConverter.getBytes(Float.floatToRawIntBits(f));
    }

    @Override
    public Float fromBytes(byte[] bytes) {
        return getFloat(bytes);
    }

    @Override
    public byte[] toBytes(Float value) {
        return getBytes(value);
    }
}
