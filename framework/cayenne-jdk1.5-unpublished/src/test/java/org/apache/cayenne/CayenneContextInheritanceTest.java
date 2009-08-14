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
package org.apache.cayenne;

import java.util.List;

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable1Subclass;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CayenneContextInheritanceTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    private CayenneContext createClientContext() {
        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        LocalConnection connection = new LocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel clientChannel = new ClientChannel(connection);
        return new CayenneContext(clientChannel);
    }

    public void testInsertSubclass() {
        CayenneContext context = createClientContext();

        ClientMtTable1Subclass object = context.newObject(ClientMtTable1Subclass.class);
        object.setGlobalAttribute1("sub1");
        object.setServerAttribute1("sa1");
        object.setSubclassAttribute1("suba1");

        context.commitChanges();

        ObjectContext checkContext = createDataContext();
        SQLTemplate query = new SQLTemplate(MtTable1.class, "SELECT * FROM MT_TABLE1");
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setFetchingDataRows(true);

        List<DataRow> rows = checkContext.performQuery(query);
        assertEquals(1, rows.size());
        assertEquals("sub1", rows.get(0).get("GLOBAL_ATTRIBUTE1"));
        assertEquals("sa1", rows.get(0).get("SERVER_ATTRIBUTE1"));
        assertEquals("suba1", rows.get(0).get("SUBCLASS_ATTRIBUTE1"));
    }

    public void testPerformQueryInheritanceLeaf() {

        ObjectContext setupContext = createDataContext();
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (1, 'xxx', 'yyy')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1, SUBCLASS_ATTRIBUTE1) VALUES (2, 'sub1', 'zzz', 'sa1')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (3, '1111', 'aaa')"));

        CayenneContext context = createClientContext();

        SelectQuery query = new SelectQuery(ClientMtTable1Subclass.class);
        List<ClientMtTable1Subclass> objects = context.performQuery(query);

        assertEquals(1, objects.size());
        assertEquals("sa1", objects.get(0).getSubclassAttribute1());
    }

    public void testPerformQueryInheritanceSuper() {

        ObjectContext setupContext = createDataContext();
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (1, 'a', 'yyy')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1, SUBCLASS_ATTRIBUTE1) VALUES (2, 'sub1', 'zzz', 'sa1')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (3, 'z', 'aaa')"));

        CayenneContext context = createClientContext();

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        List<ClientMtTable1> objects = context.performQuery(query);

        assertEquals(3, objects.size());

        int checkedFields = 0;
        for (int i = 0; i < objects.size(); i++) {
            Integer id = (Integer) objects.get(i).getObjectId().getIdSnapshot().get(
                    "TABLE1_ID");
            if (id == 1) {
                assertEquals("a", objects.get(i).getGlobalAttribute1());
                checkedFields++;
            }
            else if (id == 2) {
                assertEquals("sa1", ((ClientMtTable1Subclass) objects.get(i))
                        .getSubclassAttribute1());
                checkedFields++;
            }

        }
        assertEquals(2, checkedFields);
    }

    public void testPerformQueryWithQualifierInheritanceSuper() {

        ObjectContext setupContext = createDataContext();
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (1, 'a', 'XX')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1, SUBCLASS_ATTRIBUTE1) VALUES (2, 'sub1', 'XXA', 'sa1')"));
        setupContext
                .performQuery(new SQLTemplate(
                        MtTable1.class,
                        "INSERT INTO MT_TABLE1 (TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) VALUES (3, 'z', 'MM')"));

        CayenneContext context = createClientContext();

        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.andQualifier(ExpressionFactory.likeExp(
                ClientMtTable1.SERVER_ATTRIBUTE1_PROPERTY,
                "X%"));
        List<ClientMtTable1> objects = context.performQuery(query);

        assertEquals(2, objects.size());
        
        int checkedFields = 0;
        for (int i = 0; i < objects.size(); i++) {
            Integer id = (Integer) objects.get(i).getObjectId().getIdSnapshot().get(
                    "TABLE1_ID");
            if (id == 1) {
                assertEquals("a", objects.get(i).getGlobalAttribute1());
                checkedFields++;
            }
            else if (id == 2) {
                assertEquals("sa1", ((ClientMtTable1Subclass) objects.get(i))
                        .getSubclassAttribute1());
                checkedFields++;
            }

        }
        assertEquals(2, checkedFields);
    }

}
