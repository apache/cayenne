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
import org.apache.cayenne.crypto.cipher.CryptoHandler;
import org.apache.cayenne.crypto.map.ColumnMapper;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.QueryMetadata;

public class CryptoRowReaderFactoryDecorator implements RowReaderFactory {

    private RowReaderFactory delegate;
    private ColumnMapper columnMapper;
    private CryptoHandler cryptoHandler;

    public CryptoRowReaderFactoryDecorator(@Inject RowReaderFactory delegate, @Inject CryptoHandler cryptoHandler,
            @Inject ColumnMapper columnMapper) {
        this.delegate = delegate;
        this.columnMapper = columnMapper;
        this.cryptoHandler = cryptoHandler;
    }

    @Override
    public RowReader<?> rowReader(RowDescriptor descriptor, QueryMetadata queryMetadata, DbAdapter adapter,
            Map<ObjAttribute, ColumnDescriptor> attributeOverrides) {

        List<ColumnDescriptor> encryptedColumns = null;

        for (ColumnDescriptor cd : descriptor.getColumns()) {

            DbAttribute attribute = cd.getAttribute();
            if (attribute != null && columnMapper.isEncrypted(attribute)) {
                if (encryptedColumns == null) {
                    encryptedColumns = new ArrayList<ColumnDescriptor>();
                }

                encryptedColumns.add(cd);
            }
        }

        final RowReader<?> delegateReader = delegate.rowReader(descriptor, queryMetadata, adapter, attributeOverrides);

        if (encryptedColumns == null) {
            return delegateReader;
        }

        final int len = encryptedColumns.size();
        final String[] labels = new String[len];
        final int[] types = new int[len];

        for (int i = 0; i < len; i++) {
            labels[i] = encryptedColumns.get(i).getDataRowKey();
            types[i] = encryptedColumns.get(i).getJdbcType();
        }

        return new RowReader<Object>() {
            private Boolean canDecrypt;

            @Override
            public Object readRow(ResultSet resultSet) {
                Object row = delegateReader.readRow(resultSet);

                if (canDecrypt == null) {
                    canDecrypt = row instanceof Map;
                }

                if (canDecrypt) {
                    @SuppressWarnings({ "unchecked", "rawtypes" })
                    Map<String, Object> map = (Map) row;

                    for (int i = 0; i < len; i++) {
                        Object encrypted = map.get(labels[i]);

                        if (encrypted != null) {
                            map.put(labels[i], cryptoHandler.decrypt(encrypted, types[i]));
                        }
                    }
                }

                return row;
            }
        };
    }

}
