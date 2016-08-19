package org.apache.cayenne.crypto.transformer.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Converts between {@link java.math.BigDecimal} and byte[]
 *
 * @since 4.0
 */
public class BigDecimalConverter implements BytesConverter<BigDecimal> {

    public static final BytesConverter<BigDecimal> INSTANCE = new BigDecimalConverter();

    private static final int MIN_BYTES = Integer.BYTES + 1;

    static BigDecimal getBigDecimal(byte[] bytes) {

        byte[] unscaledBytes, scaleBytes;

        if (bytes.length < MIN_BYTES) {
            throw new IllegalArgumentException("byte[] is too small for a BigDecimal value: " + bytes.length);
        }

        scaleBytes = Arrays.copyOfRange(bytes, 0, Integer.BYTES);
        unscaledBytes = Arrays.copyOfRange(bytes, Integer.BYTES, bytes.length);

        return new BigDecimal(new BigInteger(unscaledBytes), IntegerConverter.getInt(scaleBytes));
    }

    static byte[] getBytes(BigDecimal bigDecimal) {

        byte[] result, unscaledBytes, scaleBytes;

        unscaledBytes = bigDecimal.unscaledValue().toByteArray();
        scaleBytes = IntegerConverter.getBytes(bigDecimal.scale());

        result = new byte[Integer.BYTES + unscaledBytes.length];
        System.arraycopy(scaleBytes, 0, result, Integer.BYTES - scaleBytes.length, scaleBytes.length);
        System.arraycopy(unscaledBytes, 0, result, Integer.BYTES, unscaledBytes.length);

        return result;
    }

    @Override
    public BigDecimal fromBytes(byte[] bytes) {
        return getBigDecimal(bytes);
    }

    @Override
    public byte[] toBytes(BigDecimal value) {
        return getBytes(value);
    }
}
