/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A builder that allows to customize {@link CryptoModule} module.
 *
 * @since 4.0
 */
public class CryptoModuleBuilder {

    private Class<? extends ValueTransformerFactory> valueTransformerFactoryType;
    private Class<? extends BytesTransformerFactory> bytesTransformerFactoryType;

    private Map<String, BytesConverter<?>> extraObjectToBytes;
    private Map<Integer, BytesConverter<?>> extraDbToBytes;

    private String columnMapperPattern;
    private ColumnMapper columnMapper;
    private Class<? extends ColumnMapper> columnMapperType;

    private String cipherAlgoritm;
    private String cipherMode;
    private Class<? extends CipherFactory> cipherFactoryType;

    private URL keyStoreUrl;
    private String keyStoreUrlString;
    private File keyStoreFile;
    private Class<? extends KeySource> keySourceType;
    private KeySource keySource;

    private String encryptionKeyAlias;
    private char[] keyPassword;

    private boolean compress;

    // use CryptoModule.builder() to create the builder...
    protected CryptoModuleBuilder() {
        this.extraDbToBytes = new HashMap<>();
        this.extraObjectToBytes = new HashMap<>();
    }

    public CryptoModuleBuilder cipherAlgorithm(String algorithm) {
        this.cipherAlgoritm = Objects.requireNonNull(algorithm);
        return this;
    }

    public CryptoModuleBuilder cipherMode(String mode) {
        this.cipherMode = Objects.requireNonNull(mode);
        return this;
    }

    public CryptoModuleBuilder cipherFactory(Class<? extends CipherFactory> factoryType) {
        this.cipherFactoryType = Objects.requireNonNull(factoryType);
        return this;
    }

    public CryptoModuleBuilder valueTransformer(Class<? extends ValueTransformerFactory> factoryType) {
        this.valueTransformerFactoryType = Objects.requireNonNull(factoryType);
        return this;
    }

    public <T> CryptoModuleBuilder objectToBytesConverter(Class<T> objectType, BytesConverter<T> converter) {
        extraObjectToBytes.put(objectType.getName(), Objects.requireNonNull(converter));
        return this;
    }

    public CryptoModuleBuilder dbToBytesConverter(int sqlType, BytesConverter<?> converter) {
        extraDbToBytes.put(sqlType, Objects.requireNonNull(converter));
        return this;
    }

    public CryptoModuleBuilder bytesTransformer(Class<? extends BytesTransformerFactory> factoryType) {
        this.bytesTransformerFactoryType = Objects.requireNonNull(factoryType);
        return this;
    }

    public CryptoModuleBuilder columnMapper(Class<? extends ColumnMapper> columnMapperType) {
        this.columnMapperPattern = null;
        this.columnMapperType = Objects.requireNonNull(columnMapperType);
        this.columnMapper = null;
        return this;
    }

    public CryptoModuleBuilder columnMapper(ColumnMapper columnMapper) {
        this.columnMapperPattern = null;
        this.columnMapperType = null;
        this.columnMapper = Objects.requireNonNull(columnMapper);
        return this;
    }

    public CryptoModuleBuilder columnMapper(String pattern) {
        this.columnMapperPattern = Objects.requireNonNull(pattern);
        this.columnMapperType = null;
        this.columnMapper = null;
        return this;
    }

    /**
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleBuilder encryptionKeyAlias(String encryptionKeyAlias) {
        this.encryptionKeyAlias = Objects.requireNonNull(encryptionKeyAlias);
        return this;
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently all
     * keys must be protected with the same password.
     *
     * @param file               A file to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleBuilder keyStore(File file, char[] passwordForAllKeys, String encryptionKeyAlias) {
        this.encryptionKeyAlias = encryptionKeyAlias;
        this.keyPassword = passwordForAllKeys;
        this.keyStoreUrl = null;
        this.keyStoreUrlString = null;
        this.keyStoreFile = Objects.requireNonNull(file);
        return this;
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently all
     * keys must be protected with the same password.
     *
     * @param url                A URL to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleBuilder keyStore(String url, char[] passwordForAllKeys, String encryptionKeyAlias) {
        this.encryptionKeyAlias = encryptionKeyAlias;
        this.keyPassword = passwordForAllKeys;
        this.keyStoreUrl = null;
        this.keyStoreUrlString = Objects.requireNonNull(url);
        this.keyStoreFile = null;
        return this;
    }

    /**
     * Configures keystore parameters. The KeyStore must be of "jceks" type and
     * contain all needed secret keys for the target database. Currently all
     * keys must be protected with the same password.
     *
     * @param url                A URL to load keystore from.
     * @param passwordForAllKeys A password that unlocks all keys in the keystore.
     * @param encryptionKeyAlias The name of the key in the keystore that should be used for
     *                           encryption by default.
     */
    public CryptoModuleBuilder keyStore(URL url, char[] passwordForAllKeys, String encryptionKeyAlias) {
        this.encryptionKeyAlias = encryptionKeyAlias;
        this.keyPassword = passwordForAllKeys;
        this.keyStoreUrl = Objects.requireNonNull(url);
        this.keyStoreUrlString = null;
        this.keyStoreFile = null;
        return this;
    }

