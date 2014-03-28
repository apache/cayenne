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
import java.util.Map;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.crypto.cipher.CryptoFactory;
import org.apache.cayenne.crypto.cipher.MapTransformer;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.QueryMetadata;

public class CryptoRowReaderFactoryDecorator implements RowReaderFactory {

    private RowReaderFactory delegate;
    private CryptoFactory cryptoFactory;

    public CryptoRowReaderFactoryDecorator(@Inject RowReaderFactory delegate, @Inject CryptoFactory cryptoFactory) {
        this.delegate = delegate;
        this.cryptoFactory = cryptoFactory;
    }

    @Override
    public RowReader<?> rowReader(final RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
            Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

        final RowReader<?> delegateReader = delegate.rowReader(descriptor, queryMetadata, adapter, attributeOverrides);

        return new RowReader<Object>() {

            private boolean decryptorCompiled;
            private MapTransformer decryptor;

            private void ensureDecryptorCompiled(Object row) {
                if (!decryptorCompiled) {
                    decryptor = cryptoFactory.createDecryptor(descriptor.getColumns(), row);
                    decryptorCompiled = true;
                }
            }

            @Override
            public Object readRow(ResultSet resultSet) {
                Object row = delegateReader.readRow(resultSet);

                ensureDecryptorCompiled(row);

                if (decryptor != null) {

                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> map = (Map) row;

                    decryptor.transform(map);
                }

                return row;
            }
        };
    }
}
