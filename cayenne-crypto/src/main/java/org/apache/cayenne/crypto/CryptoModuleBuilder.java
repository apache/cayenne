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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.crypto.batch.CryptoBatchTranslatorFactoryDecorator;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.cipher.DefaultCipherFactory;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.crypto.key.JceksKeySource;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.map.PatternColumnMapper;
import org.apache.cayenne.crypto.reader.CryptoRowReaderFactoryDecorator;
import org.apache.cayenne.crypto.transformer.DefaultTransformerFactory;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

/**
 * A builder of a Cayenne DI module that will contain all extension to Cayenne
 * runtime needed to enable encryption of certain data columns. Builder allows
 * to specify custom ciphers, as well as a strategy for discovering which
 * columns are encrypted.
 * 
 * @since 3.2
 */
public class CryptoModuleBuilder {

    private static final String DEFAULT_CIPHER_ALGORITHM = "AES";
    private static final String DEFAULT_CIPHER_MODE = "CBC";
    private static final String DEFAULT_CIPHER_PADDING = "PKCS5Padding";

    private Class<? extends ValueTransformerFactory> valueTransformerFactoryType;

    private String columnMapperPattern;
    private ColumnMapper columnMapper;
    private Class<? extends ColumnMapper> columnMapperType;

    private String cipherAlgoritm;
    private String cipherMode;
    private String cipherPadding;
    private Class<? extends CipherFactory> cipherFactoryType;

    private URL keyStoreUrl;
    private String keyStoreUrlString;
    private File keyStoreFile;
    private Class<? extends KeySource> keySourceType;

    private char[] keyPassword;

    public CryptoModuleBuilder() {

        // init some sensible defaults that work in JVM without extra
        // packages...
        this.cipherAlgoritm = DEFAULT_CIPHER_ALGORITHM;
        this.cipherMode = DEFAULT_CIPHER_MODE;
        this.cipherPadding = DEFAULT_CIPHER_PADDING;

        this.cipherFactoryType = DefaultCipherFactory.class;
        this.keySourceType = JceksKeySource.class;

        this.columnMapperPattern = "^CRYPTO_";
    }

    public CryptoModuleBuilder cipherAlgorithm(String algorithm) {
        this.cipherAlgoritm = algorithm;
        return this;
    }

    public CryptoModuleBuilder cipherMode(String mode) {
        this.cipherMode = mode;
        return this;
    }

    public CryptoModuleBuilder cipherFactory(Class<? extends CipherFactory> factoryType) {
        this.cipherFactoryType = factoryType;
        return this;
    }

    public CryptoModuleBuilder valueTransformer(Class<? extends ValueTransformerFactory> factoryType) {
        this.valueTransformerFactoryType = factoryType;
        return this;
    }

    public CryptoModuleBuilder columnMapper(Class<? extends ColumnMapper> columnMapperType) {
        this.columnMapperPattern = null;
        this.columnMapperType = columnMapperType;
        this.columnMapper = null;
        return this;
    }

    public CryptoModuleBuilder columnMapper(ColumnMapper columnMapper) {
        this.columnMapperPattern = null;
        this.columnMapperType = null;
        this.columnMapper = columnMapper;
        return this;
    }

    public CryptoModuleBuilder columnMapper(String pattern) {
        this.columnMapperPattern = pattern;
        this.columnMapperType = null;
        this.columnMapper = null;
        return this;
    }

    /**
     * Sets a password that unlocks a secret key.
     */
    public CryptoModuleBuilder keyPassword(char[] password) {
        this.keyPassword = password;
        return this;
    }

    /**
     * Instructs builder to use a given file to load keystore data. The KeyStore
     * must be of "jceks" type and contain all needed secret keys for the target
     * database.
     */
    public CryptoModuleBuilder keyStore(File file) {
        this.keyStoreUrl = null;
        this.keyStoreUrlString = null;
        this.keyStoreFile = file;
        return this;
    }

