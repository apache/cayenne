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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.apache.cayenne.crypto.CayenneCryptoException;
import org.apache.cayenne.crypto.CryptoConstants;
import org.junit.Test;

public class DefaultCipherFactoryTest {

    @Test
    public void testConstructor() {

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CryptoConstants.CIPHER_ALGORITHM, "AES");
        properties.put(CryptoConstants.CIPHER_MODE, "CBC");
        properties.put(CryptoConstants.CIPHER_PADDING, "PKCS5Padding");
        
        DefaultCipherFactory f = new DefaultCipherFactory(properties);
        assertEquals("AES/CBC/PKCS5Padding", f.transformation);
    }

    @Test(expected = CayenneCryptoException.class)
    public void testConstructor_Missing3Props() {
        Map<String, String> properties = new HashMap<String, String>();
        
        new DefaultCipherFactory(properties);
    }

    @Test(expected = CayenneCryptoException.class)
    public void testConstructor_Missing2Props() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CryptoConstants.CIPHER_ALGORITHM, "AES");
        
        new DefaultCipherFactory(properties);
    }

    @Test(expected = CayenneCryptoException.class)
    public void testConstructor_Missing1Props() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CryptoConstants.CIPHER_ALGORITHM, "AES");
        properties.put(CryptoConstants.CIPHER_MODE, "CBC");
        
        new DefaultCipherFactory(properties);
    }

    @Test
    public void testGetCipher() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CryptoConstants.CIPHER_ALGORITHM, "AES");
        properties.put(CryptoConstants.CIPHER_MODE, "CBC");
        properties.put(CryptoConstants.CIPHER_PADDING, "PKCS5Padding");

        DefaultCipherFactory f = new DefaultCipherFactory(properties);
        Cipher c = f.cipher();
        assertNotNull(c);
        assertEquals("AES/CBC/PKCS5Padding", c.getAlgorithm());
    }

}
