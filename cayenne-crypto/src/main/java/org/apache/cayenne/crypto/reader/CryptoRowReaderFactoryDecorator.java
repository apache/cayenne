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
package org.apache.cayenne.crypto.reader;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.crypto.cipher.CryptoFactory;
import org.apache.cayenne.crypto.cipher.Decryptor;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.QueryMetadata;

public class CryptoRowReaderFactoryDecorator implements RowReaderFactory {

    private static final MapEntryDecryptor[] EMPTY_DECRYPTORS = new MapEntryDecryptor[0];

    private RowReaderFactory delegate;
    private CryptoFactory cryptoFactory;
    private ColumnMapper columnMapper;

    public CryptoRowReaderFactoryDecorator(@Inject RowReaderFactory delegate, @Inject CryptoFactory cryptoFactory,
            @Inject ColumnMapper columnMapper) {
        this.delegate = delegate;
        this.cryptoFactory = cryptoFactory;
        this.columnMapper = columnMapper;
    }

    @Override
    public RowReader<?> rowReader(final RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
            Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

        final RowReader<?> delegateReader = delegate.rowReader(descriptor, queryMetadata, adapter, attributeOverrides);

        return new RowReader<Object>() {

            private int len;
            private MapEntryDecryptor[] decryptors;

            private void ensureDecryptorCompiled(Object row) {
                if (decryptors == null) {

                    List<MapEntryDecryptor> decList = null;

                    if (row instanceof Map) {

                        ColumnDescriptor[] columns = descriptor.getColumns();
                        int len = columns.length;

                        for (int i = 0; i < len; i++) {

                            DbAttribute a = columns[i].getAttribute();
                            if (a != null && columnMapper.isEncrypted(a)) {
                                if (decList == null) {
                                    decList = new ArrayList<MapEntryDecryptor>(len - i);
                                }

                                decList.add(new MapEntryDecryptor(columns[i].getDataRowKey(), cryptoFactory
                                        .getDecryptor(a)));
                            }
                        }

                    }

                    this.decryptors = decList == null ? EMPTY_DECRYPTORS : decList
                            .toArray(new MapEntryDecryptor[decList.size()]);
                    this.len = decryptors.length;
                }
            }

            @Override
            public Object readRow(ResultSet resultSet) {
                Object row = delegateReader.readRow(resultSet);

                ensureDecryptorCompiled(row);

                if (len > 0) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> map = (Map) row;

                    for (int i = 0; i < len; i++) {

                        MapEntryDecryptor decryptor = decryptors[i];
                        Object encrypted = map.get(decryptor.key);

                        if (encrypted != null) {
                            map.put(decryptor.key, decryptor.decryptor.decrypt(encrypted));
                        }
                    }
                }

                return row;
            }
        };
    }

    class MapEntryDecryptor {
        final String key;
        final Decryptor decryptor;

        MapEntryDecryptor(String key, Decryptor decryptor) {
            this.key = key;
            this.decryptor = decryptor;
        }
    }
}
