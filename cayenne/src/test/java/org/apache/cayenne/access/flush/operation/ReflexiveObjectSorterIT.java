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
package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships.ReflexiveAndToOne;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Object-level dependency sorting of the objects of a reflexive entity, formerly a part of the entity sorter.
 */
public class ReflexiveObjectSorterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_PROJECT);

    private TableHelper tRelationshipHelper;
    private TableHelper tReflexiveAndToOne;

    @BeforeEach
    public void setUp() throws Exception {
        tRelationshipHelper = env.table("RELATIONSHIP_HELPER", "RELATIONSHIP_HELPER_ID", "NAME");
        tReflexiveAndToOne = env.table("REFLEXIVE_AND_TO_ONE")
                .setColumns("REFLEXIVE_AND_TO_ONE_ID", "PARENT_ID", "RELATIONSHIP_HELPER_ID", "NAME")
                .setColumnTypes(Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR);
    }

    private CompiledEntity entity() {
        return env.mapping().entity(ReflexiveAndToOne.class);
    }

    @Test
    public void reflexiveTable() {
        assertTrue(env.mapping().schema().table("REFLEXIVE_AND_TO_ONE").isReflexive());
        assertFalse(env.mapping().schema().table("RELATIONSHIP_HELPER").isReflexive());
    }

    @Test
    public void sortObjectsForEntityReflexiveWithFaults() throws Exception {
        tRelationshipHelper.insert(1, "rh1");
        tReflexiveAndToOne.insert(1, null, 1, "r1");
        tReflexiveAndToOne.insert(2, 1, 1, "r2");
        tReflexiveAndToOne.insert(3, 2, 1, "r3");

        List<ReflexiveAndToOne> objects = ObjectSelect.query(ReflexiveAndToOne.class).select(env.context());
        Collections.shuffle(objects);
        assertEquals(3, objects.size());

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, true);
        assertEquals("r3", objects.get(0).getName());
        assertEquals("r2", objects.get(1).getName());
        assertEquals("r1", objects.get(2).getName());

        tReflexiveAndToOne.delete().where("PARENT_ID", 2).execute();
        tReflexiveAndToOne.delete().where("PARENT_ID", 1).execute();
        tReflexiveAndToOne.deleteAll();
    }

    @Test
    public void newObjectsTreeInsertAndDeleteOrder() {
        // a random tree of new objects: every parent must be inserted before, and deleted after, its children
        ObjectContext context = env.context();
        Random random = new Random(3072);
        List<ReflexiveAndToOne> objects = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            ReflexiveAndToOne o = context.newObject(ReflexiveAndToOne.class);
            o.setName("n" + i);
            if (i > 0) {
                o.setParent(objects.get(random.nextInt(i)));
            }
            objects.add(o);
        }
        Collections.shuffle(objects, random);

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, false);
        assertEquals(200, objects.size());
        for (int i = 0; i < objects.size(); i++) {
            ReflexiveAndToOne parent = objects.get(i).getParent();
            assertTrue(parent == null || objects.indexOf(parent) < i, "insert: " + objects.get(i).getName()
                    + " before its parent");
        }

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, true);
        for (int i = 0; i < objects.size(); i++) {
            ReflexiveAndToOne parent = objects.get(i).getParent();
            assertTrue(parent == null || objects.indexOf(parent) > i, "delete: " + objects.get(i).getName()
                    + " after its parent");
        }
        context.rollbackChanges();
    }

    @Test
    public void independentObjectsKeepTheirOrder() {
        ObjectContext context = env.context();
        List<ReflexiveAndToOne> objects = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ReflexiveAndToOne o = context.newObject(ReflexiveAndToOne.class);
            o.setName("n" + i);
            objects.add(o);
        }
        List<ReflexiveAndToOne> original = new ArrayList<>(objects);

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, false);
        assertEquals(original, objects);

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, true);
        Collections.reverse(original);
        assertEquals(original, objects);
        context.rollbackChanges();
    }

    @Test
    public void cycleIsAnError() {
        ObjectContext context = env.context();
        ReflexiveAndToOne a = context.newObject(ReflexiveAndToOne.class);
        ReflexiveAndToOne b = context.newObject(ReflexiveAndToOne.class);
        ReflexiveAndToOne c = context.newObject(ReflexiveAndToOne.class);
        a.setParent(b);
        b.setParent(a);
        c.setParent(a);
        List<ReflexiveAndToOne> objects = new ArrayList<>(List.of(c, a, b));

        CayenneRuntimeException e = assertThrows(CayenneRuntimeException.class,
                () -> new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, false));
        assertTrue(e.getMessage().contains("Cycles found"), e.getMessage());
        context.rollbackChanges();
    }

    @Test
    public void masterOutsideTheSortedObjectsIsIgnored() {
        ObjectContext context = env.context();
        ReflexiveAndToOne outside = context.newObject(ReflexiveAndToOne.class);
        ReflexiveAndToOne child = context.newObject(ReflexiveAndToOne.class);
        ReflexiveAndToOne grandChild = context.newObject(ReflexiveAndToOne.class);
        child.setParent(outside);
        grandChild.setParent(child);
        List<ReflexiveAndToOne> objects = new ArrayList<>(List.of(grandChild, child));

        new ReflexiveObjectSorter().sortObjectsForEntity(entity(), objects, false);
        assertEquals(List.of(child, grandChild), objects);
        context.rollbackChanges();
    }
}
