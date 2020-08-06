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

import org.apache.cayenne.crypto.key.JceksKeySourceTest;
import org.apache.cayenne.crypto.key.KeySource;
import org.apache.cayenne.crypto.transformer.value.DefaultValueTransformerFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.junit.Test;

import java.net.URL;
import java.security.Key;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CryptoModuleBuilderTest {

    @Test
    public void testBuild_KeySource() {

        URL ksUrl = JceksKeySourceTest.class.getResource(JceksKeySourceTest.KS1_JCEKS);
        Module m = new CryptoModuleExtender().keyStore(ksUrl, JceksKeySourceTest.TEST_KEY_PASS, "k1")
                .valueTransformer(DefaultValueTransformerFactory.class).module();

        Injector injector = DIBootstrap.createInjector(new CryptoModule(), m);

        KeySource ks = injector.getInstance(KeySource.class);
        Key k1 = ks.getKey("k1");
        assertNotNull(k1);
        assertEquals("DES", k1.getAlgorithm());

        String dkName = ks.getDefaultKeyAlias();

        assertEquals("k1", dkName);
    }

}
