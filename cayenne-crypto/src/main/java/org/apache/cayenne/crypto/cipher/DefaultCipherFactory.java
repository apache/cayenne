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

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.apache.cayenne.di.Inject;

/**
 * Creates and returns a new {@link Cipher} configured using properties from
 * {@link CryptoConstants#PROPERTIES_MAP}.
 * 
 * @since 3.2
 */
public class DefaultCipherFactory implements CipherFactory {

    protected String transformation;
    protected int blockSize;

    public DefaultCipherFactory(@Inject(CryptoConstants.PROPERTIES_MAP) Map<String, String> properties) {
        String algorithm = properties.get(CryptoConstants.CIPHER_ALGORITHM);

        if (algorithm == null) {
            throw new CayenneCryptoException("Cipher algorithm is not set. Property name: "
                    + CryptoConstants.CIPHER_ALGORITHM);
        }

        String mode = properties.get(CryptoConstants.CIPHER_MODE);
        if (mode == null) {
            throw new CayenneCryptoException("Cipher mode is not set. Property name: " + CryptoConstants.CIPHER_MODE);
        }

        String padding = properties.get(CryptoConstants.CIPHER_PADDING);
        if (padding == null) {
            throw new CayenneCryptoException("Cipher padding is not set. Property name: "
                    + CryptoConstants.CIPHER_PADDING);
        }

        this.transformation = algorithm + "/" + mode + "/" + padding;
        this.blockSize = cipher().getBlockSize();
    }

    @Override
    public Cipher cipher() {
        try {
            return Cipher.getInstance(transformation);
        } catch (NoSuchAlgorithmException e) {
            throw new CayenneCryptoException("Error instantiating a cipher - no such algorithm: " + transformation, e);
        } catch (NoSuchPaddingException e) {
            throw new CayenneCryptoException("Error instantiating a cipher - no such padding: " + transformation, e);
        }
    }
    
    @Override
    public int blockSize() {
        return blockSize;
    }
}
