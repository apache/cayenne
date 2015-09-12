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
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.crypto.db.Table2;
import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.apache.cayenne.query.SelectQuery;
import org.junit.Before;
import org.junit.Test;

public class Runtime_AES128_Test extends Runtime_AES128_Base {

    @Before
    public void setUp() throws Exception {
        super.setUp(false);
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
        t2.setCryptoBytes("crypto_2".getBytes());

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setCryptoBytes(null);

        context.commitChanges();

        List<Object[]> data = table2.selectAll();
        assertEquals(3, data.size());

        Map<String, byte[]> cipherByPlain = new HashMap<>();
        for (Object[] r : data) {
            cipherByPlain.put(new String((byte[]) r[1]), (byte[]) r[2]);
        }

        assertArrayEquals("crypto_1".getBytes(), CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("a"), runtime));
        assertArrayEquals("crypto_2".getBytes(), CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("b"), runtime));
        assertNull(cipherByPlain.get("c"));
    }

    @Test
    public void test_SelectQuery() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes("crypto_1".getBytes());

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setCryptoBytes("crypto_2".getBytes());

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setCryptoBytes(null);

        context.commitChanges();

        SelectQuery<Table2> select = SelectQuery.query(Table2.class);
        select.addOrdering(Table2.PLAIN_BYTES.asc());

        List<Table2> result = runtime.newContext().select(select);

        assertEquals(3, result.size());
        assertArrayEquals("crypto_1".getBytes(), result.get(0).getCryptoBytes());
        assertArrayEquals("crypto_2".getBytes(), result.get(1).getCryptoBytes());
        assertArrayEquals(null, result.get(2).getCryptoBytes());
    }

}
