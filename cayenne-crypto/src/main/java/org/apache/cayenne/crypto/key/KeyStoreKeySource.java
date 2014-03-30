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
package org.apache.cayenne.crypto.key;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.apache.cayenne.di.Inject;

/**
 * A {@link KeySource} based on a JDK KeyStore. DI properties are used to locate
 * the KeyStore and keys within it.
 * 
 * @since 3.2
 */
public class KeyStoreKeySource implements KeySource {

    private KeyStore keyStore;
    private char[] keyPasswordChars;

    public KeyStoreKeySource(@Inject(CryptoConstants.PROPERTIES_MAP) Map<String, String> properties)
            throws KeyStoreException {

        String keyStoreUrl = properties.get(CryptoConstants.KEYSTORE_URL);
        if (keyStoreUrl == null) {
            throw new CayenneCryptoException("KeyStore URL is not set. Property name: " + CryptoConstants.KEYSTORE_URL);
        }

        String keyStorePassword = properties.get(CryptoConstants.KEYSTORE_PASSWORD);
        // NULL password is valid, though not secure .. so no NULL validation

        String keyPassword = properties.get(CryptoConstants.KEY_PASSWORD);
        this.keyPasswordChars = keyPassword != null ? keyPassword.toCharArray() : null;
        // NULL password is valid, though not secure .. so no NULL validation

        try {
            this.keyStore = createKeyStore(keyStoreUrl, keyStorePassword);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error loading keystore at " + keyStoreUrl, e);
        }
    }

    private KeyStore createKeyStore(String keyStoreUrl, String keyStorePassword) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {
        char[] keyStorePasswordChars = keyStorePassword != null ? keyStorePassword.toCharArray() : null;

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        URL url = new URL(keyStoreUrl);
        InputStream in = url.openStream();

        try {
            keyStore.load(in, keyStorePasswordChars);
        } finally {
            in.close();
        }

        return keyStore;
    }

    @Override
    public Key getKey(String alias) {
        try {
            return keyStore.getKey(alias, keyPasswordChars);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error accessing key for alias: " + alias, e);
        }
    }
}
