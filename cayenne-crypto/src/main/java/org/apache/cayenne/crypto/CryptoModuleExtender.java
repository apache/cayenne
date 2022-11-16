/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.crypto;

import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.map.PatternColumnMapper;
import org.apache.cayenne.crypto.transformer.bytes.BytesTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.BytesConverter;
import org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * A builder of custom extensions and required configuration for {@link CryptoModule} module.
 *
 * @since 4.0
 */
public class CryptoModuleExtender {

    private final Binder binder;

    private MapBuilder<String> properties;
    private MapBuilder<char[]> credentials;
    private MapBuilder<BytesConverter<?>> dbToByteConverters;
    private MapBuilder<BytesConverter<?>> objectToByteConverters;

    protected CryptoModuleExtender(Binder binder) {
        this.binder = binder;
    }

    public CryptoModuleExtender cipherAlgorithm(String algorithm) {
        contributeProperties(binder).put(CryptoConstants.CIPHER_ALGORITHM, algorithm);
        return this;
    }

    public CryptoModuleExtender cipherMode(String mode) {
        contributeProperties(binder).put(CryptoConstants.CIPHER_MODE, mode);
        return this;
    }

    public CryptoModuleExtender cipherPadding(String padding) {
        contributeProperties(binder).put(CryptoConstants.CIPHER_PADDING, padding);
        return this;
    }

    public CryptoModuleExtender cipherFactory(Class<? extends CipherFactory> factoryType) {
        binder.bind(CipherFactory.class).to(factoryType);
        return this;
    }

    public CryptoModuleExtender valueTransformer(Class<? extends ValueTransformerFactory> factoryType) {
        binder.bind(ValueTransformerFactory.class).to(factoryType);
        return this;
    }

    public CryptoModuleExtender bytesTransformer(Class<? extends BytesTransformerFactory> factoryType) {
        binder.bind(BytesTransformerFactory.class).to(factoryType);
        return this;
    }

    public <T> CryptoModuleExtender objectToBytesConverter(Class<T> objectType, BytesConverter<T> converter) {
        contributeObjectToByteConverters(binder).put(typeName(objectType), Objects.requireNonNull(converter));
        return this;
    }

    public CryptoModuleExtender dbToBytesConverter(int sqlType, BytesConverter<?> converter) {
        contributeDbToByteConverters(binder).put(String.valueOf(sqlType), Objects.requireNonNull(converter));
        return this;
    }

    public CryptoModuleExtender columnMapper(Class<? extends ColumnMapper> columnMapperType) {
        binder.bind(ColumnMapper.class).to(columnMapperType);
        return this;
    }

    public CryptoModuleExtender columnMapper(ColumnMapper columnMapper) {
        binder.bind(ColumnMapper.class).toInstance(columnMapper);
        return this;
    }

    public CryptoModuleExtender columnMapper(String pattern) {
        binder.bind(ColumnMapper.class).toInstance(new PatternColumnMapper(pattern));
        return this;
    }

    /**
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleExtender encryptionKeyAlias(String encryptionKeyAlias) {
        contributeProperties(binder).put(CryptoConstants.ENCRYPTION_KEY_ALIAS, encryptionKeyAlias);
        return this;
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently, all
     * keys must be protected with the same password.
     *
     * @param file               A file to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleExtender keyStore(File file, char[] passwordForAllKeys, String encryptionKeyAlias) {
        try {
            String fileUrl = file.toURI().toURL().toExternalForm();
            return keyStore(fileUrl, passwordForAllKeys, encryptionKeyAlias);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid keyStore file", e);
        }
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently, all
     * keys must be protected with the same password.
     *
     * @param url                A URL to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleExtender keyStore(URL url, char[] passwordForAllKeys, String encryptionKeyAlias) {
        return keyStore(url.toExternalForm(), passwordForAllKeys, encryptionKeyAlias);
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently, all
     * keys must be protected with the same password.
     *
     * @param url                A URL to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleExtender keyStore(String url, char[] passwordForAllKeys, String encryptionKeyAlias) {
        MapBuilder<String> propertiesBuilder = contributeProperties(binder);
        propertiesBuilder.put(CryptoConstants.KEYSTORE_URL, url);
        propertiesBuilder.put(CryptoConstants.ENCRYPTION_KEY_ALIAS, encryptionKeyAlias);
        contributeCredentials(binder).put(CryptoConstants.KEY_PASSWORD, passwordForAllKeys);
        return this;
    }

    public CryptoModuleExtender keySource(Class<? extends KeySource> keySourceType) {
        binder.bind(KeySource.class).to(keySourceType);
        return this;
    }

    public CryptoModuleExtender keySource(KeySource keySource) {
        binder.bind(KeySource.class).toInstance(keySource);
        return this;
    }

    public CryptoModuleExtender compress() {
        contributeProperties(binder).put(CryptoConstants.COMPRESSION, "true");
        return this;
    }

    /**
     * Enable authentication codes
     */
    public CryptoModuleExtender useHMAC() {
        contributeProperties(binder).put(CryptoConstants.USE_HMAC, "true");
        return this;
    }

    private MapBuilder<String> contributeProperties(Binder binder) {
        if (properties == null) {
            properties = binder.bindMap(String.class, CryptoConstants.PROPERTIES_MAP);
        }
        return properties;
    }

    private MapBuilder<char[]> contributeCredentials(Binder binder) {
        if (credentials == null) {
            credentials = binder.bindMap(char[].class, CryptoConstants.CREDENTIALS_MAP);
        }
        return credentials;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private MapBuilder<BytesConverter<?>> contributeDbToByteConverters(Binder binder) {
        if (dbToByteConverters == null) {
            MapBuilder mapBuilder = binder.bindMap(BytesConverter.class,
                                                   DefaultValueTransformerFactory.DB_TO_BYTE_CONVERTERS_KEY);
            dbToByteConverters = mapBuilder;
        }
        return dbToByteConverters;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private MapBuilder<BytesConverter<?>> contributeObjectToByteConverters(Binder binder) {
        if (objectToByteConverters == null) {
            MapBuilder mapBuilder = binder.bindMap(BytesConverter.class,
                                                   DefaultValueTransformerFactory.OBJECT_TO_BYTE_CONVERTERS_KEY);
            objectToByteConverters = mapBuilder;
        }
        return objectToByteConverters;
    }

    /**
     * Get a name of the provided Java type.
     * Consistent with the logic of {@link org.apache.cayenne.di.AdhocObjectFactory#getJavaClass(String)} method
     * @param objectType to get a name of
     * @return a name of the type
     * @param <T> the type of the class
     */
    static <T> String typeName(Class<T> objectType) {
        if(objectType.isArray()) {
            if(objectType.getComponentType().isPrimitive()) {
                return objectType.getComponentType().getSimpleName() + "[]";
            } else {
                return objectType.getComponentType().getName() + "[]";
            }
        }
        return objectType.getName();
    }
}
