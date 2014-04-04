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
package org.apache.cayenne.crypto;

import org.apache.cayenne.crypto.key.KeySource;

/**
 * @since 3.2
 */
public interface CryptoConstants {

    /**
     * An injection key for the Map<String, String> of the crypto properties.
     */
    public static final String PROPERTIES_MAP = "cayenne.crypto.properties";

    /**
     * An injection key for the Map<String, char[]> of credentials.
     */
    public static final String CREDENTIALS_MAP = "cayenne.crypto.properties";

    public static final String CIPHER_ALGORITHM = "cayenne.crypto.cipher.algorithm";

    public static final String CIPHER_MODE = "cayenne.crypto.cipher.mode";

    public static final String CIPHER_PADDING = "cayenne.crypto.cipher.padding";

    /**
     * Defines a URL of a KeyStore. The actual format depends on the
     * {@link KeySource} implementation that will be reading it. E.g. it can be
     * a "jceks" Java key store.
     */
    public static final String KEYSTORE_URL = "cayenne.crypto.keystore.url";

    /**
     * A password to access a secret key within the keystore.
     */
    public static final String KEY_PASSWORD = "cayenne.crypto.key.password";

    /**
     * A symbolic name of the default encryption key in the keystore.
     */
    public static final String DEFAULT_KEY_ALIAS = "cayenne.crypto.key.defaultalias";

}
