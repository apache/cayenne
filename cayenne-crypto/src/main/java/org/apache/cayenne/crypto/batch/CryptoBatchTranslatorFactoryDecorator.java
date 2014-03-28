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
package org.apache.cayenne.crypto.batch;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.crypto.cipher.CryptoFactory;
import org.apache.cayenne.crypto.cipher.Encryptor;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * @since 3.2
 */
public class CryptoBatchTranslatorFactoryDecorator implements BatchTranslatorFactory {

    private static final PositionalEncryptor[] EMPTY_ENCRYPTORS = new PositionalEncryptor[0];

    private ColumnMapper columnMapper;
    private CryptoFactory cryptoFactory;
    private BatchTranslatorFactory delegate;

    public CryptoBatchTranslatorFactoryDecorator(@Inject BatchTranslatorFactory delegate,
            @Inject CryptoFactory cryptoFactory, @Inject ColumnMapper columnMapper) {

        this.cryptoFactory = cryptoFactory;
        this.columnMapper = columnMapper;
        this.delegate = delegate;
    }

    @Override
    public BatchTranslator translator(BatchQuery query, DbAdapter adapter, String trimFunction) {
        final BatchTranslator delegateTranslator = delegate.translator(query, adapter, trimFunction);

        return new BatchTranslator() {

            private int len;
            private PositionalEncryptor[] encryptors;

            private void ensureEncryptorsCompiled() {
                if (this.encryptors == null) {
                    BatchParameterBinding[] bindings = getBindings();

                    int len = bindings.length;
                    List<PositionalEncryptor> encList = null;

                    for (int i = 0; i < len; i++) {

                        DbAttribute a = bindings[i].getAttribute();
                        if (columnMapper.isEncrypted(a)) {

                            if (encList == null) {
                                encList = new ArrayList<PositionalEncryptor>(len - i);
                            }

                            Encryptor e = cryptoFactory.getEncryptor(a);
                            encList.add(new PositionalEncryptor(i, e));
                        }
                    }

                    this.encryptors = encList == null ? EMPTY_ENCRYPTORS : encList
                            .toArray(new PositionalEncryptor[encList.size()]);
                    this.len = encryptors.length;
                }
            }

            @Override
            public String getSql() {
                return delegateTranslator.getSql();
            }

            @Override
            public BatchParameterBinding[] getBindings() {
                return delegateTranslator.getBindings();
            }

            @Override
            public BatchParameterBinding[] updateBindings(BatchQueryRow row) {

                ensureEncryptorsCompiled();

                BatchParameterBinding[] bindings = delegateTranslator.updateBindings(row);

                for (int i = 0; i < len; i++) {
                    BatchParameterBinding b = bindings[encryptors[i].position];
                    Object encrypted = encryptors[i].encryptor.encrypt(b.getValue());
                    b.setValue(encrypted);
                }

                return bindings;
            }
        };
    }

    class PositionalEncryptor {
        final int position;
        final Encryptor encryptor;

        PositionalEncryptor(int position, Encryptor encryptor) {
            this.position = position;
            this.encryptor = encryptor;
        }
    }
}
