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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.art.GeneratedColumnCompKey;
import org.apache.art.GeneratedColumnCompMaster;
import org.apache.art.GeneratedColumnDep;
import org.apache.art.GeneratedColumnTest2;
import org.apache.art.GeneratedColumnTestEntity;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class IdentityColumnsTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    /**
     * Tests a bug casued by the ID Java type mismatch vs the default JDBC type of the ID
     * column.
     */
    public void testCAY823() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTestEntity idObject = context
                .newObject(GeneratedColumnTestEntity.class);

        String name = "n_" + System.currentTimeMillis();
        idObject.setName(name);

        idObject.getObjectContext().commitChanges();

        ObjectId id = idObject.getObjectId();
        context.invalidateObjects(Collections.singleton(idObject));

        SelectQuery q = new SelectQuery(GeneratedColumnTestEntity.class);
        q.setPageSize(10);
        List results = context.performQuery(q);
        assertEquals(1, results.size());

        // per CAY-823 an attempt to resolve an object results in an exception
        assertEquals(id, ((Persistent) results.get(0)).getObjectId());
    }

    public void testNewObject() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTestEntity idObject = context
                .newObject(GeneratedColumnTestEntity.class);

        String name = "n_" + System.currentTimeMillis();
        idObject.setName(name);

        idObject.getObjectContext().commitChanges();

        // this will throw an exception if id wasn't generated one way or another
        int id = DataObjectUtils.intPKForObject(idObject);
        assertTrue(id >= 0);

        // make sure that id is the same as id in the DB
        context.invalidateObjects(Collections.singleton(idObject));
        GeneratedColumnTestEntity object = DataObjectUtils
                .objectForPK(context, GeneratedColumnTestEntity.class, id);
        assertNotNull(object);
        assertEquals(name, object.getName());
    }

    /**
     * Tests CAY-422 bug.
     */
    public void testUnrelatedUpdate() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTestEntity m = context
                .newObject(GeneratedColumnTestEntity.class);

        m.setName("m");

        GeneratedColumnDep d = context
                .newObject(GeneratedColumnDep.class);
        d.setName("d");
        d.setToMaster(m);
        context.commitChanges();

        context.invalidateObjects(Arrays.asList(m, d));

        context.prepareForAccess(d, null, false);

        // this line caused CAY-422 error
        d.getToMaster();

        d.setName("new name");
        context.commitChanges();
    }

    /**
     * Tests that insert in two tables with identity pk does not generate a conflict. See
     * CAY-341 for the original bug.
     */
    public void testMultipleNewObjectsSeparateTables() throws Exception {
        DataContext context = createDataContext();

        GeneratedColumnTestEntity idObject1 = context
                .newObject(GeneratedColumnTestEntity.class);
        idObject1.setName("o1");

        GeneratedColumnTest2 idObject2 = context
                .newObject(GeneratedColumnTest2.class);
        idObject2.setName("o2");

        context.commitChanges();
    }

    public void testMultipleNewObjects() throws Exception {
        DataContext context = createDataContext();

        String[] names = new String[] {
                "n1_" + System.currentTimeMillis(), "n2_" + System.currentTimeMillis(),
                "n3_" + System.currentTimeMillis()
        };

        GeneratedColumnTestEntity[] idObjects = new GeneratedColumnTestEntity[] {
                context
                        .newObject(GeneratedColumnTestEntity.class),
                context
                        .newObject(GeneratedColumnTestEntity.class),
                context
                        .newObject(GeneratedColumnTestEntity.class)
        };

        for (int i = 0; i < idObjects.length; i++) {
            idObjects[i].setName(names[i]);
        }

        context.commitChanges();

        int[] ids = new int[idObjects.length];
        for (int i = 0; i < idObjects.length; i++) {
            ids[i] = DataObjectUtils.intPKForObject(idObjects[i]);
            assertTrue(ids[i] > 0);
        }

        context.invalidateObjects(Arrays.asList(idObjects));

        for (int i = 0; i < ids.length; i++) {
            GeneratedColumnTestEntity object = DataObjectUtils
                    .objectForPK(context, GeneratedColumnTestEntity.class, ids[i]);
            assertNotNull(object);
            assertEquals(names[i], object.getName());
        }
    }

    public void testCompoundPKWithGeneratedColumn() throws Exception {
        if (getAccessStackAdapter().getAdapter().supportsGeneratedKeys()) {
            // only works for generated keys, as the entity tested has one Cayenne
            // auto-pk and one generated key

            String masterName = "m_" + System.currentTimeMillis();
            String depName1 = "dep1_" + System.currentTimeMillis();
            String depName2 = "dep2_" + System.currentTimeMillis();

            DataContext context = createDataContext();
            GeneratedColumnCompMaster master = context
                    .newObject(GeneratedColumnCompMaster.class);
            master.setName(masterName);

            GeneratedColumnCompKey dep1 = context
                    .newObject(GeneratedColumnCompKey.class);
            dep1.setName(depName1);
            dep1.setToMaster(master);

            GeneratedColumnCompKey dep2 = context
                    .newObject(GeneratedColumnCompKey.class);
            dep2.setName(depName2);
            dep2.setToMaster(master);

            context.commitChanges();

            int masterId = DataObjectUtils.intPKForObject(master);

            ObjectId id2 = dep2.getObjectId();

            // check propagated id
            Number propagatedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.PROPAGATED_PK_PK_COLUMN);
            assertNotNull(propagatedID2);
            assertEquals(masterId, propagatedID2.intValue());

            // check Cayenne-generated ID
            Number cayenneGeneratedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.AUTO_PK_PK_COLUMN);
            assertNotNull(cayenneGeneratedID2);

            // check DB-generated ID
            Number dbGeneratedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.GENERATED_COLUMN_PK_COLUMN);
            assertNotNull(dbGeneratedID2);

            context.invalidateObjects(Arrays.asList(master, dep1, dep2));

            Object fetchedDep2 = DataObjectUtils.objectForPK(context, id2);
            assertNotNull(fetchedDep2);
        }
    }

    public void testUpdateDependentWithNewMaster() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTestEntity master1 = context
                .newObject(GeneratedColumnTestEntity.class);
        master1.setName("aaa");

        GeneratedColumnDep dependent = context
                .newObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(master1);

        context.commitChanges();

        // change master
        GeneratedColumnTestEntity master2 = context
                .newObject(GeneratedColumnTestEntity.class);
        master2.setName("bbb");

        // TESTING THIS
        dependent.setToMaster(master2);
        context.commitChanges();

        int id1 = DataObjectUtils.intPKForObject(master2);
        assertTrue(id1 >= 0);

        int id2 = DataObjectUtils.intPKForObject(dependent);
        assertTrue(id2 >= 0);
        assertEquals(id1, id2);

        context.invalidateObjects(Arrays.asList(master2, dependent));

        assertNotNull(DataObjectUtils.objectForPK(
                context,
                GeneratedColumnTestEntity.class,
                id1));
        assertNotNull(DataObjectUtils.objectForPK(context, GeneratedColumnDep.class, id2));
    }

    public void testGeneratedDefaultValue() throws Exception {

        // fail("TODO: test insert with DEFAULT generated column...need custom SQL to
        // build such table");
    }

    public void testPropagateToDependent() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTestEntity idObject = context
                .newObject(GeneratedColumnTestEntity.class);
        idObject.setName("aaa");

        GeneratedColumnDep dependent = idObject
                .getObjectContext()
                .newObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(idObject);

        context.commitChanges();

        // this will throw an exception if id wasn't generated
        int id1 = DataObjectUtils.intPKForObject(idObject);
        assertTrue(id1 >= 0);

        int id2 = DataObjectUtils.intPKForObject(dependent);
        assertTrue(id2 >= 0);

        assertEquals(id1, id2);

        // refetch from DB
        context.invalidateObjects(Arrays.asList(idObject, dependent));

        assertNotNull(DataObjectUtils.objectForPK(
                context,
                GeneratedColumnTestEntity.class,
                id1));
        assertNotNull(DataObjectUtils.objectForPK(context, GeneratedColumnDep.class, id2));
    }
}
