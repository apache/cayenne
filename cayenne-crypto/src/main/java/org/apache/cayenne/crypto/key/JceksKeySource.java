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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.apache.cayenne.di.Inject;

/**
 * A {@link KeySource} based on a Java "jceks" KeyStore. Uses
 * {@link CryptoConstants#KEYSTORE_URL} to locate the keystore and
 * {@link CryptoConstants#KEY_PASSWORD} to read the secret key.
 * 
 * @since 3.2
 */
public class JceksKeySource implements KeySource {

    // this is the only standard keystore type that supports storing secret keys
    private static final String JCEKS_KEYSTORE_TYPE = "jceks";
    private static final Key NULL_KEY = new Key() {

        private static final long serialVersionUID = 4755682444381893880L;

        @Override
        public String getFormat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte[] getEncoded() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getAlgorithm() {
            throw new UnsupportedOperationException();
        }
    };

    private KeyStore keyStore;
    private char[] keyPassword;
    private String defaultKeyAlias;

    // caching the keys may not be a good idea for security reasons, but
    // re-reading the key from KeyStore for every select row creates a huge
    // bottleneck... And considering we are caching keystore password, it
    // probably doesn't make things that much worse
    private ConcurrentMap<String, Key> keyCache;

    public JceksKeySource(@Inject(CryptoConstants.PROPERTIES_MAP) Map<String, String> properties,
            @Inject(CryptoConstants.CREDENTIALS_MAP) Map<String, char[]> credentials) {

        String keyStoreUrl = properties.get(CryptoConstants.KEYSTORE_URL);
        if (keyStoreUrl == null) {
            throw new CayenneCryptoException("KeyStore URL is not set. Property name: " + CryptoConstants.KEYSTORE_URL);
        }

        this.keyPassword = credentials.get(CryptoConstants.KEY_PASSWORD);
        // NULL password is valid, though not secure .. so no NULL validation

        try {
            this.keyStore = createKeyStore(keyStoreUrl);
        } catch (Exception e) {
            throw new CayenneCryptoException("Error loading keystore at " + keyStoreUrl, e);
        }

        this.defaultKeyAlias = properties.get(CryptoConstants.ENCRYPTION_KEY_ALIAS);
        if (defaultKeyAlias == null) {
            throw new CayenneCryptoException("Default key alias is not set. Property name: "
                    + CryptoConstants.ENCRYPTION_KEY_ALIAS);
        }

        this.keyCache = new ConcurrentHashMap<String, Key>();
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

        Key key = keyCache.get(alias);
        if (key == null) {

            Key newKey = createKey(alias);
            Key oldKey = keyCache.putIfAbsent(alias, newKey);
            key = oldKey != null ? oldKey : newKey;
        }

        return key == NULL_KEY ? null : key;
    }

    protected Key createKey(String alias) {
        try {
            Key key = keyStore.getKey(alias, keyPassword);
            return key != null ? key : NULL_KEY;
        } catch (Exception e) {
            throw new CayenneCryptoException("Error accessing key for alias: " + alias, e);
        }
    }

    @Override
    public String getDefaultKeyAlias() {
        return defaultKeyAlias;
    }
}
