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
package org.apache.cayenne.crypto.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.batch.BatchParameterBinding;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.transformer.value.ValueDecryptor;
import org.apache.cayenne.crypto.transformer.value.ValueEncryptor;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 3.2
 */
public class DefaultTransformerFactory implements TransformerFactory {

    private CipherFactory cipherFactory;
    private ColumnMapper columnMapper;
    private ValueTransformerFactory transformerFactory;

    public DefaultTransformerFactory(@Inject ColumnMapper columnMapper,
            @Inject ValueTransformerFactory transformerFactory, @Inject CipherFactory cipherFactory) {
        this.columnMapper = columnMapper;
        this.transformerFactory = transformerFactory;
        this.cipherFactory = cipherFactory;
    }

    @Override
    public MapTransformer decryptor(ColumnDescriptor[] columns, Object sampleRow) {

        if (!(sampleRow instanceof Map)) {
            return null;
        }

        int len = columns.length;
        List<Integer> cryptoColumns = null;

        for (int i = 0; i < len; i++) {

            DbAttribute a = columns[i].getAttribute();
            if (a != null && columnMapper.isEncrypted(a)) {
                if (cryptoColumns == null) {
                    cryptoColumns = new ArrayList<Integer>(len - i);
                }

                cryptoColumns.add(i);
            }
        }

        if (cryptoColumns != null) {

            int dlen = cryptoColumns.size();
            String[] mapKeys = new String[dlen];
            ValueDecryptor[] transformers = new ValueDecryptor[dlen];

            for (int i = 0; i < dlen; i++) {

                ColumnDescriptor cd = columns[cryptoColumns.get(i)];
                mapKeys[i] = cd.getDataRowKey();
                transformers[i] = transformerFactory.decryptor(cd.getAttribute());
            }

            return new DefaultMapTransformer(mapKeys, transformers, cipherFactory.cipher());
        }

        return null;
    }

    @Override
    public BindingsTransformer encryptor(BatchParameterBinding[] bindings) {
        int len = bindings.length;
        List<Integer> cryptoColumns = null;

        for (int i = 0; i < len; i++) {

            DbAttribute a = bindings[i].getAttribute();
            if (columnMapper.isEncrypted(a)) {

                if (cryptoColumns == null) {
                    cryptoColumns = new ArrayList<Integer>(len - i);
                }

                cryptoColumns.add(i);
            }
        }

        if (cryptoColumns != null) {

            int dlen = cryptoColumns.size();
            int[] positions = new int[dlen];
            ValueEncryptor[] transformers = new ValueEncryptor[dlen];

            for (int i = 0; i < dlen; i++) {
                int pos = cryptoColumns.get(i);
                BatchParameterBinding b = bindings[pos];
                positions[i] = pos;
                transformers[i] = transformerFactory.encryptor(b.getAttribute());
            }

            return new DefaultBindingsTransformer(positions, transformers, cipherFactory.cipher());
        }

        return null;
    }

}
