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
package org.apache.cayenne.crypto.transformer.bytes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.crypto.CryptoConstants;
import org.apache.cayenne.crypto.key.KeySource;
import org.junit.Test;

public class DefaultBytesTransformerFactoryTest {

    @Test
    public void testCreateEncryptionHeader() {

        Map<String, String> properties = new HashMap<>();
        KeySource keySource = mock(KeySource.class);
        when(keySource.getDefaultKeyAlias()).thenReturn("bla");

        Header h1 = DefaultBytesTransformerFactory.createEncryptionHeader(properties, keySource);
        assertNotNull(h1);
        assertFalse(h1.isCompressed());
        assertEquals("bla", h1.getKeyName());

        properties.put(CryptoConstants.COMPRESSION, "false");
        Header h2 = DefaultBytesTransformerFactory.createEncryptionHeader(properties, keySource);
        assertFalse(h2.isCompressed());
        assertEquals("bla", h2.getKeyName());

        properties.put(CryptoConstants.COMPRESSION, "true");
        Header h3 = DefaultBytesTransformerFactory.createEncryptionHeader(properties, keySource);
        assertTrue(h3.isCompressed());
        assertEquals("bla", h3.getKeyName());
    }

}
