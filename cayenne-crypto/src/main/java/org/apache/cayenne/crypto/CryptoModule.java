/*
 *    Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.crypto.batch.CryptoBatchTranslatorFactoryDecorator;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.cipher.DefaultCipherFactory;
import org.apache.cayenne.crypto.key.JceksKeySource;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.map.PatternColumnMapper;
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
import org.apache.cayenne.crypto.transformer.value.BytesConverter;
import org.apache.cayenne.crypto.transformer.value.BytesToBytesConverter;
import org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.DoubleConverter;
import org.apache.cayenne.crypto.transformer.value.FloatConverter;
import org.apache.cayenne.crypto.transformer.value.IntegerConverter;
import org.apache.cayenne.crypto.transformer.value.LazyValueTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.LongConverter;
import org.apache.cayenne.crypto.transformer.value.ShortConverter;
import org.apache.cayenne.crypto.transformer.value.Utf8StringConverter;
import org.apache.cayenne.crypto.transformer.value.UtilDateConverter;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Types;
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
    private static final char[] DEFAULT_KEY_PASSWORD = "changeit".toCharArray();
    private static final String DEFAULT_COLUMN_MAPPER_PATTERN = "^CRYPTO_";

    /**
     * Returns a new extender that helps to build a custom module that provides required configuration for {@link CryptoModule}
     * as well as custom strategies overriding the defaults defined here.
     *
     * @return a new extender that helps to build a custom module that provides required configuration for {@link CryptoModule}.
     */
    public static CryptoModuleExtender extend() {
        return new CryptoModuleExtender();
    }

    static MapBuilder<String> contributeProperties(Binder binder) {
        return binder.bindMap(String.class, CryptoConstants.PROPERTIES_MAP);
    }

    static MapBuilder<char[]> contributeCredentials(Binder binder) {
        return binder.bindMap(char[].class, CryptoConstants.CREDENTIALS_MAP);
    }

    @SuppressWarnings("unchecked")
    static MapBuilder<BytesConverter<?>> contributeDbToByteConverters(Binder binder) {
        MapBuilder mapBuilder = binder.bindMap(BytesConverter.class, DefaultValueTransformerFactory.DB_TO_BYTE_CONVERTERS_KEY);
        return (MapBuilder<BytesConverter<?>>) mapBuilder;
    }

    @SuppressWarnings("unchecked")
    static MapBuilder<BytesConverter<?>> contributeObjectToByteConverters(Binder binder) {
        MapBuilder mapBuilder = binder.bindMap(BytesConverter.class, DefaultValueTransformerFactory.OBJECT_TO_BYTE_CONVERTERS_KEY);
        return (MapBuilder<BytesConverter<?>>) mapBuilder;
    }

    @Override
    public void configure(Binder binder) {

        contributeProperties(binder)
                .put(CryptoConstants.CIPHER_ALGORITHM, DEFAULT_CIPHER_ALGORITHM)
                .put(CryptoConstants.CIPHER_MODE, DEFAULT_CIPHER_MODE)
                .put(CryptoConstants.CIPHER_PADDING, DEFAULT_CIPHER_PADDING);

        // credentials are stored as char[] to potentially allow wiping them clean in memory...
        contributeCredentials(binder).put(CryptoConstants.KEY_PASSWORD, DEFAULT_KEY_PASSWORD);

        binder.bind(CipherFactory.class).to(DefaultCipherFactory.class);
        binder.bind(TransformerFactory.class).to(DefaultTransformerFactory.class);
        binder.bind(ValueTransformerFactory.class).to(DefaultValueTransformerFactory.class);

        MapBuilder<BytesConverter<?>> dbToBytesBinder = contributeDbToByteConverters(binder);
        contributeDefaultDbConverters(dbToBytesBinder);

        MapBuilder<BytesConverter<?>> objectToBytesBinder = contributeObjectToByteConverters(binder);
        contributeDefaultObjectConverters(objectToBytesBinder);

        binder.bind(BytesTransformerFactory.class).to(DefaultBytesTransformerFactory.class);
        binder.bind(KeySource.class).to(JceksKeySource.class);
        binder.bind(ColumnMapper.class).toInstance(new PatternColumnMapper(DEFAULT_COLUMN_MAPPER_PATTERN));

        binder.decorate(BatchTranslatorFactory.class).before(CryptoBatchTranslatorFactoryDecorator.class);
        binder.decorate(RowReaderFactory.class).before(CryptoRowReaderFactoryDecorator.class);

        // decorate Crypto's own services to allow Cayenne to operate over plaintext entities even if crypto keys are
        // not available.
        binder.decorate(ValueTransformerFactory.class).after(LazyValueTransformerFactory.class);
        binder.decorate(BytesTransformerFactory.class).after(LazyBytesTransformerFactory.class);
    }

    private static void contributeDefaultDbConverters(MapBuilder<BytesConverter<?>> mapBuilder) {

        mapBuilder.put(String.valueOf(Types.BINARY), BytesToBytesConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.BLOB), BytesToBytesConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.VARBINARY), BytesToBytesConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.LONGVARBINARY), BytesToBytesConverter.INSTANCE);

        mapBuilder.put(String.valueOf(Types.CHAR), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.NCHAR), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.CLOB), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.NCLOB), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.LONGVARCHAR), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.LONGNVARCHAR), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.VARCHAR), Base64StringConverter.INSTANCE);
        mapBuilder.put(String.valueOf(Types.NVARCHAR), Base64StringConverter.INSTANCE);
    }

    private static void contributeDefaultObjectConverters(MapBuilder<BytesConverter<?>> mapBuilder) {

        mapBuilder.put("byte[]", BytesToBytesConverter.INSTANCE);
        mapBuilder.put(String.class.getName(), Utf8StringConverter.INSTANCE);

        mapBuilder.put(Double.class.getName(), DoubleConverter.INSTANCE);
        mapBuilder.put(Double.TYPE.getName(), DoubleConverter.INSTANCE);

        mapBuilder.put(Float.class.getName(), FloatConverter.INSTANCE);
        mapBuilder.put(Float.TYPE.getName(), FloatConverter.INSTANCE);

        mapBuilder.put(Long.class.getName(), LongConverter.INSTANCE);
        mapBuilder.put(Long.TYPE.getName(), LongConverter.INSTANCE);

        mapBuilder.put(Integer.class.getName(), IntegerConverter.INSTANCE);
        mapBuilder.put(Integer.TYPE.getName(), IntegerConverter.INSTANCE);

        mapBuilder.put(Short.class.getName(), ShortConverter.INSTANCE);
        mapBuilder.put(Short.TYPE.getName(), ShortConverter.INSTANCE);

        mapBuilder.put(Byte.class.getName(), ByteConverter.INSTANCE);
        mapBuilder.put(Byte.TYPE.getName(), ByteConverter.INSTANCE);

        mapBuilder.put(Boolean.class.getName(), BooleanConverter.INSTANCE);
        mapBuilder.put(Boolean.TYPE.getName(), BooleanConverter.INSTANCE);

        mapBuilder.put(Date.class.getName(), UtilDateConverter.INSTANCE);
        mapBuilder.put(BigInteger.class.getName(), BigIntegerConverter.INSTANCE);
        mapBuilder.put(BigDecimal.class.getName(), BigDecimalConverter.INSTANCE);
    }
}
