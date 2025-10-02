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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.crypto.db.Table1;
import org.apache.cayenne.crypto.db.Table2;
import org.apache.cayenne.crypto.db.Table7;
import org.apache.cayenne.crypto.transformer.value.IntegerConverter;
import org.apache.cayenne.crypto.unit.CryptoUnitUtils;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class Runtime_AES128_IT extends Runtime_AES128_Base {

    @Before
    public void setUp() throws Exception {
        super.setUp(false, false);
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
    public void testInsert_Numeric() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setPlainInt(59);
        t1.setCryptoInt(61);

        context.commitChanges();

        Object[] data = table1.select();
        assertEquals(59, data[3]);
        assertEquals(Integer.valueOf(61), IntegerConverter.INSTANCE.fromBytes(CryptoUnitUtils.decrypt_AES_CBC((byte[]) data[4], runtime)));
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
    public void test_SelectQuery() {

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

        ObjectSelect<Table2> select = ObjectSelect.query(Table2.class)
                .orderBy(Table2.PLAIN_BYTES.asc());

        List<Table2> result = runtime.newContext().select(select);

        assertEquals(3, result.size());
        assertArrayEquals("crypto_1".getBytes(), result.get(0).getCryptoBytes());
        assertArrayEquals("crypto_2".getBytes(), result.get(1).getCryptoBytes());
        assertArrayEquals(null, result.get(2).getCryptoBytes());
    }


    @Test
    public void test_SelectNumeric() {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setPlainInt(59);
        t1.setCryptoInt(61);

        context.commitChanges();

        List<Table1> result = ObjectSelect.query(Table1.class).select(runtime.newContext());

        assertEquals(1, result.size());
        assertEquals(59, result.get(0).getPlainInt());
        assertEquals(61, result.get(0).getCryptoInt());
    }

    @Test
    public void test_ColumnQueryObject() {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setCryptoString("test");
        context.commitChanges();

        List<Table1> result = ObjectSelect
                .columnQuery(Table1.class, PropertyFactory.createSelf(Table1.class))
                .select(context);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getCryptoInt());
        assertEquals("test", result.get(0).getCryptoString());
    }

    @Test
    public void test_ColumnQueryObjectWithPlainScalar() {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setPlainInt(2);
        t1.setCryptoString("test");
        context.commitChanges();

        List<Object[]> result = ObjectSelect
                .columnQuery(Table1.class,
                        PropertyFactory.createSelf(Table1.class),
                        Table1.PLAIN_INT)
                .select(context);

        assertEquals(1, result.size());
        assertEquals(1, ((Table1)result.get(0)[0]).getCryptoInt());
        assertEquals("test", ((Table1)result.get(0)[0]).getCryptoString());
        assertEquals(2, result.get(0)[1]);
    }

    @Test
    public void test_ColumnQueryObjectWithEncryptedScalar() {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setPlainInt(2);
        t1.setCryptoString("test");
        t1.setPlainString("Test");
        context.commitChanges();

        List<Object[]> result = ObjectSelect
                .columnQuery(Table1.class,
                        PropertyFactory.createSelf(Table1.class),
                        Table1.CRYPTO_INT)
                .select(context);

        assertEquals(1, result.size());
        assertEquals(1, ((Table1)result.get(0)[0]).getCryptoInt());
        assertEquals("test", ((Table1)result.get(0)[0]).getCryptoString());
        assertEquals(1, result.get(0)[1]);
    }

    @Test
    public void testColumnQueryWithRelationshipWithTheSameNames() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setPlainInt(3);
        t1.setCryptoString("test");
        t1.setPlainString("Test");

        Table7 t7 = context.newObject(Table7.class);
        t7.setCryptoInt(2);
        t7.setCryptoString("string");

        t1.addToTable7s(t7);

        context.commitChanges();

        List<Object[]> result = ObjectSelect
                .columnQuery(Table1.class,
                        Table1.CRYPTO_INT,
                        PropertyFactory.createSelf(Table1.class),
                        Table1.CRYPTO_INT,
                        Table1.TABLE7S.dot(Table7.CRYPTO_INT),
                        Table1.TABLE7S.dot(Table7.CRYPTO_STRING))
                .select(context);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0)[0]);
        assertEquals(1, ((Table1)result.get(0)[1]).getCryptoInt());
        assertEquals("test", ((Table1)result.get(0)[1]).getCryptoString());
        assertEquals(1, result.get(0)[2]);
        assertEquals(2, result.get(0)[3]);
        assertEquals("string", result.get(0)[4]);
    }

    @Test
    public void testSelectWith2Objects() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setPlainInt(3);
        t1.setCryptoString("test");
        t1.setPlainString("Test");

        Table7 t7 = context.newObject(Table7.class);
        t7.setCryptoInt(2);
        t7.setCryptoString("string");

        t1.addToTable7s(t7);

        context.commitChanges();

        List<Object[]> result = ObjectSelect
                .columnQuery(Table1.class,
                        PropertyFactory.createSelf(Table1.class),
                        Table1.TABLE7S.flat())
                .select(context);
        assertEquals(1, result.size());
        assertEquals("test", ((Table1)result.get(0)[0]).getCryptoString());
        assertTrue(result.get(0)[1] instanceof Table7);
        assertEquals(2, ((Table7)result.get(0)[1]).getCryptoInt());
    }

    @Test
    public void testObjectSelectWithPrefetch() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setPlainInt(3);
        t1.setCryptoString("test");
        t1.setPlainString("Test");

        Table7 t7 = context.newObject(Table7.class);
        t7.setCryptoInt(2);
        t7.setCryptoString("string");

        t1.addToTable7s(t7);

        context.commitChanges();

        List<Table1> table1s = ObjectSelect.query(Table1.class)
                .prefetch(Table1.TABLE7S.disjoint())
                .select(context);

        assertEquals(1, table1s.size());
        assertEquals("test", table1s.get(0).getCryptoString());
        assertEquals("string", table1s.get(0).getTable7s().get(0).getCryptoString());
    }

    @Test
    public void test_ColumnQuerySingleScalar() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setCryptoString("test");
        context.commitChanges();

        List<String> result = ObjectSelect
                .columnQuery(Table1.class, Table1.CRYPTO_STRING)
                .select(context);

        assertEquals(1, result.size());
        assertEquals("test", result.get(0));
    }

    @Test
    public void test_ColumnQuerySingleScalarNull() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setCryptoString(null);
        context.commitChanges();

        List<String> result = ObjectSelect
                .columnQuery(Table1.class, Table1.CRYPTO_STRING)
                .select(context);

        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    public void test_ColumnQueryMultipleScalars() {
        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setCryptoInt(1);
        t1.setCryptoString("test");
        t1.setPlainInt(2);
        context.commitChanges();

        List<Object[]> result = ObjectSelect
                .columnQuery(Table1.class, Table1.CRYPTO_STRING, Table1.CRYPTO_INT, Table1.PLAIN_INT)
                .select(context);

        assertEquals(1, result.size());
        assertEquals("test", result.get(0)[0]);
        assertEquals(1, result.get(0)[1]);
        assertEquals(2, result.get(0)[2]);
    }

}
