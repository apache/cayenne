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

package org.apache.cayenne.access;

import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.MeaningfulPKDep;
import org.apache.cayenne.testdo.testmap.MeaningfulPKTest1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextEntityWithMeaningfulPKTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private ServerRuntime runtime;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MEANINGFUL_PK_DEP");
        dbHelper.deleteAll("MEANINGFUL_PK_TEST1");
    }

    public void testInsertWithMeaningfulPK() throws Exception {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa-aaa");
        context.commitChanges();
        ObjectIdQuery q = new ObjectIdQuery(new ObjectId(
                "MeaningfulPKTest1",
                MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN,
                1000), true, ObjectIdQuery.CACHE_REFRESH);
        assertEquals(1, context.performQuery(q).size());
    }

    public void testGeneratedKey() throws Exception {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        assertNotNull(obj.getPkAttribute());
        assertSame(obj, Cayenne.objectForPK(context, MeaningfulPKTest1.class, obj
                .getPkAttribute()));

        int id = Cayenne.intPKForObject(obj);

        Map snapshot = context.getObjectStore().getDataRowCache().getCachedSnapshot(
                obj.getObjectId());
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey(MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN));
        assertEquals(new Integer(id), snapshot
                .get(MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN));
    }

    public void testChangeKey() throws Exception {
        MeaningfulPKTest1 obj = (MeaningfulPKTest1) context
                .newObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        obj.setPkAttribute(new Integer(2000));
        context.commitChanges();

        // assert that object id got fixed
        ObjectId id = obj.getObjectId();
        assertEquals(new Integer(2000), id.getIdSnapshot().get("PK_ATTRIBUTE"));
    }

    public void testToManyRelationshipWithMeaningfulPK1() throws Exception {
        MeaningfulPKTest1 obj = (MeaningfulPKTest1) context
                .newObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        // must be able to resolve to-many relationship
        ObjectContext context = runtime.newContext();
        List objects = context.performQuery(new SelectQuery(MeaningfulPKTest1.class));
        assertEquals(1, objects.size());
        obj = (MeaningfulPKTest1) objects.get(0);
        assertEquals(0, obj.getMeaningfulPKDepArray().size());
    }

    public void testToManyRelationshipWithMeaningfulPK2() throws Exception {
        MeaningfulPKTest1 obj = (MeaningfulPKTest1) context
                .newObject("MeaningfulPKTest1");
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep = (MeaningfulPKDep) context.newObject("MeaningfulPKDep");
        dep.setToMeaningfulPK(obj);
        context.commitChanges();
    }

}
