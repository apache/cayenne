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
package org.apache.cayenne.lifecycle.relationship;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.lifecycle.db.E1;
import org.apache.cayenne.lifecycle.db.UuidRoot1;
import org.apache.cayenne.lifecycle.id.IdCoder;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;

public class ObjectIdRelationshipHandlerTest {

    private ServerRuntime runtime;

    private TableHelper rootTable;
    private TableHelper e1Table;

    @Before
    public void setUp() throws Exception {
        runtime = ServerRuntime.builder()
                // Using soft, as weak could lead to unloading data before test completes
                .addModule(b -> ServerModule.contributeProperties(b)
                        .put(Constants.SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY, "soft"))
                .addConfig("cayenne-lifecycle.xml").build();

        // a filter is required to invalidate root objects after commit
        ObjectIdRelationshipFilter filter = new ObjectIdRelationshipFilter();
        runtime.getDataDomain().addQueryFilter(filter);
        runtime.getDataDomain().getEntityResolver().getCallbackRegistry().addListener(filter);

        DBHelper dbHelper = new DBHelper(runtime.getDataSource(null));

        rootTable = new TableHelper(dbHelper, "UUID_ROOT1").setColumns("ID", "UUID");
        rootTable.deleteAll();

        e1Table = new TableHelper(dbHelper, "E1").setColumns("ID");
        e1Table.deleteAll();
    }

    @After
    public void tearDown() {
        runtime.shutdown();
    }

    @Test
    public void testRelate_Existing() throws Exception {

        e1Table.insert(1);

        ObjectContext context = runtime.newContext();
        E1 e1 = (E1) Cayenne.objectForQuery(context, ObjectSelect.query(E1.class));

        UuidRoot1 r1 = context.newObject(UuidRoot1.class);

        IdCoder refHandler = new IdCoder(context.getEntityResolver());
        ObjectIdRelationshipHandler handler = new ObjectIdRelationshipHandler(refHandler);
        handler.relate(r1, e1);

        assertEquals("E1:1", r1.getUuid());
        assertSame(e1, r1.readPropertyDirectly("cay:related:uuid"));

        context.commitChanges();

        Object[] r1x = rootTable.select();
        assertEquals("E1:1", r1x[1]);
    }

    @Test
    public void testRelate_New() throws Exception {

        ObjectContext context = runtime.newContext();
        E1 e1 = context.newObject(E1.class);

        UuidRoot1 r1 = context.newObject(UuidRoot1.class);

        IdCoder refHandler = new IdCoder(context
                .getEntityResolver());
        ObjectIdRelationshipHandler handler = new ObjectIdRelationshipHandler(refHandler);
        handler.relate(r1, e1);

        assertSame(e1, r1.readPropertyDirectly("cay:related:uuid"));

        context.commitChanges();

        int id = Cayenne.intPKForObject(e1);

        Object[] r1x = rootTable.select();
        assertEquals("E1:" + id, r1x[1]);
        assertEquals("E1:" + id, r1.getUuid());
    }

    @Test
    public void testRelate_Change() throws Exception {

        e1Table.insert(1);
        rootTable.insert(1, "E1:1");

        ObjectContext context = runtime.newContext();

        UuidRoot1 r1 = Cayenne.objectForPK(context, UuidRoot1.class, 1);
        assertEquals("E1:1", r1.getUuid());

        E1 e1 = context.newObject(E1.class);

        IdCoder refHandler = new IdCoder(context
                .getEntityResolver());
        ObjectIdRelationshipHandler handler = new ObjectIdRelationshipHandler(refHandler);
        handler.relate(r1, e1);

        assertSame(e1, r1.readPropertyDirectly("cay:related:uuid"));

        context.commitChanges();

        int id = Cayenne.intPKForObject(e1);
        assertNotEquals(1, id);

        Object[] r1x = rootTable.select();
        assertEquals("E1:" + id, r1x[1]);
        assertEquals("E1:" + id, r1.getUuid());
        assertSame(e1, r1.readProperty("cay:related:uuid"));
    }
}
