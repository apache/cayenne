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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.security.Key;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.junit.Test;

public class JceksKeySourceTest {

    public static final char[] TEST_KEY_PASS = "testkeypass".toCharArray();
    public static final String KS1_JCEKS = "ks1.jceks";

    @Test(expected = CayenneCryptoException.class)
    public void testConstructor_NoUrl() {
        Map<String, String> props = new HashMap<String, String>();
        Map<String, char[]> creds = new HashMap<String, char[]>();
        new JceksKeySource(props, creds);
    }

    @Test
    public void testGetKey_JCEKS_DES() {

        URL url = getClass().getResource(KS1_JCEKS);
        assertNotNull(url);

        Map<String, String> props = new HashMap<String, String>();
        props.put(CryptoConstants.KEYSTORE_URL, url.toExternalForm());
        props.put(CryptoConstants.ENCRYPTION_KEY_ALIAS, "k2");

        Map<String, char[]> creds = new HashMap<String, char[]>();
        creds.put(CryptoConstants.KEY_PASSWORD, TEST_KEY_PASS);

        JceksKeySource ks = new JceksKeySource(props, creds);

        assertNull(ks.getKey("no-such-key"));

        Key k1 = ks.getKey("k1");
        assertNotNull(k1);
        assertEquals("DES", k1.getAlgorithm());

        Key k2 = ks.getKey("k2");
        assertNotNull(k2);
        assertEquals("DES", k2.getAlgorithm());

        Key k3 = ks.getKey("k3");
        assertNotNull(k3);
        assertEquals("AES", k3.getAlgorithm());
    }

}
