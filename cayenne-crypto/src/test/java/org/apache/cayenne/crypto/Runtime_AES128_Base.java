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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.crypto.key.JceksKeySourceTest;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

import java.net.URL;

public class Runtime_AES128_Base {

    protected ServerRuntime runtime;
    protected TableHelper table1;
    protected TableHelper table2;

    protected void setUp(boolean compress) throws Exception {

        URL keyStoreUrl = JceksKeySourceTest.class.getResource(JceksKeySourceTest.KS1_JCEKS);

        CryptoModuleBuilder builder = new CryptoModuleBuilder().keyStore(keyStoreUrl, JceksKeySourceTest.TEST_KEY_PASS,
                "k3");

        if (compress) {
            builder.compress();
        }

        Module crypto = builder.build();

        this.runtime = new ServerRuntime("cayenne-crypto.xml", crypto);

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        this.table2 = new TableHelper(dbHelper, "TABLE2").setColumns("ID", "PLAIN_BYTES", "CRYPTO_BYTES");
        table2.deleteAll();

        this.table1 = new TableHelper(dbHelper, "TABLE1").setColumns("ID", "PLAIN_STRING", "CRYPTO_STRING",
                "PLAIN_INT", "CRYPTO_INT");
        table1.deleteAll();
    }

}
