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

import java.util.Map;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.relationship.IdMapToMany;
import org.apache.cayenne.testdo.relationship.MapToMany;
import org.apache.cayenne.testdo.relationship.MapToManyTarget;
import org.apache.cayenne.unit.RelationshipCase;

public class CDOMapRelationshipTest extends RelationshipCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testReadToMany() throws Exception {
        createTestData("prepare");

        MapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                MapToMany.class,
                1);

        Map targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get("A"));
        assertNotNull(targets.get("B"));
        assertNotNull(targets.get("C"));

        assertEquals(1, DataObjectUtils.intPKForObject((Persistent) targets.get("A")));
        assertEquals(2, DataObjectUtils.intPKForObject((Persistent) targets.get("B")));
        assertEquals(3, DataObjectUtils.intPKForObject((Persistent) targets.get("C")));
    }

    public void testReadToManyId() throws Exception {
        createTestData("prepare-id");

        IdMapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                IdMapToMany.class,
                1);

        Map targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get(new Integer(1)));
        assertNotNull(targets.get(new Integer(2)));
        assertNotNull(targets.get(new Integer(3)));

        assertEquals(1, DataObjectUtils.intPKForObject((Persistent) targets
                .get(new Integer(1))));
        assertEquals(2, DataObjectUtils.intPKForObject((Persistent) targets
                .get(new Integer(2))));
        assertEquals(3, DataObjectUtils.intPKForObject((Persistent) targets
                .get(new Integer(3))));
    }

    public void testReadToManyPrefetching() throws Exception {
        createTestData("prepare");

        SelectQuery query = new SelectQuery(MapToMany.class, ExpressionFactory
                .matchDbExp(MapToMany.ID_PK_COLUMN, new Integer(1)));
        query.addPrefetch(MapToMany.TARGETS_PROPERTY);
        MapToMany o1 = (MapToMany) DataObjectUtils.objectForQuery(
                createDataContext(),
                query);

        Map targets = o1.getTargets();

        assertFalse(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get("A"));
        assertNotNull(targets.get("B"));
        assertNotNull(targets.get("C"));
    }

    public void testAddToMany() throws Exception {
        createTestData("prepare");

        MapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                MapToMany.class,
                1);

        Map targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        MapToManyTarget newTarget = o1.getObjectContext().newObject(
                MapToManyTarget.class);

        newTarget.setName("X");
        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertSame(newTarget, o1.getTargets().get("X"));
        assertSame(o1, newTarget.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    public void testRemoveToMany() throws Exception {
        createTestData("prepare");

        MapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                MapToMany.class,
                1);

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

    public void testAddToManyViaReverse() throws Exception {
        createTestData("prepare");

        MapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                MapToMany.class,
                1);

        Map targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        MapToManyTarget newTarget = o1.getObjectContext().newObject(
                MapToManyTarget.class);

        newTarget.setName("X");
        newTarget.setMapToMany(o1);
        assertSame(o1, newTarget.getMapToMany());
        assertEquals(4, targets.size());
        assertSame(newTarget, o1.getTargets().get("X"));

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    public void testModifyToManyKey() throws Exception {
        createTestData("prepare");

        MapToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                MapToMany.class,
                1);

        Map targets = o1.getTargets();
        MapToManyTarget target = (MapToManyTarget) targets.get("B");
        target.setName("B1");

        o1.getObjectContext().commitChanges();

        assertNull(o1.getTargets().get("B"));
        assertSame(target, o1.getTargets().get("B1"));
    }
}
