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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.crypto.db.Table1;
import org.apache.cayenne.crypto.map.PatternColumnMapper;
import org.apache.cayenne.crypto.unit.Rot13TransformerFactory;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

public class Crypto_InRuntime_Test extends TestCase {

    private ServerRuntime runtime;

    private TableHelper table1;

    @Override
    protected void setUp() throws Exception {

        Module crypto = new CryptoModuleBuilder().valueTransformer(Rot13TransformerFactory.class)
                .columnMapper(new PatternColumnMapper("^CRYPTO_")).build();

        this.runtime = new ServerRuntime("cayenne-crypto.xml", crypto);

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        this.table1 = new TableHelper(dbHelper, "TABLE1").setColumns("ID", "PLAIN_STRING", "CRYPTO_STRING");
        table1.deleteAll();
    }

    public void testInsert() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setPlainString("plain_1");
        t1.setCryptoString("crypto_1");

        context.commitChanges();

        Object[] data = table1.select();
        assertEquals("plain_1", data[1]);
        assertEquals(Rot13TransformerFactory.rotate("crypto_1"), data[2]);
    }

    public void testInsert_MultipleObjects() throws SQLException {

        ObjectContext context = runtime.newContext();

        Table1 t1 = context.newObject(Table1.class);
        t1.setPlainString("a");
        t1.setCryptoString("crypto_1");

        Table1 t2 = context.newObject(Table1.class);
        t2.setPlainString("b");
        t2.setCryptoString("crypto_2");

        context.commitChanges();

        List<Object[]> data = table1.selectAll();
        assertEquals(2, data.size());

        Map<Object, Object> cipherByPlain = new HashMap<Object, Object>();
        for (Object[] r : data) {
            cipherByPlain.put(r[1], r[2]);
        }

        assertEquals(Rot13TransformerFactory.rotate("crypto_1"), cipherByPlain.get("a"));
        assertEquals(Rot13TransformerFactory.rotate("crypto_2"), cipherByPlain.get("b"));
    }

    public void test_SelectQuery() throws SQLException {

        table1.insert(1, "plain_1", Rot13TransformerFactory.rotate("crypto_1"));
        table1.insert(2, "plain_2", Rot13TransformerFactory.rotate("crypto_2"));
        table1.insert(3, "plain_3", Rot13TransformerFactory.rotate("crypto_3"));

        SelectQuery<Table1> select = SelectQuery.query(Table1.class);
        select.addOrdering(Table1.PLAIN_STRING.asc());

        List<Table1> result = runtime.newContext().select(select);

        assertEquals(3, result.size());
        assertEquals("crypto_1", result.get(0).getCryptoString());
        assertEquals("crypto_2", result.get(1).getCryptoString());
        assertEquals("crypto_3", result.get(2).getCryptoString());
    }

}
