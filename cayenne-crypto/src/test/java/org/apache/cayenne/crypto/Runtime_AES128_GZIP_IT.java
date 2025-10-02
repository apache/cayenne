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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.crypto.db.Table2;
import org.apache.cayenne.crypto.db.Table8;
import org.apache.cayenne.crypto.transformer.bytes.Header;
import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Runtime_AES128_GZIP_IT extends Runtime_AES128_Base {

    private static final int GZIP_THRESHOLD = 150;

    @Before
    public void setUp() throws Exception {
        super.setUp(true, false);
    }

    @Test
    public void testInsert() throws SQLException {

        ObjectContext context = runtime.newContext();

        // make sure compression is on...
        byte[] cryptoBytes = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 100);

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("plain_1".getBytes());
        t1.setCryptoBytes(cryptoBytes);

        context.commitChanges();

        Object[] data = table2.select();
        assertArrayEquals("plain_1".getBytes(), (byte[]) data[1]);

        Header h = Header.create((byte[]) data[2], 0);
        assertTrue(h.isCompressed());
        assertArrayEquals(cryptoBytes,
                CryptoUnitUtils.gunzip(CryptoUnitUtils.decrypt_AES_CBC((byte[]) data[2], runtime)));
    }

    @Test
    public void testInsert_Small() throws SQLException {

        ObjectContext context = runtime.newContext();

        // make sure compression is on...
        byte[] cryptoBytes = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD - 20);

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("plain_1".getBytes());
        t1.setCryptoBytes(cryptoBytes);

        context.commitChanges();

        Object[] data = table2.select();
        assertArrayEquals("plain_1".getBytes(), (byte[]) data[1]);

        Header h = Header.create((byte[]) data[2], 0);
        assertFalse(h.isCompressed());
        assertArrayEquals(cryptoBytes, CryptoUnitUtils.decrypt_AES_CBC((byte[]) data[2], runtime));
    }

    @Test
    public void testInsert_MultipleObjects() throws SQLException {

        ObjectContext context = runtime.newContext();

        // make sure compression is on...
        byte[] cryptoBytes1 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 101);
        byte[] cryptoBytes2 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 102);

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes(cryptoBytes1);

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setCryptoBytes(cryptoBytes2);

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

        assertArrayEquals(cryptoBytes1,
                CryptoUnitUtils.gunzip(CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("a"), runtime)));
        assertArrayEquals(cryptoBytes2,
                CryptoUnitUtils.gunzip(CryptoUnitUtils.decrypt_AES_CBC(cipherByPlain.get("b"), runtime)));
        assertNull(cipherByPlain.get("c"));
    }

    @Test
    public void test_SelectQuery() throws SQLException {

        // make sure compression is on...
        byte[] cryptoBytes1 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 101);
        byte[] cryptoBytes2 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 102);

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes(cryptoBytes1);

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setCryptoBytes(cryptoBytes2);

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setCryptoBytes(null);

        context.commitChanges();

        ObjectSelect<Table2> select = ObjectSelect.query(Table2.class)
                .orderBy(Table2.PLAIN_BYTES.asc());

        List<Table2> result = runtime.newContext().select(select);

        assertEquals(3, result.size());
        assertArrayEquals(cryptoBytes1, result.get(0).getCryptoBytes());
        assertArrayEquals(cryptoBytes2, result.get(1).getCryptoBytes());
        assertArrayEquals(null, result.get(2).getCryptoBytes());
    }
    
    @Test
    public void test_SelectQueryWithOptimisticLocking() throws SQLException {
        ObjectContext context = runtime.newContext();
        
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            builder.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }
        
        String str = builder.toString();
        
        Table8 t1 = context.newObject(Table8.class);
        t1.setCryptoString(str);
        context.commitChanges();

        ObjectSelect<Table8> select = ObjectSelect.query(Table8.class);
        List<Table8> result = context.select(select);
        assertEquals(str, result.get(0).getCryptoString());
        
        t1.setCryptoString(str + "A");
        context.commitChanges(); // the update should execute without any locking in the WHERE clause
        
        result = context.select(select);
        assertEquals(str+"A", result.get(0).getCryptoString());
    }

    @Test
    public void test_ScalarColumnQuery() throws SQLException {
        // make sure compression is on...
        byte[] cryptoBytes1 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 101);
        byte[] cryptoBytes2 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 102);

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes(cryptoBytes1);

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setCryptoBytes(cryptoBytes2);

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setCryptoBytes(null);

        context.commitChanges();

        List<byte[]> result = ObjectSelect.query(Table2.class)
                .column(Table2.CRYPTO_BYTES)
                .orderBy(Table2.PLAIN_BYTES.asc())
                .select(runtime.newContext());

        assertEquals(3, result.size());
        assertArrayEquals(cryptoBytes1, result.get(0));
        assertArrayEquals(cryptoBytes2, result.get(1));
        assertArrayEquals(null, result.get(2));
    }

    @Test
    public void test_MultipleColumnsQuery() throws SQLException {
        // make sure compression is on...
        byte[] cryptoBytes1 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 101);
        byte[] cryptoBytes2 = CryptoUnitUtils.bytesOfSize(GZIP_THRESHOLD + 102);

        ObjectContext context = runtime.newContext();

        Table2 t1 = context.newObject(Table2.class);
        t1.setPlainBytes("a".getBytes());
        t1.setCryptoBytes(cryptoBytes1);

        Table2 t2 = context.newObject(Table2.class);
        t2.setPlainBytes("b".getBytes());
        t2.setCryptoBytes(cryptoBytes2);

        Table2 t3 = context.newObject(Table2.class);
        t3.setPlainBytes("c".getBytes());
        t3.setCryptoBytes(null);

        context.commitChanges();

        List<Object[]> result = ObjectSelect.query(Table2.class)
                .columns(Table2.CRYPTO_BYTES, Table2.PLAIN_BYTES)
                .orderBy(Table2.PLAIN_BYTES.asc())
                .select(runtime.newContext());

        assertEquals(3, result.size());
        assertArrayEquals(cryptoBytes1, (byte[])result.get(0)[0]);
        assertArrayEquals("a".getBytes(), (byte[])result.get(0)[1]);
        assertArrayEquals(cryptoBytes2, (byte[])result.get(1)[0]);
        assertArrayEquals("b".getBytes(), (byte[])result.get(1)[1]);
        assertArrayEquals(null, (byte[])result.get(2)[0]);
    }

}
