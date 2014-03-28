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

import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.crypto.batch.CryptoBatchTranslatorFactoryDecorator;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.cipher.DefaultCipherFactory;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.reader.CryptoRowReaderFactoryDecorator;
import org.apache.cayenne.crypto.transformer.DefaultTransformerFactory;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.crypto.transformer.ValueTransformerFactory;
import org.apache.cayenne.di.Binder;
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

    private ColumnMapper columnMapper;
    private Class<? extends ColumnMapper> columnMapperType;

    private String cipherAlgoritm;
    private String cipherMode;
    private String cipherPadding;
    private Class<? extends CipherFactory> cipherFactoryType;

    public CryptoModuleBuilder() {

        // init some sensible defaults that work in JVM without extra
        // packages...
        this.cipherAlgoritm = DEFAULT_CIPHER_ALGORITHM;
        this.cipherMode = DEFAULT_CIPHER_MODE;
        this.cipherPadding = DEFAULT_CIPHER_PADDING;

        this.cipherFactoryType = DefaultCipherFactory.class;
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
        this.columnMapperType = columnMapperType;
        this.columnMapper = null;
        return this;
    }

    public CryptoModuleBuilder columnMapper(ColumnMapper columnMapper) {
        this.columnMapperType = null;
        this.columnMapper = columnMapper;
        return this;
    }

    /**
     * Produces a module that can be used to start Cayenne runtime.
     */
    public Module build() {

        if (valueTransformerFactoryType == null) {
            throw new IllegalStateException("'ValueTransformerFactory' is not initialized");
        }

        if (columnMapperType == null && columnMapper == null) {
            throw new IllegalStateException("'ColumnMapper' is not initialized");
        }

        if (cipherFactoryType == null) {
            throw new IllegalStateException("'CipherFactory' is not initialized");
        }

        return new Module() {

            @Override
            public void configure(Binder binder) {

                // init default cipher settings
                binder.bindMap(CryptoConstants.PROPERTIES_MAP).put(CryptoConstants.CIPHER_ALGORITHM, cipherAlgoritm)
                        .put(CryptoConstants.CIPHER_MODE, cipherMode)
                        .put(CryptoConstants.CIPHER_PADDING, cipherPadding);

                binder.bind(CipherFactory.class).to(cipherFactoryType);
                binder.bind(TransformerFactory.class).to(DefaultTransformerFactory.class);
                binder.bind(ValueTransformerFactory.class).to(valueTransformerFactoryType);

                if (columnMapperType != null) {
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