    /**
     * Instructs builder to use a given URL to load keystore data. The KeyStore
     * must be of "jceks" type and contain all needed secret keys for the target
     * database.
     */
    public CryptoModuleBuilder keyStore(String url) {
        this.keyStoreUrl = null;
        this.keyStoreUrlString = url;
        this.keyStoreFile = null;
        return this;
    }

    /**
     * Instructs builder to use a given URL to load keystore data. The KeyStore
     * must be of "jceks" type and contain all needed secret keys for the target
     * database.
     */
    public CryptoModuleBuilder keyStore(URL url) {
        this.keyStoreUrl = url;
        this.keyStoreUrlString = null;
        this.keyStoreFile = null;
        return this;
    }

    public CryptoModuleBuilder keySource(Class<? extends KeySource> type) {
        this.keySourceType = type;
        return this;
    }

    /**
     * Produces a module that can be used to start Cayenne runtime.
     */
    public Module build() {

        if (valueTransformerFactoryType == null) {
            throw new IllegalStateException("'ValueTransformerFactory' is not initialized");
        }

        if (columnMapperType == null && columnMapper == null && columnMapperPattern == null) {
            throw new IllegalStateException("'ColumnMapper' is not initialized");
        }

        if (cipherFactoryType == null) {
            throw new IllegalStateException("'CipherFactory' is not initialized");
        }

        return new Module() {

            @Override
            public void configure(Binder binder) {

                String keyStoreUrl = null;
                if (CryptoModuleBuilder.this.keyStoreUrl != null) {
                    keyStoreUrl = CryptoModuleBuilder.this.keyStoreUrl.toExternalForm();
                } else if (CryptoModuleBuilder.this.keyStoreUrlString != null) {
                    keyStoreUrl = CryptoModuleBuilder.this.keyStoreUrlString;
                } else if (keyStoreFile != null) {
                    try {
                        keyStoreUrl = keyStoreFile.toURI().toURL().toExternalForm();
                    } catch (MalformedURLException e) {
                        throw new IllegalStateException("Invalid keyStore file", e);
                    }
                }

                // String properties
                MapBuilder<String> props = binder.<String> bindMap(CryptoConstants.PROPERTIES_MAP)
                        .put(CryptoConstants.CIPHER_ALGORITHM, cipherAlgoritm)
                        .put(CryptoConstants.CIPHER_MODE, cipherMode)
                        .put(CryptoConstants.CIPHER_PADDING, cipherPadding);

                if (keyStoreUrl != null) {
                    props.put(CryptoConstants.KEYSTORE_URL, keyStoreUrl);
                }

                // char[] credentials... stored as char[] to potentially allow
                // wiping them clean in memory...
                MapBuilder<char[]> creds = binder.<char[]> bindMap(CryptoConstants.CREDENTIALS_MAP);

                if (keyPassword != null) {
                    creds.put(CryptoConstants.KEY_PASSWORD, keyPassword);
                }

                binder.bind(CipherFactory.class).to(cipherFactoryType);
                binder.bind(TransformerFactory.class).to(DefaultTransformerFactory.class);
                binder.bind(ValueTransformerFactory.class).to(valueTransformerFactoryType);
                binder.bind(KeySource.class).to(keySourceType);

                if (columnMapperPattern != null) {
                    binder.bind(ColumnMapper.class).toInstance(new PatternColumnMapper(columnMapperPattern));
                } else if (columnMapperType != null) {
                    binder.bind(ColumnMapper.class).to(columnMapperType);
                } else {
                    binder.bind(ColumnMapper.class).toInstance(columnMapper);
                }

                binder.decorate(BatchTranslatorFactory.class).before(CryptoBatchTranslatorFactoryDecorator.class);
                binder.decorate(RowReaderFactory.class).before(CryptoRowReaderFactoryDecorator.class);
            }
        };
    }
}
