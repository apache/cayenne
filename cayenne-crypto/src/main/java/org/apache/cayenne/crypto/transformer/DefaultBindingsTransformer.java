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

import java.util.Arrays;

import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.DeferredValue;
import org.apache.cayenne.crypto.transformer.bytes.BytesEncryptor;
import org.apache.cayenne.crypto.transformer.value.ValueEncryptor;

/**
 * @since 4.0
 */
public class DefaultBindingsTransformer implements BindingsTransformer {

    private final int[] positions;
    private final ValueEncryptor[] transformers;
    private final BytesEncryptor encryptor;

    public DefaultBindingsTransformer(int[] positions, ValueEncryptor[] transformers, BytesEncryptor encryptor) {
        this.positions = positions;
        this.transformers = transformers;
        this.encryptor = encryptor;
    }

    @Override
    public PSBatchParameter[] transform(PSBatchParameter[] bindings) {
        PSBatchParameter[] result = Arrays.copyOf(bindings, bindings.length);

        for (int i = 0; i < positions.length; i++) {
            PSBatchParameter b = bindings[positions[i]];
            ValueEncryptor transformer = transformers[i];

            Object[] values = b.values();
            Object[] encrypted = new Object[values.length];
            for (int r = 0; r < values.length; r++) {
                Object value = values[r];
                if (value instanceof DeferredValue deferred) {
                    // a deferred value can only be encrypted after it is resolved at bind time
                    encrypted[r] = (DeferredValue) () -> transformer.encrypt(encryptor, deferred.get());
                } else {
                    encrypted[r] = transformer.encrypt(encryptor, value);
                }
            }

            result[positions[i]] = new PSBatchParameter(encrypted, b.psPosition(), b.psType(), b.psScale(), b.attribute());
        }

        return result;
    }
}
