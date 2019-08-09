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
package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable1Subclass1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class CayenneContextInheritanceIT extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    private TableHelper tMtTable1;

    @Before
    public void setUp() throws Exception {
        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns(
                "TABLE1_ID",
                "GLOBAL_ATTRIBUTE1",
                "SERVER_ATTRIBUTE1",
                "SUBCLASS_ATTRIBUTE1").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.VARCHAR,
                Types.VARCHAR);
    }

    @Test
    public void testInsertSubclass() throws Exception {

        ClientMtTable1Subclass1 object = context.newObject(ClientMtTable1Subclass1.class);
        object.setGlobalAttribute1("sub1");
        object.setServerAttribute1("sa1");
        object.setSubclass1Attribute1("suba1");

        context.commitChanges();

        assertEquals(1, tMtTable1.getRowCount());
        assertEquals("sub1", tMtTable1.getString("GLOBAL_ATTRIBUTE1"));
        assertEquals("sa1", tMtTable1.getString("SERVER_ATTRIBUTE1"));
        assertEquals("suba1", tMtTable1.getString("SUBCLASS_ATTRIBUTE1"));
    }

    @Test
    public void testPerformQueryInheritanceLeaf() throws Exception {

        tMtTable1.insert(1, "xxx", "yyy", null);
        tMtTable1.insert(2, "sub1", "zzz", "sa1");
        tMtTable1.insert(3, "1111", "aaa", null);

        List<ClientMtTable1Subclass1> objects = ObjectSelect.query(ClientMtTable1Subclass1.class).select(context);

        assertEquals(1, objects.size());
        assertEquals("sa1", objects.get(0).getSubclass1Attribute1());
    }

    @Test
    public void testPerformQueryInheritanceSuper() throws Exception {

        tMtTable1.insert(1, "a", "yyy", null);
        tMtTable1.insert(2, "sub1", "zzz", "sa1");
        tMtTable1.insert(3, "z", "aaa", null);

        List<ClientMtTable1> objects = ObjectSelect.query(ClientMtTable1.class).select(context);

        assertEquals(3, objects.size());

        int checkedFields = 0;
        for (ClientMtTable1 object : objects) {
            Integer id = (Integer) object.getObjectId().getIdSnapshot().get("TABLE1_ID");
            if (id == 1) {
                assertEquals("a", object.getGlobalAttribute1());
                checkedFields++;
            } else if (id == 2) {
                assertEquals("sa1", ((ClientMtTable1Subclass1) object).getSubclass1Attribute1());
                checkedFields++;
            }

        }
        assertEquals(2, checkedFields);
    }

    @Test
    public void testPerformQueryWithQualifierInheritanceSuper() throws Exception {

        tMtTable1.insert(1, "a", "XX", null);
        tMtTable1.insert(2, "sub1", "XXA", "sa1");
        tMtTable1.insert(3, "z", "MM", null);

        List<ClientMtTable1> objects = ObjectSelect.query(ClientMtTable1.class)
                .where(ClientMtTable1.SERVER_ATTRIBUTE1.like("X%"))
                .select(context);

        assertEquals(2, objects.size());

        int checkedFields = 0;
        for (ClientMtTable1 object : objects) {
            Integer id = (Integer) object.getObjectId().getIdSnapshot().get("TABLE1_ID");
            if (id == 1) {
                assertEquals("a", object.getGlobalAttribute1());
                checkedFields++;
            } else if (id == 2) {
                assertEquals("sa1", ((ClientMtTable1Subclass1) object).getSubclass1Attribute1());
                checkedFields++;
            }

        }
        assertEquals(2, checkedFields);
    }

}
