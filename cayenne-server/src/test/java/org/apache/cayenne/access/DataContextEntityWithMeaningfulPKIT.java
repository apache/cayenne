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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKDep;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKTest1;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPkTest2;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@UseServerRuntime(CayenneProjects.MEANINGFUL_PK_PROJECT)
public class DataContextEntityWithMeaningfulPKIT extends ServerCase {

    @Inject
    private DataContext context;


    @Inject
    private ServerRuntime runtime;

    @Test
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

    @Test
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

    @Test
    public void testChangeKey() throws Exception {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        obj.setPkAttribute(new Integer(2000));
        context.commitChanges();

        // assert that object id got fixed
        ObjectId id = obj.getObjectId();
        assertEquals(new Integer(2000), id.getIdSnapshot().get("PK_ATTRIBUTE"));
    }

    @Test
    public void testToManyRelationshipWithMeaningfulPK1() throws Exception {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        // must be able to resolve to-many relationship
        ObjectContext context = runtime.newContext();
        List<MeaningfulPKTest1> objects = ObjectSelect.query(MeaningfulPKTest1.class).select(context);
        assertEquals(1, objects.size());
        obj = objects.get(0);
        assertEquals(0, obj.getMeaningfulPKDepArray().size());
    }

    @Test
    public void testToManyRelationshipWithMeaningfulPK2() throws Exception {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(new Integer(1000));
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep = context.newObject(MeaningfulPKDep.class);
        dep.setToMeaningfulPK(obj);
        context.commitChanges();
    }

    @Test
    public void testGeneratedPK(){
        MeaningfulPkTest2 obj = context.newObject(MeaningfulPkTest2.class);
        MeaningfulPkTest2 obj1 = context.newObject(MeaningfulPkTest2.class);
        context.commitChanges();

        ObjectContext context = runtime.newContext();
        List<MeaningfulPkTest2> objects = ObjectSelect.query(MeaningfulPkTest2.class).select(context);
        assertEquals(2, objects.size());
        assertNotEquals(objects.get(0).getPk1(), objects.get(1).getPk1());
    }
}
