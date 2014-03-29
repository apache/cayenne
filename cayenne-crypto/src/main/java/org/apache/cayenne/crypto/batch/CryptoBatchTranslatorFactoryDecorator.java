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

import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.crypto.transformer.BindingsTransformer;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * @since 3.2
 */
public class CryptoBatchTranslatorFactoryDecorator implements BatchTranslatorFactory {

    private TransformerFactory cryptoFactory;
    private BatchTranslatorFactory delegate;

    public CryptoBatchTranslatorFactoryDecorator(@Inject BatchTranslatorFactory delegate,
            @Inject TransformerFactory cryptoFactory) {

        this.cryptoFactory = cryptoFactory;
        this.delegate = delegate;
    }

    @Override
    public BatchTranslator translator(BatchQuery query, DbAdapter adapter, String trimFunction) {
        final BatchTranslator delegateTranslator = delegate.translator(query, adapter, trimFunction);

        return new BatchTranslator() {

            private boolean encryptorCompiled;
            private BindingsTransformer encryptor;

            private void ensureEncryptorCompiled() {
                if (!encryptorCompiled) {
                    encryptor = cryptoFactory.encryptor(getBindings());
                    encryptorCompiled = true;
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

                ensureEncryptorCompiled();

                BatchParameterBinding[] bindings = delegateTranslator.updateBindings(row);

                if (encryptor != null) {
                    encryptor.transform(bindings);
                }

                return bindings;
            }
        };
    }

}
