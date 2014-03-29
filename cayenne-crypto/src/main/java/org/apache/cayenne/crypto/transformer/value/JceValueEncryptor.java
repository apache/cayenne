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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.apache.cayenne.crypto.CayenneCryptoException;

/**
 * @since 3.2
 */
public class JceValueEncryptor implements ValueTransformer {

    private ToBytesConverter preConverter;
    private FromBytesConverter postConverter;

    public JceValueEncryptor(ToBytesConverter preConverter, FromBytesConverter postConverter) {
        this.preConverter = preConverter;
        this.postConverter = postConverter;
    }

    ToBytesConverter getPreConverter() {
        return preConverter;
    }

    FromBytesConverter getPostConverter() {
        return postConverter;
    }

    @Override
    public Object transform(Cipher cipher, Object value) {

        byte[] bytes = preConverter.toBytes(value);
        byte[] transformed;

        try {
            transformed = cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            throw new CayenneCryptoException("Illegal block size", e);
        } catch (BadPaddingException e) {
            throw new CayenneCryptoException("Bad padding", e);
        }

        return postConverter.fromBytes(transformed);
    }

}
