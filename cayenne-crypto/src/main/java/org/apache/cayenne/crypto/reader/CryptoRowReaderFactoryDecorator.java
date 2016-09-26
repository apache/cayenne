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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.jdbc.RowDescriptor;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.transformer.MapTransformer;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.QueryMetadata;

import java.sql.ResultSet;
import java.util.Map;

public class CryptoRowReaderFactoryDecorator implements RowReaderFactory {

    private RowReaderFactory delegate;
    private TransformerFactory transformerFactory;
    private ColumnMapper columnMapper;

    public CryptoRowReaderFactoryDecorator(@Inject RowReaderFactory delegate,
                                           @Inject TransformerFactory transformerFactory,
                                           @Inject ColumnMapper columnMapper) {
        this.delegate = delegate;
        this.transformerFactory = transformerFactory;
        this.columnMapper = columnMapper;
    }

    @Override
    public RowReader<?> rowReader(final RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
                                  Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

        final RowReader<?> delegateReader = delegate.rowReader(encryptedRowDescriptor(descriptor, adapter.getExtendedTypes()),
                queryMetadata,
                adapter,
                attributeOverrides);

        return new RowReader<Object>() {

            private boolean decryptorCompiled;
            private MapTransformer decryptor;

            private void ensureDecryptorCompiled(Object row) {
                if (!decryptorCompiled) {
                    decryptor = transformerFactory.decryptor(descriptor.getColumns(), row);
                    decryptorCompiled = true;
                }
            }

            @Override
            public Object readRow(ResultSet resultSet) {
                Object row = delegateReader.readRow(resultSet);

                ensureDecryptorCompiled(row);

                if (decryptor != null) {

                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Map<String, Object> map = (Map) row;

                    decryptor.transform(map);
                }

                return row;
            }
        };
    }

    protected RowDescriptor encryptedRowDescriptor(RowDescriptor descriptor, ExtendedTypeMap typeMap) {

        // need to tweak the original descriptor to ensure binary columns are read as binary, eben if the plain Java
        // type is not a byte[]

        ColumnDescriptor[] originalColumns = descriptor.getColumns();
        int len = originalColumns.length;

        ExtendedType[] originalConverters = descriptor.getConverters();
        ExtendedType[] encryptedConverters = new ExtendedType[len];

        for (int i = 0; i < len; i++) {
            DbAttribute attribute = originalColumns[i].getAttribute();

            ExtendedType t = originalConverters[i];

            if (attribute != null && columnMapper.isEncrypted(attribute)) {

                // only char or binary columns can store encrypted data
                if (TypesMapping.isBinary(attribute.getType())) {
                    t = typeMap.getRegisteredType(byte[].class);
                } else if (TypesMapping.isCharacter(attribute.getType())) {
                    t = typeMap.getRegisteredType(String.class);
                }
                // else - warning?
            }

            encryptedConverters[i] = t;
        }

        return new RowDescriptor(originalColumns, encryptedConverters);
    }
}
