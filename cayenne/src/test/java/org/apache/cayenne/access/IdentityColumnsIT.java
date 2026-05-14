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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.generated.GeneratedColumnCompKey;
import org.apache.cayenne.testdo.generated.GeneratedColumnCompMaster;
import org.apache.cayenne.testdo.generated.GeneratedColumnDep;
import org.apache.cayenne.testdo.generated.GeneratedColumnTest2;
import org.apache.cayenne.testdo.generated.GeneratedColumnTestEntity;
import org.apache.cayenne.testdo.generated.GeneratedF1;
import org.apache.cayenne.testdo.generated.GeneratedF2;
import org.apache.cayenne.testdo.generated.GeneratedReflexive;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IdentityColumnsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.GENERATED_PROJECT);

    private DataNode node;
    private TableHelper joinTable;
    
    @BeforeEach
    public void setUp() throws Exception {
        node = env.dataNode();
        joinTable = env.table("GENERATED_JOIN");
    }

    /**
     * Tests a bug casued by the ID Java type mismatch vs the default JDBC type
     * of the ID column.
     */
    @Test
    public void cAY823() throws Exception {

        GeneratedColumnTestEntity idObject = env.context().newObject(GeneratedColumnTestEntity.class);

        String name = "n_" + System.currentTimeMillis();
        idObject.setName(name);

        idObject.getObjectContext().commitChanges();

        ObjectId id = idObject.getObjectId();
        env.context().invalidateObjects(idObject);
        
        List<?> results = ObjectSelect.query(GeneratedColumnTestEntity.class)
                .pageSize(10)
                .select(env.context());
        assertEquals(1, results.size());

        // per CAY-823 an attempt to resolve an object results in an exception
        assertEquals(id, ((Persistent) results.get(0)).getObjectId());
    }

    @Test
    public void newObject() throws Exception {

        GeneratedColumnTestEntity idObject = env.context().newObject(GeneratedColumnTestEntity.class);

        String name = "n_" + System.currentTimeMillis();
        idObject.setName(name);

        idObject.getObjectContext().commitChanges();

        // this will throw an exception if id wasn't generated one way or
        // another
        int id = Cayenne.intPKForObject(idObject);
        assertTrue(id >= 0);

        // make sure that id is the same as id in the DB
        env.context().invalidateObjects(idObject);
        GeneratedColumnTestEntity object = Cayenne.objectForPK(env.context(), GeneratedColumnTestEntity.class, id);
        assertNotNull(object);
        assertEquals(name, object.getName());
    }

    @Test
    public void generatedJoinInFlattenedRelationship() throws Exception {

        // before saving objects, let's manually access PKGenerator to get a
        // base PK value
        // for comparison
        DbEntity joinTableEntity = env.context().getEntityResolver().getDbEntity(joinTable.getTableName());
        DbAttribute pkAttribute = joinTableEntity.getAttribute("ID");
        Number pk = (Number) node.getAdapter().getPkGenerator().generatePk(node, pkAttribute);

        GeneratedF1 f1 = env.context().newObject(GeneratedF1.class);
        GeneratedF2 f2 = env.context().newObject(GeneratedF2.class);
        f1.addToF2(f2);

        env.context().commitChanges();

        int id = joinTable.getInt("ID");
        assertTrue(id > 0);

        // this is a leap of faith that autoincrement-based IDs will not match
        // PkGenertor provided ids... This sorta works though if pk generator
        // has a 200
        // base value
        if (node.getAdapter().supportsGeneratedKeys()) {
            assertFalse(id == pk.intValue() + 1, "Looks like auto-increment wasn't used for the join table. ID: " + id);
        } else {
            assertEquals(id, pk.intValue() + 1);
        }
    }

    /**
     * Tests CAY-422 bug.
     */
    @Test
    public void unrelatedUpdate() throws Exception {

        GeneratedColumnTestEntity m = env.context().newObject(GeneratedColumnTestEntity.class);

        m.setName("m");

        GeneratedColumnDep d = env.context().newObject(GeneratedColumnDep.class);
        d.setName("d");
        d.setToMaster(m);
        env.context().commitChanges();

        env.context().invalidateObjects(m, d);

        env.context().prepareForAccess(d, null, false);

        // this line caused CAY-422 error
        d.getToMaster();

        d.setName("new name");
        env.context().commitChanges();
    }

    /**
     * Tests that insert in two tables with identity pk does not generate a
     * conflict. See CAY-341 for the original bug.
     */
    @Test
    public void multipleNewObjectsSeparateTables() throws Exception {

        GeneratedColumnTestEntity idObject1 = env.context().newObject(GeneratedColumnTestEntity.class);
        idObject1.setName("o1");

        GeneratedColumnTest2 idObject2 = env.context().newObject(GeneratedColumnTest2.class);
        idObject2.setName("o2");

        env.context().commitChanges();
    }

    @Test
    public void multipleNewObjects() throws Exception {

        String[] names = new String[] { "n1_" + System.currentTimeMillis(), "n2_" + System.currentTimeMillis(),
                "n3_" + System.currentTimeMillis() };

        GeneratedColumnTestEntity[] idObjects = new GeneratedColumnTestEntity[] {
                env.context().newObject(GeneratedColumnTestEntity.class), env.context().newObject(GeneratedColumnTestEntity.class),
                env.context().newObject(GeneratedColumnTestEntity.class) };

        for (int i = 0; i < idObjects.length; i++) {
            idObjects[i].setName(names[i]);
        }

        env.context().commitChanges();

        int[] ids = new int[idObjects.length];
        for (int i = 0; i < idObjects.length; i++) {
            ids[i] = Cayenne.intPKForObject(idObjects[i]);
            assertTrue(ids[i] > 0);
        }

        env.context().invalidateObjects(idObjects);

        for (int i = 0; i < ids.length; i++) {
            GeneratedColumnTestEntity object = Cayenne.objectForPK(env.context(), GeneratedColumnTestEntity.class, ids[i]);
            assertNotNull(object);
            assertEquals(names[i], object.getName());
        }
    }

    @Test
    public void compoundPKWithGeneratedColumn() throws Exception {
        if (node.getAdapter().supportsGeneratedKeys()) {
            // only works for generated keys, as the entity tested has one
            // Cayenne
            // auto-pk and one generated key

            String masterName = "m_" + System.currentTimeMillis();
            String depName1 = "dep1_" + System.currentTimeMillis();
            String depName2 = "dep2_" + System.currentTimeMillis();

            GeneratedColumnCompMaster master = env.context().newObject(GeneratedColumnCompMaster.class);
            master.setName(masterName);

            GeneratedColumnCompKey dep1 = env.context().newObject(GeneratedColumnCompKey.class);
            dep1.setName(depName1);
            dep1.setToMaster(master);

            GeneratedColumnCompKey dep2 = env.context().newObject(GeneratedColumnCompKey.class);
            dep2.setName(depName2);
            dep2.setToMaster(master);

            env.context().commitChanges();

            int masterId = Cayenne.intPKForObject(master);

            ObjectId id2 = dep2.getObjectId();

            // check propagated id
            Number propagatedID2 = (Number) id2.getIdSnapshot().get(GeneratedColumnCompKey.PROPAGATED_PK_PK_COLUMN);
            assertNotNull(propagatedID2);
            assertEquals(masterId, propagatedID2.intValue());

            // check Cayenne-generated ID
            Number cayenneGeneratedID2 = (Number) id2.getIdSnapshot().get(GeneratedColumnCompKey.AUTO_PK_PK_COLUMN);
            assertNotNull(cayenneGeneratedID2);

            // check DB-generated ID
            Number dbGeneratedID2 = (Number) id2.getIdSnapshot().get(GeneratedColumnCompKey.GENERATED_COLUMN_PK_COLUMN);
            assertNotNull(dbGeneratedID2);

            env.context().invalidateObjects(master, dep1, dep2);

            Object fetchedDep2 = Cayenne.objectForPK(env.context(), id2);
            assertNotNull(fetchedDep2);
        }
    }

    @Test
    public void updateDependentWithNewMaster() throws Exception {

        GeneratedColumnTestEntity master1 = env.context().newObject(GeneratedColumnTestEntity.class);
        master1.setName("aaa");

        GeneratedColumnDep dependent = env.context().newObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(master1);

        env.context().commitChanges();

        // change master
        GeneratedColumnTestEntity master2 = env.context().newObject(GeneratedColumnTestEntity.class);
        master2.setName("bbb");

        // TESTING THIS
        dependent.setToMaster(master2);
        env.context().commitChanges();

        int id1 = Cayenne.intPKForObject(master2);
        assertTrue(id1 >= 0);

        int id2 = Cayenne.intPKForObject(dependent);
        assertTrue(id2 >= 0);
        assertEquals(id1, id2);

        env.context().invalidateObjects(master2, dependent);

        assertNotNull(Cayenne.objectForPK(env.context(), GeneratedColumnTestEntity.class, id1));
        assertNotNull(Cayenne.objectForPK(env.context(), GeneratedColumnDep.class, id2));
    }

    @Test
    public void generatedDefaultValue() throws Exception {

        // fail("TODO: test insert with DEFAULT generated column...need custom
        // SQL to
        // build such table");
    }

    @Test
    public void propagateToDependent() throws Exception {

        GeneratedColumnTestEntity idObject = env.context().newObject(GeneratedColumnTestEntity.class);
        idObject.setName("aaa");

        GeneratedColumnDep dependent = idObject.getObjectContext().newObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(idObject);

        env.context().commitChanges();

        // this will throw an exception if id wasn't generated
        int id1 = Cayenne.intPKForObject(idObject);
        assertTrue(id1 >= 0);

        int id2 = Cayenne.intPKForObject(dependent);
        assertTrue(id2 >= 0);

        assertEquals(id1, id2);

        // refetch from DB
        env.context().invalidateObjects(idObject, dependent);

        assertNotNull(Cayenne.objectForPK(env.context(), GeneratedColumnTestEntity.class, id1));
        assertNotNull(Cayenne.objectForPK(env.context(), GeneratedColumnDep.class, id2));
    }

    @Test
    public void reflexiveDep() {

        GeneratedReflexive reflexive3 = env.context().newObject(GeneratedReflexive.class);
        reflexive3.setName("3");

        GeneratedReflexive reflexive2 = env.context().newObject(GeneratedReflexive.class);
        reflexive2.setName("2");

        GeneratedReflexive reflexive4 = env.context().newObject(GeneratedReflexive.class);
        reflexive4.setName("4");

        GeneratedReflexive reflexive1 = env.context().newObject(GeneratedReflexive.class);
        reflexive1.setName("1");

        reflexive1.setNext(reflexive2);
        reflexive2.setNext(reflexive3);
        reflexive3.setNext(reflexive4);

        env.context().commitChanges();

        reflexive1.setNext(null);
        reflexive2.setNext(null);
        reflexive3.setNext(null);

        env.context().commitChanges();
    }
}
