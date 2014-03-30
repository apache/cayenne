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
package org.apache.cayenne.crypto.transformer;

import java.util.Map;

import javax.crypto.Cipher;

import org.apache.cayenne.crypto.transformer.value.ValueTransformer;

/**
 * @since 3.2
 */
public class DefaultMapTransformer implements MapTransformer {

    private String[] mapKeys;
    private ValueTransformer[] transformers;
    private Cipher cipher;

    public DefaultMapTransformer(String[] mapKeys, ValueTransformer[] transformers, Cipher cipher) {
        this.mapKeys = mapKeys;
        this.transformers = transformers;
        this.cipher = cipher;
    }

    @Override
    public void transform(Map<String, Object> map) {

        int len = mapKeys.length;

        for (int i = 0; i < len; i++) {
            Object value = map.get(mapKeys[i]);

            if (value != null) {
                Object transformed = transformers[i].transform(cipher, value);
                map.put(mapKeys[i], transformed);
            }
        }
    }
}
