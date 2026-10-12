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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.RowLayout;
import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;
import org.apache.cayenne.crypto.transformer.value.ValueDecryptor;

/**
 * Decrypts the encrypted columns of a row. A row with a {@link RowLayout} is decrypted by position, with the column
 * positions resolved once per layout, i.e. once per result set, as the transformer is created per row reader. Not
 * thread-safe.
 *
 * @since 4.0
 */
public class DefaultMapTransformer implements MapTransformer {

    private final String[] mapKeys;
    private final ValueDecryptor[] transformers;
    private final BytesDecryptor bytesDecryptor;

    // the positions of mapKeys in the layout of the last row seen, -1 for a key the layout does not have
    private RowLayout layout;
    private int[] indexes;

    public DefaultMapTransformer(String[] mapKeys, ValueDecryptor[] transformers, BytesDecryptor bytesDecryptor) {
        this.mapKeys = mapKeys;
        this.transformers = transformers;
        this.bytesDecryptor = bytesDecryptor;
    }

    @Override
    public DataRow transform(DataRow row) {
        RowLayout rowLayout = row.getLayout();
        return rowLayout != null ? transformByPosition(row, rowLayout) : transformByName(row);
    }

    private DataRow transformByPosition(DataRow row, RowLayout rowLayout) {
        int[] indexes = indexes(rowLayout);

        // the values are copied on the first encrypted non-null value, so a row with nothing to decrypt is returned
        // as is
        Object[] values = null;
        for (int i = 0; i < indexes.length; i++) {
            int index = indexes[i];
            if (index < 0) {
                continue;
            }

            Object value = row.valueAt(index);
            if (value != null) {
                if (values == null) {
                    values = new Object[rowLayout.size()];
                    for (int j = 0; j < values.length; j++) {
                        values[j] = row.valueAt(j);
                    }
                }
                values[index] = transformers[i].decrypt(bytesDecryptor, value);
            }
        }

        return values != null ? DataRow.of(row.getEntityName(), rowLayout, values, row.getVersion()) : row;
    }

    private int[] indexes(RowLayout rowLayout) {
        if (rowLayout != layout) {
            int[] resolved = new int[mapKeys.length];
            for (int i = 0; i < mapKeys.length; i++) {
                resolved[i] = rowLayout.indexOf(mapKeys[i]);
            }
            indexes = resolved;
            layout = rowLayout;
        }
        return indexes;
    }

    private DataRow transformByName(DataRow row) {
        Map<String, Object> decrypted = null;
        for (int i = 0; i < mapKeys.length; i++) {
            Object value = row.get(mapKeys[i]);
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
