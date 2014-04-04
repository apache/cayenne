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
package org.apache.cayenne.crypto.transformer.bytes;

import java.util.Map;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.apache.cayenne.crypto.cipher.CipherFactory;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.di.Inject;

/**
 * A {@link BytesTransformerFactory} that creates transformers depending on the
 * encryption mode specified via properties.
 * 
 * @since 3.2
 */
public class DefaultBytesTransformerFactory implements BytesTransformerFactory {

    private BytesTransformerFactory delegate;

    public DefaultBytesTransformerFactory(@Inject(CryptoConstants.PROPERTIES_MAP) Map<String, String> properties,
            @Inject CipherFactory cipherFactory, @Inject KeySource keySource) {

        String mode = properties.get(CryptoConstants.CIPHER_MODE);
        if (mode == null) {
            throw new CayenneCryptoException("Cipher mode is not set. Property name: " + CryptoConstants.CIPHER_MODE);
        }

        if ("CBC".equals(mode)) {
            this.delegate = new CbcBytesTransformerFactory(cipherFactory, keySource);
        }
        // TODO: ECB and other modes...
        else {
            throw new CayenneCryptoException("Unsupported mode: " + mode
                    + ". The following modes are currently supported:  CBC");
        }
    }

    public BytesEncryptor encryptor() {
        return delegate.encryptor();
    }

    public BytesDecryptor decryptor() {
        return delegate.decryptor();
    }
}
