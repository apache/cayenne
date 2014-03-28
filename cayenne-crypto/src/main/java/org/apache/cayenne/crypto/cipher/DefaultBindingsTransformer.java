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
package org.apache.cayenne.crypto.cipher;

import javax.crypto.Cipher;

import org.apache.cayenne.access.translator.batch.BatchParameterBinding;

/**
 * @since 3.2
 */
public class DefaultBindingsTransformer implements BindingsTransformer {

    private int[] positions;
    private ValueTransformer[] transformers;
    private Cipher cipher;

    public DefaultBindingsTransformer(int[] positions, ValueTransformer[] transformers, Cipher cipher) {
        this.positions = positions;
        this.transformers = transformers;
        this.cipher = cipher;
    }

    @Override
    public void transform(BatchParameterBinding[] bindings) {

        int len = positions.length;

        for (int i = 0; i < len; i++) {
            BatchParameterBinding b = bindings[positions[i]];
            Object transformed = transformers[i].transform(cipher, b.getValue());
            b.setValue(transformed);
        }
    }

}
