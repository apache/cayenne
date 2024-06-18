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

import java.util.Map;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.map_to_many.IdMapToMany;
import org.apache.cayenne.testdo.map_to_many.MapToMany;
import org.apache.cayenne.testdo.map_to_many.MapToManyTarget;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.MAP_TO_MANY_PROJECT)
public class CDOMapRelationshipIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tMapToMany;
    protected TableHelper tMapToManyTarget;
    protected TableHelper tIdMapToMany;
    protected TableHelper tIdMapToManyTarget;

    @Before
    public void setUp() throws Exception {
        tMapToMany = new TableHelper(dbHelper, "MAP_TO_MANY");
        tMapToMany.setColumns("ID");

        tMapToManyTarget = new TableHelper(dbHelper, "MAP_TO_MANY_TARGET");
        tMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID", "NAME");

        tIdMapToMany = new TableHelper(dbHelper, "ID_MAP_TO_MANY");
        tIdMapToMany.setColumns("ID");

        tIdMapToManyTarget = new TableHelper(dbHelper, "ID_MAP_TO_MANY_TARGET");
        tIdMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID");
    }

    protected void createTestDataSet() throws Exception {
        tMapToMany.insert(1);
        tMapToMany.insert(2);
        tMapToManyTarget.insert(1, 1, "A");
        tMapToManyTarget.insert(2, 1, "B");
        tMapToManyTarget.insert(3, 1, "C");
        tMapToManyTarget.insert(4, 2, "A");
    }

    protected void createTestIdDataSet() throws Exception {
        tIdMapToMany.insert(1);
        tIdMapToMany.insert(2);
        tIdMapToManyTarget.insert(1, 1);
        tIdMapToManyTarget.insert(2, 1);
        tIdMapToManyTarget.insert(3, 1);
        tIdMapToManyTarget.insert(4, 2);
    }

    @Test
    public void testReadToMany() throws Exception {
        createTestDataSet();

        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);

        Map targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get("A"));
        assertNotNull(targets.get("B"));
        assertNotNull(targets.get("C"));

        assertEquals(1, Cayenne.intPKForObject((Persistent) targets.get("A")));
        assertEquals(2, Cayenne.intPKForObject((Persistent) targets.get("B")));
        assertEquals(3, Cayenne.intPKForObject((Persistent) targets.get("C")));
    }

    @Test
    public void testReadToManyId() throws Exception {
        createTestIdDataSet();

        IdMapToMany o1 = Cayenne.objectForPK(context, IdMapToMany.class, 1);

        Map targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get(1));
        assertNotNull(targets.get(2));
        assertNotNull(targets.get(3));

        assertEquals(1, Cayenne.intPKForObject((Persistent) targets.get(1)));
        assertEquals(2, Cayenne.intPKForObject((Persistent) targets.get(2)));
        assertEquals(3, Cayenne.intPKForObject((Persistent) targets.get(3)));
    }

    @Test
    public void testReadToManyPrefetching() throws Exception {
        createTestDataSet();

        MapToMany o1 = SelectById.query(MapToMany.class, 1).prefetch(MapToMany.TARGETS.disjoint()).selectOne(context);
        Map targets = o1.getTargets();

        assertFalse(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get("A"));
        assertNotNull(targets.get("B"));
        assertNotNull(targets.get("C"));
    }

    @Test
    public void testAddToMany() throws Exception {
        createTestDataSet();

        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);

        Map targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        MapToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(MapToManyTarget.class);

        newTarget.setName("X");
        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertSame(newTarget, o1.getTargets().get("X"));
        assertSame(o1, newTarget.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    @Test
    public void testRemoveToMany() throws Exception {
        createTestDataSet();

        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);

        Map targets = o1.getTargets();
        assertEquals(3, targets.size());

        MapToManyTarget target = (MapToManyTarget) targets.get("B");
        o1.removeFromTargets(target);

        assertEquals(2, targets.size());
        assertNull(o1.getTargets().get("B"));
        assertNull(target.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(2, o1.getTargets().size());
        assertNotNull(o1.getTargets().get("A"));
        assertNotNull(o1.getTargets().get("C"));
    }

    @Test
    public void testAddToManyViaReverse() throws Exception {
        createTestDataSet();

        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);

        Map targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        MapToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(MapToManyTarget.class);

        newTarget.setName("X");
        newTarget.setMapToMany(o1);
        assertSame(o1, newTarget.getMapToMany());
        assertEquals(4, targets.size());
        assertSame(newTarget, o1.getTargets().get("X"));

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    @Test
    public void testModifyToManyKey() throws Exception {
        createTestDataSet();

        MapToMany o1 = Cayenne.objectForPK(context, MapToMany.class, 1);

        Map targets = o1.getTargets();
        MapToManyTarget target = (MapToManyTarget) targets.get("B");
        target.setName("B1");

        o1.getObjectContext().commitChanges();

        assertNull(o1.getTargets().get("B"));
        assertSame(target, o1.getTargets().get("B1"));
    }
}
