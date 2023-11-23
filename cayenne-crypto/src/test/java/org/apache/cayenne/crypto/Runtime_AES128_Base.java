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
import org.apache.cayenne.di.Module;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

import java.net.URL;
import java.sql.SQLException;

public class Runtime_AES128_Base {

    protected CayenneRuntime runtime;
    protected TableHelper table1;
    protected TableHelper table2;
    protected TableHelper table4;
    protected TableHelper table8;

    protected void setUp(boolean compress, boolean useHMAC) throws Exception {

        Module crypto = createCryptoModule(compress, useHMAC);
        this.runtime = createRuntime(crypto);

        setupTestTables(new DBHelper(runtime.getDataSource(null)));
    }

    protected void setupTestTables(DBHelper dbHelper) throws SQLException {

        this.table2 = new TableHelper(dbHelper, "TABLE2").setColumns("ID", "PLAIN_BYTES", "CRYPTO_BYTES");
        table2.deleteAll();

        this.table1 = new TableHelper(dbHelper, "TABLE1").setColumns("ID", "PLAIN_STRING", "CRYPTO_STRING",
                "PLAIN_INT", "CRYPTO_INT");
        table1.deleteAll();

        this.table4 = new TableHelper(dbHelper, "TABLE4").setColumns("ID", "PLAIN_STRING", "PLAIN_INT");
        table4.deleteAll();
        
        this.table8 = new TableHelper(dbHelper, "TABLE8").setColumns("ID", "CRYPTO_BYTES");
        table8.deleteAll();
    }

    protected CayenneRuntime createRuntime(Module crypto) {
        return CayenneRuntime.builder().addConfig("cayenne-crypto.xml").addModule(crypto).build();
    }

    protected Module createCryptoModule(boolean compress, boolean useHMAC) {
        URL keyStoreUrl = JceksKeySourceTest.class.getResource(JceksKeySourceTest.KS1_JCEKS);

        return b -> {
            CryptoModuleExtender moduleExtender = CryptoModule.extend(b)
                    .keyStore(keyStoreUrl, JceksKeySourceTest.TEST_KEY_PASS, "k3");
            if (compress) {
                moduleExtender.compress();
            }
            if(useHMAC) {
                moduleExtender.useHMAC();
            }
        };
    }

}
