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
 * the KeyStore and keys within it. Since Java only supports storing secret keys
 * in a "jceks" type of of KeyStore, this class assumes that provided keystore
 * is "jceks", and will throw if it is of a different type.
 * 
 * @since 3.2
 */
public class KeyStoreKeySource implements KeySource {

    // this is the only standard keystore type that supports storing secret keys
    private static final String JCEKS_KEYSTORE_TYPE = "jceks";

    private KeyStore keyStore;
    private char[] keyPassword;

    public KeyStoreKeySource(@Inject(CryptoConstants.PROPERTIES_MAP) Map<String, String> properties,
            @Inject(CryptoConstants.CREDENTIALS_MAP) Map<String, char[]> credentials) {

        String keyStoreUrl = properties.get(CryptoConstants.JCEKS_KEYSTORE_URL);
        if (keyStoreUrl == null) {
            throw new CayenneCryptoException("KeyStore URL is not set. Property name: " + CryptoConstants.JCEKS_KEYSTORE_URL);
        }

        this.keyPassword = credentials.get(CryptoConstants.KEY_PASSWORD);
        // NULL password is valid, though not secure .. so no NULL validation

        try {
            this.keyStore = createKeyStore(keyStoreUrl);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error loading keystore at " + keyStoreUrl, e);
        }
    }

    private KeyStore createKeyStore(String keyStoreUrl) throws KeyStoreException, IOException,
            NoSuchAlgorithmException, CertificateException {

        KeyStore keyStore = KeyStore.getInstance(JCEKS_KEYSTORE_TYPE);

        URL url = new URL(keyStoreUrl);
        InputStream in = url.openStream();

        try {
            keyStore.load(in, null);
        } finally {
            in.close();
        }

        return keyStore;
    }

    @Override
    public Key getKey(String alias) {
        try {
            return keyStore.getKey(alias, keyPassword);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error accessing key for alias: " + alias, e);
        }
    }
}
