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

package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKDep;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPKTest1;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPk;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPkDep2;
import org.apache.cayenne.testdo.meaningful_pk.MeaningfulPkTest2;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.MEANINGFUL_PK_PROJECT)
public class DataContextEntityWithMeaningfulPKIT extends RuntimeCase {

    @Inject
    private DataContext context;


    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testInsertWithMeaningfulPK() {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa-aaa");
        context.commitChanges();
        ObjectId objId = ObjectId.of("MeaningfulPKTest1", MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN, 1000);
        ObjectIdQuery q = new ObjectIdQuery(objId, true, ObjectIdQuery.CACHE_REFRESH);
        @SuppressWarnings("unchecked")
        List<DataRow> result = (List<DataRow>)context.performQuery(q);
        assertEquals(1, result.size());
        assertEquals(1000, result.get(0).get(MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN));
    }

    @Test
    public void testGeneratedKey() {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        assertNotEquals(0, obj.getPkAttribute());
        assertSame(obj, Cayenne.objectForPK(context, MeaningfulPKTest1.class, obj.getPkAttribute()));

        int id = Cayenne.intPKForObject(obj);

        Map snapshot = context.getObjectStore().getDataRowCache().getCachedSnapshot(obj.getObjectId());
        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey(MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN));
        assertEquals(id, snapshot.get(MeaningfulPKTest1.PK_ATTRIBUTE_PK_COLUMN));
    }

    @Test
    public void testChangeKey() {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        obj.setPkAttribute(2000);
        context.commitChanges();

        // assert that object id got fixed
        ObjectId id = obj.getObjectId();
        assertEquals(2000, id.getIdSnapshot().get("PK_ATTRIBUTE"));
    }

    @Test
    public void testToManyRelationshipWithMeaningfulPK1() {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
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
    public void testToManyRelationshipWithMeaningfulPK2() {
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa-aaa");
        context.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep = context.newObject(MeaningfulPKDep.class);
        dep.setToMeaningfulPK(obj);
        context.commitChanges();
    }

    @Test
    public void testGeneratedIntegerPK(){
        MeaningfulPkTest2 obj1 = context.newObject(MeaningfulPkTest2.class);
        obj1.setIntegerAttribute(10);
        MeaningfulPkTest2 obj2 = context.newObject(MeaningfulPkTest2.class);
        obj2.setIntegerAttribute(20);
        context.commitChanges();

        ObjectContext context = runtime.newContext();
        List<MeaningfulPkTest2> objects = ObjectSelect.query(MeaningfulPkTest2.class).select(context);
        assertEquals(2, objects.size());
        assertNotEquals(Integer.valueOf(0), obj1.getPkAttribute());
        assertNotEquals(Integer.valueOf(0), obj2.getPkAttribute());
        assertNotEquals(obj1.getPkAttribute(), obj2.getPkAttribute());
    }

    @Test
    public void testMeaningfulIntegerPK(){
        MeaningfulPkTest2 obj1 = context.newObject(MeaningfulPkTest2.class);
        obj1.setIntegerAttribute(10);
        obj1.setPkAttribute(1);
        MeaningfulPkTest2 obj2 = context.newObject(MeaningfulPkTest2.class);
        obj2.setIntegerAttribute(20);
        obj2.setPkAttribute(2);
        context.commitChanges();

        ObjectContext context = runtime.newContext();
        List<MeaningfulPkTest2> objects = ObjectSelect.query(MeaningfulPkTest2.class).select(context);
        assertEquals(2, objects.size());
        assertEquals(Integer.valueOf(1), obj1.getPkAttribute());
        assertEquals(Integer.valueOf(2), obj2.getPkAttribute());
    }

    @Test
    public void testGeneratedIntPK(){
        MeaningfulPKTest1 obj1 = context.newObject(MeaningfulPKTest1.class);
        obj1.setIntAttribute(10);
        MeaningfulPKTest1 obj2 = context.newObject(MeaningfulPKTest1.class);
        obj2.setIntAttribute(20);
        context.commitChanges();

        ObjectContext context = runtime.newContext();
        List<MeaningfulPKTest1> objects = ObjectSelect.query(MeaningfulPKTest1.class).select(context);
        assertEquals(2, objects.size());
        assertNotEquals(0, obj1.getPkAttribute());
        assertNotEquals(0, obj2.getPkAttribute());
        assertNotEquals(obj1.getPkAttribute(), obj2.getPkAttribute());
    }

    @Test
    public void testMeaningfulIntPK(){
        MeaningfulPKTest1 obj1 = context.newObject(MeaningfulPKTest1.class);
        obj1.setIntAttribute(10);
        obj1.setPkAttribute(1);
        MeaningfulPKTest1 obj2 = context.newObject(MeaningfulPKTest1.class);
        obj2.setIntAttribute(20);
        obj2.setPkAttribute(2);
        context.commitChanges();

        ObjectContext context = runtime.newContext();
        List<MeaningfulPKTest1> objects = ObjectSelect.query(MeaningfulPKTest1.class).select(context);
        assertEquals(2, objects.size());
        assertEquals(1, obj1.getPkAttribute());
        assertEquals(2, obj2.getPkAttribute());
    }

    @Test
    @Ignore("Insert will fail")
    public void testInsertDelete() {
        MeaningfulPk pkObj = context.newObject(MeaningfulPk.class);
        pkObj.setPk("123");
        context.commitChanges();

        context.deleteObject(pkObj);
        MeaningfulPk pkObj2 = context.newObject(MeaningfulPk.class);
        pkObj2.setPk("123");
        context.commitChanges();
    }

    @Test
    @Ignore
    public void test_MeaningfulPkInsertDeleteCascade() {
        // setup
        MeaningfulPKTest1 obj = context.newObject(MeaningfulPKTest1.class);
        obj.setPkAttribute(1000);
        obj.setDescr("aaa");
        context.commitChanges();

        // must be able to set reverse relationship
        MeaningfulPKDep dep = context.newObject(MeaningfulPKDep.class);
        dep.setToMeaningfulPK(obj);
        dep.setPk(10);
        context.commitChanges();

        // test
        context.deleteObject(obj);

        MeaningfulPKTest1 obj2 = context.newObject(MeaningfulPKTest1.class);
        obj2.setPkAttribute(1000);
        obj2.setDescr("bbb");

        MeaningfulPKDep dep2 = context.newObject(MeaningfulPKDep.class);
        dep2.setToMeaningfulPK(obj2);
        dep2.setPk(10);
        context.commitChanges();
    }

    @Test
    public void testMeaningfulFKToOneInvalidate() {
        MeaningfulPk pk = context.newObject(MeaningfulPk.class);
        MeaningfulPkDep2 dep = context.newObject(MeaningfulPkDep2.class);
        dep.setMeaningfulPk(pk);
        dep.setDescr("test");

        ObjectContext childContext = runtime.newContext(context);

        MeaningfulPkDep2 depChild = childContext.localObject(dep);
        depChild.setDescr("test2");

        assertEquals("test2", depChild.getDescr());
        assertNotNull(depChild.getMeaningfulPk());
        assertNull(depChild.getMeaningfulPk().getPk());
    }
}
