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
package org.apache.cayenne.crypto.transformer;

import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;
import org.apache.cayenne.crypto.transformer.value.ValueDecryptor;

import java.util.HashMap;
import java.util.Map;

/**
 * Decrypts the encrypted columns of a row. The columns are read through a {@link DataRowLayout.Reader}, which
 * resolves their positions once per result set.
 *
 * @since 4.0
 */
public class DefaultMapTransformer implements MapTransformer {

    private final String[] mapKeys;
    private final ValueDecryptor[] transformers;
    private final BytesDecryptor bytesDecryptor;
    private final DataRowLayout.Reader reader;

    public DefaultMapTransformer(String[] mapKeys, ValueDecryptor[] transformers, BytesDecryptor bytesDecryptor) {
        this.mapKeys = mapKeys;
        this.transformers = transformers;
        this.bytesDecryptor = bytesDecryptor;
        this.reader = DataRowLayout.of(mapKeys).reader();
    }

    @Override
    public DataRow transform(DataRow row) {
        // a row with nothing to decrypt is returned as is
        Map<String, Object> decrypted = null;
        for (int i = 0; i < mapKeys.length; i++) {
            Object value = reader.get(row, i);
            if (value != null) {
                if (decrypted == null) {
                    decrypted = new HashMap<>();
                }
                decrypted.put(mapKeys[i], transformers[i].decrypt(bytesDecryptor, value));
            }
        }
        return decrypted != null ? row.merged(decrypted, row.getVersion()) : row;
    }
}
