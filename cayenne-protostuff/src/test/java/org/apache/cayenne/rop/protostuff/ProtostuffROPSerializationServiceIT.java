/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/
package org.apache.cayenne.rop.protostuff;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable1;
import org.apache.cayenne.rop.protostuff.persistent.ClientMtTable2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class ProtostuffROPSerializationServiceIT extends RuntimeBase {

    private ClientMtTable1 table1;
    private ClientMtTable2 table2;

    @Before
    public void setUp() throws Exception {
        context = clientRuntime.newContext();

        table1 = context.newObject(ClientMtTable1.class);
        table1.setGlobalAttribute("table1");
        table1.setDateAttribute(LocalDate.now());

        table2 = context.newObject(ClientMtTable2.class);
        table2.setGlobalAttribute("table2");

        table1.addToTable2Array(table2);
        table2.setTable1(table1);

        context.commitChanges();
    }

    @After
    public void setDown() throws Exception {
        context.deleteObjects(table2, table1);
        context.commitChanges();
    }

    @Test
    public void testSerializationWithPrefetch1() throws Exception {
        ClientMtTable1 table1 = ObjectSelect.query(ClientMtTable1.class)
                .prefetch(ClientMtTable1.TABLE2ARRAY.joint())
                .selectOne(context);

        ClientMtTable2 table2 = table1.getTable2Array().get(0);

        assertEquals(this.table1, table1);
        assertEquals(this.table2, table2);
    }

    @Test
    public void testSerializationWithPrefetch2() throws Exception {
        ClientMtTable2 table2 = ObjectSelect.query(ClientMtTable2.class)
                .prefetch(ClientMtTable2.TABLE1.joint())
                .selectOne(context);

        ClientMtTable1 table1 = table2.getTable1();

        assertEquals(this.table1, table1);
        assertEquals(this.table2, table2);
    }
}
