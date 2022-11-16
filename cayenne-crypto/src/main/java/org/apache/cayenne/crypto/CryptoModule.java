/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.crypto;

import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.crypto.batch.CryptoBatchTranslatorFactoryDecorator;
import org.apache.cayenne.crypto.cipher.DefaultCipherFactory;
import org.apache.cayenne.crypto.key.JceksKeySource;
import org.apache.cayenne.crypto.map.CryptoDataMapLoader;
import org.apache.cayenne.crypto.reader.CryptoRowReaderFactoryDecorator;
import org.apache.cayenne.crypto.transformer.DefaultTransformerFactory;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.crypto.transformer.bytes.BytesTransformerFactory;
import org.apache.cayenne.crypto.transformer.bytes.DefaultBytesTransformerFactory;
import org.apache.cayenne.crypto.transformer.bytes.LazyBytesTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.Base64StringConverter;
import org.apache.cayenne.crypto.transformer.value.BigDecimalConverter;
import org.apache.cayenne.crypto.transformer.value.BigIntegerConverter;
import org.apache.cayenne.crypto.transformer.value.BooleanConverter;
import org.apache.cayenne.crypto.transformer.value.ByteConverter;
import org.apache.cayenne.crypto.transformer.value.BytesToBytesConverter;
import org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.DoubleConverter;
import org.apache.cayenne.crypto.transformer.value.DurationConverter;
import org.apache.cayenne.crypto.transformer.value.FloatConverter;
import org.apache.cayenne.crypto.transformer.value.IntegerConverter;
import org.apache.cayenne.crypto.transformer.value.LazyValueTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.LocalDateConverter;
import org.apache.cayenne.crypto.transformer.value.LocalDateTimeConverter;
import org.apache.cayenne.crypto.transformer.value.LocalTimeConverter;
import org.apache.cayenne.crypto.transformer.value.LongConverter;
import org.apache.cayenne.crypto.transformer.value.PeriodConverter;
import org.apache.cayenne.crypto.transformer.value.ShortConverter;
import org.apache.cayenne.crypto.transformer.value.Utf8StringConverter;
import org.apache.cayenne.crypto.transformer.value.UtilDateConverter;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Date;

/**
 * Contains cryptography extensions for Cayenne.
 *
 * @since 4.0
 */
public class CryptoModule implements Module {

    private static final String DEFAULT_CIPHER_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_MODE = "CBC";
    private static final String DEFAULT_CIPHER_PADDING = "PKCS5Padding";
    // same as default keystore password in java...
    // credentials are stored as char[] to potentially allow wiping them clean in memory...
    private static final char[] DEFAULT_KEY_PASSWORD = "changeit".toCharArray();
    private static final String DEFAULT_COLUMN_MAPPER_PATTERN = "^CRYPTO_";

    /**
     * Returns a new extender that helps to build a custom module that provides required configuration for
     * {@link CryptoModule}
     * as well as custom strategies overriding the defaults defined here.
     *
     * @return a new extender that helps to build a custom module that provides required configuration for {@link CryptoModule}.
     * @since 5.0
     */
    public static CryptoModuleExtender extend(Binder binder) {
        return new CryptoModuleExtender(binder);
    }

    @Override
    public void configure(Binder binder) {
        extend(binder)
                .cipherAlgorithm(DEFAULT_CIPHER_ALGORITHM)
                .cipherMode(DEFAULT_CIPHER_MODE)
                .cipherPadding(DEFAULT_CIPHER_PADDING)
                .cipherFactory(DefaultCipherFactory.class)

                .keyStore((String) null, DEFAULT_KEY_PASSWORD, null)
                .keySource(JceksKeySource.class)

                .valueTransformerFactory(DefaultValueTransformerFactory.class)
                .bytesTransformerFactory(DefaultBytesTransformerFactory.class)

                .columnMapper(DEFAULT_COLUMN_MAPPER_PATTERN)

                .objectToBytesConverter("byte[]", BytesToBytesConverter.INSTANCE)
                .objectToBytesConverter(String.class, Utf8StringConverter.INSTANCE)

                .objectToBytesConverter(Double.class, DoubleConverter.INSTANCE)
                .objectToBytesConverter(Double.TYPE, DoubleConverter.INSTANCE)
                .objectToBytesConverter(Float.class, FloatConverter.INSTANCE)
                .objectToBytesConverter(Float.TYPE, FloatConverter.INSTANCE)
                .objectToBytesConverter(Long.class, LongConverter.INSTANCE)
                .objectToBytesConverter(Long.TYPE, LongConverter.INSTANCE)
                .objectToBytesConverter(Integer.class, IntegerConverter.INSTANCE)
                .objectToBytesConverter(Integer.TYPE, IntegerConverter.INSTANCE)
                .objectToBytesConverter(Short.class, ShortConverter.INSTANCE)
                .objectToBytesConverter(Short.TYPE, ShortConverter.INSTANCE)
                .objectToBytesConverter(Byte.class, ByteConverter.INSTANCE)
                .objectToBytesConverter(Byte.TYPE, ByteConverter.INSTANCE)
                .objectToBytesConverter(Boolean.class, BooleanConverter.INSTANCE)
                .objectToBytesConverter(Boolean.TYPE, BooleanConverter.INSTANCE)

                .objectToBytesConverter(BigInteger.class, BigIntegerConverter.INSTANCE)
                .objectToBytesConverter(BigDecimal.class, BigDecimalConverter.INSTANCE)
                .objectToBytesConverter(Date.class, UtilDateConverter.INSTANCE)
                .objectToBytesConverter(LocalDate.class, LocalDateConverter.INSTANCE)
                .objectToBytesConverter(LocalTime.class, LocalTimeConverter.INSTANCE)
                .objectToBytesConverter(LocalDateTime.class, LocalDateTimeConverter.INSTANCE)
                .objectToBytesConverter(Duration.class, DurationConverter.INSTANCE)
                .objectToBytesConverter(Period.class, PeriodConverter.INSTANCE)

                .dbToBytesConverter(Types.BINARY, BytesToBytesConverter.INSTANCE)
                .dbToBytesConverter(Types.BLOB, BytesToBytesConverter.INSTANCE)
                .dbToBytesConverter(Types.VARBINARY, BytesToBytesConverter.INSTANCE)
                .dbToBytesConverter(Types.LONGVARBINARY, BytesToBytesConverter.INSTANCE)

                .dbToBytesConverter(Types.CHAR, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.NCHAR, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.CLOB, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.NCLOB, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.LONGVARCHAR, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.LONGNVARCHAR, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.VARCHAR, Base64StringConverter.INSTANCE)
                .dbToBytesConverter(Types.NVARCHAR, Base64StringConverter.INSTANCE);

        binder.bind(TransformerFactory.class).to(DefaultTransformerFactory.class);

        binder.decorate(DataMapLoader.class).before(CryptoDataMapLoader.class);
        binder.decorate(BatchTranslatorFactory.class).before(CryptoBatchTranslatorFactoryDecorator.class);
        binder.bind(RowReaderFactory.class).to(CryptoRowReaderFactoryDecorator.class);

        // decorate Crypto's own services to allow Cayenne to operate over plaintext entities even if crypto keys are
        // not available.
        binder.decorate(ValueTransformerFactory.class).after(LazyValueTransformerFactory.class);
        binder.decorate(BytesTransformerFactory.class).after(LazyBytesTransformerFactory.class);
    }
}
