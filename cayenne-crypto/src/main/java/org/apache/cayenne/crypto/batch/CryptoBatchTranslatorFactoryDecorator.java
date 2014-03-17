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

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.access.translator.batch.BatchTranslator;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.crypto.cipher.CipherService;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

/**
 * @since 3.2
 */
public class CryptoBatchTranslatorFactoryDecorator implements BatchTranslatorFactory {

    private CipherService cipherService;
    private ColumnMapper columnMapper;
    private BatchTranslatorFactory delegate;

    public CryptoBatchTranslatorFactoryDecorator(@Inject BatchTranslatorFactory delegate,
            @Inject CipherService cipherService, @Inject ColumnMapper columnMapper) {

        this.columnMapper = columnMapper;
        this.cipherService = cipherService;
        this.delegate = delegate;
    }

    @Override
    public BatchTranslator translator(BatchQuery query, DbAdapter adapter, String trimFunction) {
        final BatchTranslator delegateTranslator = delegate.translator(query, adapter, trimFunction);

        return new BatchTranslator() {

            @Override
            public String getTrimFunction() {
                return delegateTranslator.getTrimFunction();
            }

            @Override
            public String createSqlString() throws IOException {
                return delegateTranslator.createSqlString();
            }

            @Override
            public List<BatchParameterBinding> createBindings(BatchQueryRow row) {
                List<BatchParameterBinding> bindings = delegateTranslator.createBindings(row);

                for (BatchParameterBinding b : bindings) {
                    if (columnMapper.isEncrypted(b.getAttribute())) {
                        Object encrypted = cipherService.encrypt(b.getValue(), b.getAttribute().getType());
                        b.setValue(encrypted);
                    }
                }

                return bindings;
            }
        };
    }
}
