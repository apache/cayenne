package org.apache.cayenne.crypto.transformer.value;

import java.math.BigInteger;

/**
 * Converts between {@link java.math.BigInteger} and byte[]
 *
 * @since 4.0
 */
public class BigIntegerConverter implements BytesConverter<BigInteger> {

    public static final BytesConverter<BigInteger> INSTANCE = new BigIntegerConverter();

    static BigInteger getBigInteger(byte[] bytes) {
        return new BigInteger(bytes);
    }

    static byte[] getBytes(BigInteger bigInt) {
        return bigInt.toByteArray();
    }

    @Override
    public BigInteger fromBytes(byte[] bytes) {
        return getBigInteger(bytes);
    }

    @Override
    public byte[] toBytes(BigInteger value) {
        return getBytes(value);
    }
}
