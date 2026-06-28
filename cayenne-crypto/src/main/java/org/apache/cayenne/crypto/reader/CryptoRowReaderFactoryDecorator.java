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

import org.apache.cayenne.access.jdbc.RSColumn;
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
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.ScalarResultSegment;

import java.sql.ResultSet;
import java.util.Map;

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
    public RowReader<?> rowReader(RSColumn[] columns, QueryMetadata metadata, DbAdapter adapter) {
        return super.rowReader(encryptedColumns(columns, adapter.getExtendedTypes()), metadata, adapter);
    }

    @Override
    protected RowReader<?> scalarSegmentReader(RSColumn[] columns, QueryMetadata metadata, ScalarResultSegment segment) {
        RowReader<?> scalarRowReader = super.scalarSegmentReader(columns, metadata, segment);
        return new DecoratedScalarRowReader(columns[segment.getColumnOffset()], scalarRowReader);
    }

    @Override
    protected RowReader<?> entitySegmentReader(RSColumn[] columns, QueryMetadata metadata, EntityResultSegment segment) {
        RowReader<?> entityRowReader = super.entitySegmentReader(columns, metadata, segment);
        return new DecoratedEntityRowReader(columns, entityRowReader, segment);
    }

    @Override
    protected RowReader<?> fullRowReader(RSColumn[] columns, QueryMetadata metadata) {
        RowReader<?> fullRowReader = super
                .fullRowReader(columns, metadata);
        return new DecoratedFullRowReader(columns, fullRowReader);
    }

    protected RSColumn[] encryptedColumns(RSColumn[] columns, ExtendedTypeMap typeMap) {

        // need to tweak the columns to ensure encrypted columns are read as binary or char, even if the plain Java
        // type is not a byte[] / String

        RSColumn[] encrypted = new RSColumn[columns.length];

        for (int i = 0; i < columns.length; i++) {
            RSColumn column = columns[i];
            DbAttribute attribute = column.attribute();

            ExtendedType type = column.reader();

            if (attribute != null && columnMapper.isEncrypted(attribute)) {

                // only char or binary columns can store encrypted data
                if (TypesMapping.isBinary(attribute.getType())) {
                    type = typeMap.getRegisteredType(byte[].class);
                } else if (TypesMapping.isCharacter(attribute.getType())) {
                    type = typeMap.getRegisteredType(String.class);
                }
                // else - warning?
            }

            encrypted[i] = type == column.reader()
                    ? column
                    : new RSColumn(column.rsName(), column.rsType(), column.dataRowName(), type, attribute);
        }

        return encrypted;
    }

    private class DecoratedScalarRowReader implements RowReader<Object> {
        private final RowReader<?> delegateReader;
        private final ValueDecryptor valueDecryptor;
        private final BytesDecryptor bytesDecryptor;

        DecoratedScalarRowReader(RSColumn descriptor, RowReader<?> delegateReader) {
            this.delegateReader = delegateReader;
            if(descriptor.attribute() != null && columnMapper.isEncrypted(descriptor.attribute())) {
                this.valueDecryptor = valueTransformerFactory.decryptor(descriptor.attribute());
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

        final RSColumn[] columns;
        final RowReader<?> delegateReader;
        final EntityResultSegment resultMetadata;
        boolean decryptorCompiled;
        MapTransformer decryptor;

        DecoratedEntityFullRowReader(RSColumn[] columns,
                                     RowReader<?> delegateReader,
                                     EntityResultSegment resultMetadata) {
            this.columns = columns;
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

        DecoratedEntityRowReader(RSColumn[] columns,
                                 RowReader<?> delegateReader,
                                 EntityResultSegment resultMetadata) {
            super(columns, delegateReader, resultMetadata);
        }

        void ensureDecryptorCompiled(Object row) {
            if (!decryptorCompiled) {
                int offset = resultMetadata.getColumnOffset();
                int fieldsSize = resultMetadata.getFields().size();
                RSColumn[] columns =
                        new RSColumn[fieldsSize];
                for(int i = offset, j = 0; i < offset + fieldsSize; i++) {
                    columns[j++] = this.columns[i];
                }
                decryptor = transformerFactory.decryptor(columns, row);
                decryptorCompiled = true;
            }
        }

        @Override
        public Object readRow(ResultSet resultSet) {
            return super.readRow(resultSet);
        }
    }

    private class DecoratedFullRowReader extends DecoratedEntityFullRowReader {

        DecoratedFullRowReader(RSColumn[] columns, RowReader<?> delegateReader) {
            super(columns, delegateReader, null);
        }

        void ensureDecryptorCompiled(Object row) {
            if (!decryptorCompiled) {
                decryptor = transformerFactory.decryptor(columns, row);
                decryptorCompiled = true;
            }
        }

        @Override
        public Object readRow(ResultSet resultSet) {
            return super.readRow(resultSet);
        }
    }
}