    public CryptoModuleBuilder keySource(Class<? extends KeySource> type) {
        this.keySourceType = Objects.requireNonNull(type);
        this.keySource = null;
        return this;
    }

    public CryptoModuleBuilder keySource(KeySource keySource) {
        this.keySourceType = null;
        this.keySource = Objects.requireNonNull(keySource);
        return this;
    }

    public CryptoModuleBuilder compress() {
        this.compress = true;
        return this;
    }

    /**
     * Produces a module that can be used to start Cayenne runtime.
     */
    public Module build() {

        return new Module() {

            @Override
            public void configure(Binder binder) {

                MapBuilder<String> props = CryptoModule.contributeProperties(binder);

                if (cipherAlgoritm != null) {
                    props.put(CryptoConstants.CIPHER_ALGORITHM, cipherAlgoritm);
                }

                if (cipherMode != null) {
                    props.put(CryptoConstants.CIPHER_MODE, cipherMode);
                }

                String keyStoreUrl = keyStoreUrl();
                if (keyStoreUrl != null) {
                    props.put(CryptoConstants.KEYSTORE_URL, keyStoreUrl);
                }

                if (encryptionKeyAlias != null) {
                    props.put(CryptoConstants.ENCRYPTION_KEY_ALIAS, encryptionKeyAlias);
                }

                if (compress) {
                    props.put(CryptoConstants.COMPRESSION, "true");
                }

                if (keyPassword != null) {
                    CryptoModule.contributeCredentials(binder).put(CryptoConstants.KEY_PASSWORD, keyPassword);
                }

                if (cipherFactoryType != null) {
                    binder.bind(CipherFactory.class).to(cipherFactoryType);
                }

                if (valueTransformerFactoryType != null) {
                    binder.bind(ValueTransformerFactory.class).to(valueTransformerFactoryType);
                }

                if (!extraDbToBytes.isEmpty()) {
                    MapBuilder<BytesConverter<?>> dbToBytesBinder = CryptoModule.contributeDbToByteConverters(binder);
                    for (Map.Entry<Integer, BytesConverter<?>> extraConverter : extraDbToBytes.entrySet()) {
                        dbToBytesBinder.put(extraConverter.getKey().toString(), extraConverter.getValue());
                    }
                }

                if (!extraObjectToBytes.isEmpty()) {
                    MapBuilder<BytesConverter<?>> objectToBytesBinder = CryptoModule.contributeObjectToByteConverters(binder);
                    for (Map.Entry<String, BytesConverter<?>> extraConverter : extraObjectToBytes.entrySet()) {
                        objectToBytesBinder.put(extraConverter.getKey(), extraConverter.getValue());
                    }
                }

                if (bytesTransformerFactoryType != null) {
                    binder.bind(BytesTransformerFactory.class).to(bytesTransformerFactoryType);
                }

                if (keySource != null) {
                    binder.bind(KeySource.class).toInstance(keySource);
                } else if (keySourceType != null) {
                    binder.bind(KeySource.class).to(keySourceType);
                }

                if (columnMapperPattern != null) {
                    binder.bind(ColumnMapper.class).toInstance(new PatternColumnMapper(columnMapperPattern));
                } else if (columnMapperType != null) {
                    binder.bind(ColumnMapper.class).to(columnMapperType);
                } else if (columnMapper != null) {
                    binder.bind(ColumnMapper.class).toInstance(columnMapper);
                }
            }
        };
    }

    protected String keyStoreUrl() {
        if (this.keyStoreUrl != null) {
            return this.keyStoreUrl.toExternalForm();
        }

        if (this.keyStoreUrlString != null) {
            return this.keyStoreUrlString;
        }

        if (keyStoreFile != null) {
            try {
                return keyStoreFile.toURI().toURL().toExternalForm();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Invalid keyStore file", e);
            }
        }

        return null;
    }


}
