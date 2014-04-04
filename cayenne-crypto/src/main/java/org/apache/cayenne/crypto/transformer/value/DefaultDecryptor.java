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
package org.apache.cayenne.crypto.transformer.value;

import java.security.Key;

import org.apache.cayenne.crypto.transformer.bytes.BytesDecryptor;

/**
 * @since 3.2
 */
class DefaultDecryptor implements ValueDecryptor {

    private Key defaultKey;
    private BytesConverter preConverter;
    private BytesConverter postConverter;

    public DefaultDecryptor(BytesConverter preConverter, BytesConverter postConverter, Key defaultKey) {
        this.preConverter = preConverter;
        this.postConverter = postConverter;
    }

    BytesConverter getPreConverter() {
        return preConverter;
    }

    BytesConverter getPostConverter() {
        return postConverter;
    }

    @Override
    public Object decrypt(BytesDecryptor bytesDecryptor, Object value) {

        byte[] bytes = preConverter.toBytes(value);

        // 'defaultKey' is likely to be ignored by the BytesDecryptor, as the
        // key name is obtained from the record itself
        byte[] transformed = bytesDecryptor.decrypt(bytes, 0, defaultKey);
        return postConverter.fromBytes(transformed);
    }

}
