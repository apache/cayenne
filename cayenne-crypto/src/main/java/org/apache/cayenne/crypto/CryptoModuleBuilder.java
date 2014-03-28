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
import org.apache.cayenne.crypto.cipher.CryptoFactory;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.reader.CryptoRowReaderFactoryDecorator;
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

    private Class<? extends CryptoFactory> cryptoFactoryType;

    private ColumnMapper columnMapper;
    private Class<? extends ColumnMapper> columnMapperType;

    public CryptoModuleBuilder cryptoFactory(Class<? extends CryptoFactory> cryptoFactoryType) {
        this.cryptoFactoryType = cryptoFactoryType;
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

        if (cryptoFactoryType == null) {
            throw new IllegalStateException("'CryptoHandler' is not initialized");
        }

        if (columnMapperType == null && columnMapper == null) {
            throw new IllegalStateException("'ColumnMapper' is not initialized");
        }

        return new Module() {

            @Override
            public void configure(Binder binder) {
                binder.bind(CryptoFactory.class).to(cryptoFactoryType);

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
