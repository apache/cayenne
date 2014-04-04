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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.crypto.db.Table1;
import org.apache.cayenne.crypto.db.Table2;
import org.apache.cayenne.crypto.key.JceksKeySourceTest;
import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.Before;
import org.junit.Test;

public class Runtime_AES128_Test {

    private ServerRuntime runtime;

    private TableHelper table2;

    @Before
    public void setUp() throws Exception {

        URL keyStoreUrl = JceksKeySourceTest.class.getResource(JceksKeySourceTest.KS1_JCEKS);
        Module crypto = new CryptoModuleBuilder().keyStore(keyStoreUrl, JceksKeySourceTest.TEST_KEY_PASS, "k1").build();

        this.runtime = new ServerRuntime("cayenne-crypto.xml", crypto);

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        this.table2 = new TableHelper(dbHelper, "TABLE2").setColumns("ID", "PLAIN_BYTES", "CRYPTO_BYTES");
        table2.deleteAll();
    }

    @Test
    public void testInsert() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("plain_1".getBytes());
        t1.setCryptoBytes("crypto_1".getBytes());

        context.commitChanges();

        Object[] data = table2.select();
        assertArrayEquals("plain_1".getBytes(), (byte[]) data[1]);
        assertArrayEquals("crypto_1".getBytes(), CryptoUnitUtils.decrypt_AES_CBC((byte[]) data[2], runtime));
    }

    @Test
    public void testInsert_MultipleObjects() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes("crypto_1".getBytes());

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setPlainBytes("crypto_2".getBytes());

        context.commitChanges();

        List<Object[]> data = table2.selectAll();
        assertEquals(2, data.size());

        Map<String, byte[]> cipherByPlain = new HashMap<String, byte[]>();
        for (Object[] r : data) {
            cipherByPlain.put(new String((byte[]) r[1]), (byte[]) r[2]);
        }

        assertEquals("crypto_1", CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("a"), runtime));
        assertEquals("crypto_2", CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("b"), runtime));
    }

    @Test
    public void test_SelectQuery() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes("crypto_1".getBytes());

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setPlainBytes("crypto_2".getBytes());

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setPlainBytes("crypto_3".getBytes());

        context.commitChanges();

        SelectQuery<Table2> select = SelectQuery.query(Table2.class);
        select.addOrdering("db:" + Table1.ID_PK_COLUMN, SortOrder.ASCENDING);

        List<Table2> result = runtime.newContext().select(select);

        assertEquals(3, result.size());
        assertEquals("crypto_1".getBytes(), result.get(0).getCryptoBytes());
        assertEquals("crypto_2".getBytes(), result.get(1).getCryptoBytes());
        assertEquals("crypto_3".getBytes(), result.get(2).getCryptoBytes());
    }

}
