/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
 * @since 4.0
 */
public interface CryptoConstants {

    /**
     * An injection key for the Map<String, String> of the crypto properties.
     */
    String PROPERTIES_MAP = "cayenne.crypto.properties";

    /**
     * An injection key for the Map<String, char[]> of credentials.
     */
    String CREDENTIALS_MAP = "cayenne.crypto.properties";

    String CIPHER_ALGORITHM = "cayenne.crypto.cipher.algorithm";

    String CIPHER_MODE = "cayenne.crypto.cipher.mode";

    String CIPHER_PADDING = "cayenne.crypto.cipher.padding";

    /**
     * Defines a URL of a KeyStore. The actual format depends on the
     * {@link KeySource} implementation that will be reading it. E.g. it can be
     * a "jceks" Java key store.
     */
    String KEYSTORE_URL = "cayenne.crypto.keystore.url";

    /**
     * A password to access all secret keys within the keystore.
     */
    String KEY_PASSWORD = "cayenne.crypto.key.password";

    /**
     * A symbolic name of the default encryption key in the keystore.
     */
    String ENCRYPTION_KEY_ALIAS = "cayenne.crypto.key.enc.alias";

    /**
     * A property that defines whether compression is enabled. Should be "true"
     * or "false". "False" is the default.
     */
    String COMPRESSION = "cayenne.crypto.compression";

    /**
     * A property that defines whether HMAC is enabled.
     * Should be "true" or "false". "False" is the default.
     */
    String USE_HMAC = "cayenne.crypto.use_hmac";

}
