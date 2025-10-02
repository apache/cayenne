/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.access.jdbc.reader.DefaultRowReaderFactory;
import org.apache.cayenne.access.jdbc.reader.RowReader;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.crypto.transformer.MapTransformer;
import org.apache.cayenne.crypto.transformer.TransformerFactory;
import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;
import org.apache.cayenne.crypto.transformer.bytes.BytesTransformerFactory;
import org.apache.cayenne.crypto.transformer.value.ValueDecryptor;
import org.apache.cayenne.crypto.transformer.value.ValueTransformerFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

public class CryptoRowReaderFactoryDecorator extends DefaultRowReaderFactory {

    private TransformerFactory transformerFactory;
    private ColumnMapper columnMapper;
    private BytesTransformerFactory bytesTransformerFactory;
    private ValueTransformerFactory valueTransformerFactory;

    public CryptoRowReaderFactoryDecorator(@Inject TransformerFactory transformerFactory,
                                           @Inject ColumnMapper columnMapper,
                                           @Inject BytesTransformerFactory bytesTransformerFactory,
                                           @Inject ValueTransformerFactory valueTransformerFactory) {
        this.transformerFactory = transformerFactory;
        this.columnMapper = columnMapper;
        this.bytesTransformerFactory = bytesTransformerFactory;
        this.valueTransformerFactory = valueTransformerFactory;
    }

    @Override
    public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
                                  Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {
        RowDescriptor encryptedRowDescriptor = encryptedRowDescriptor(descriptor, adapter.getExtendedTypes());
        return super.rowReader(encryptedRowDescriptor, queryMetadata, adapter, attributeOverrides);
    }

    @Override
    protected RowReader<?> createScalarRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
                                                 ScalarResultSegment segment) {
        RowReader<?> scalarRowReader = super
                .createScalarRowReader(descriptor, queryMetadata, segment);
        return new DecoratedScalarRowReader(descriptor.getColumns()[segment.getColumnOffset()], scalarRowReader);
    }

    @Override
    protected RowReader<?> createEntityRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
                                                 EntityResultSegment resultMetadata,
                                                 PostprocessorFactory postProcessorFactory) {
        RowReader<?> entityRowReader = super
                .createEntityRowReader(descriptor, queryMetadata, resultMetadata, postProcessorFactory);
        return new DecoratedEntityRowReader(descriptor, entityRowReader, resultMetadata);
    }

    @Override
    protected RowReader<?> createFullRowReader(RowDescriptor descriptor, QueryMetadata queryMetadata,
                                               PostprocessorFactory postProcessorFactory) {
        RowReader<?> fullRowReader = super
                .createFullRowReader(descriptor, queryMetadata, postProcessorFactory);
        return new DecoratedFullRowReader(descriptor, fullRowReader);
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

        return new DecoratedRowDescriptor(descriptor, originalColumns, encryptedConverters);
    }

    private static class DecoratedRowDescriptor extends RowDescriptor {

        private final RowDescriptor original;

        DecoratedRowDescriptor(RowDescriptor rowDescriptor, ColumnDescriptor[] columns, ExtendedType[] converters) {
            this.original = rowDescriptor;
            this.columns = columns;
            this.converters = converters;
        }

        public RowDescriptor unwrap() {
            return original;
        }
    }

    private class DecoratedScalarRowReader implements RowReader<Object> {
        private final RowReader<?> delegateReader;
        private final ValueDecryptor valueDecryptor;
        private final BytesDecryptor bytesDecryptor;

        DecoratedScalarRowReader(ColumnDescriptor descriptor, RowReader<?> delegateReader) {
            this.delegateReader = delegateReader;
            if(descriptor.getAttribute() != null && columnMapper.isEncrypted(descriptor.getAttribute())) {
                this.valueDecryptor = valueTransformerFactory.decryptor(descriptor.getAttribute());
                this.bytesDecryptor = bytesTransformerFactory.decryptor();
            } else {
                this.valueDecryptor = null;
                this.bytesDecryptor = null;
            }
        }

        @Override
        public Object readRow(ResultSet resultSet) {
            Object value = delegateReader.readRow(resultSet);
            if(valueDecryptor == null || value == null) {
                return value;
            }
            return valueDecryptor.decrypt(bytesDecryptor, value);
        }
    }

    private abstract class DecoratedEntityFullRowReader implements RowReader<Object> {

        final RowDescriptor descriptor;
        final RowReader<?> delegateReader;
        final EntityResultSegment resultMetadata;
        boolean decryptorCompiled;
        MapTransformer decryptor;

        DecoratedEntityFullRowReader(RowDescriptor descriptor,
                                 RowReader<?> delegateReader,
                                 EntityResultSegment resultMetadata) {
            this.descriptor = descriptor;
            this.delegateReader = delegateReader;
            this.resultMetadata = resultMetadata;
        }

        abstract void ensureDecryptorCompiled(Object row);

        @Override
        public Object readRow(ResultSet resultSet) {
            Object row = delegateReader.readRow(resultSet);

            ensureDecryptorCompiled(row);

            if (decryptor != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) row;
                decryptor.transform(map);
            }

            return row;
        }
    }

    private class DecoratedEntityRowReader extends DecoratedEntityFullRowReader {

        DecoratedEntityRowReader(RowDescriptor descriptor,
                                 RowReader<?> delegateReader,
                                 EntityResultSegment resultMetadata) {
            super(descriptor, delegateReader, resultMetadata);
        }

        void ensureDecryptorCompiled(Object row) {
            if (!decryptorCompiled) {
                int offset = resultMetadata.getColumnOffset();
                int fieldsSize = resultMetadata.getFields().size();
                ColumnDescriptor[] columnDescriptors =
                        new ColumnDescriptor[fieldsSize];
                for(int i = offset, j = 0; i < offset + fieldsSize; i++) {
                    columnDescriptors[j++] = descriptor.getColumns()[i];
                }
                decryptor = transformerFactory.decryptor(columnDescriptors, row);
                decryptorCompiled = true;
            }
        }

        @Override
        public Object readRow(ResultSet resultSet) {
            return super.readRow(resultSet);
        }
    }

    private class DecoratedFullRowReader extends DecoratedEntityFullRowReader {

        DecoratedFullRowReader(RowDescriptor descriptor,
                               RowReader<?> delegateReader) {
            super(descriptor, delegateReader, null);
        }

        void ensureDecryptorCompiled(Object row) {
            if (!decryptorCompiled) {
                decryptor = transformerFactory.decryptor(descriptor.getColumns(), row);
                decryptorCompiled = true;
            }
        }

        @Override
        public Object readRow(ResultSet resultSet) {
            return super.readRow(resultSet);
        }
    }
}
