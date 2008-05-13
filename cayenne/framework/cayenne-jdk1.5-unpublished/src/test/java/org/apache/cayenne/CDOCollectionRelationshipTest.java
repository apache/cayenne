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

import java.util.Collection;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.relationship.CollectionToMany;
import org.apache.cayenne.testdo.relationship.CollectionToManyTarget;
import org.apache.cayenne.unit.RelationshipCase;

public class CDOCollectionRelationshipTest extends RelationshipCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testReadToMany() throws Exception {
        createTestData("prepare");

        CollectionToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                CollectionToMany.class,
                1);

        Collection targets = o1.getTargets();

        assertNotNull(targets);
        assertTrue(((ValueHolder) targets).isFault());

        assertEquals(3, targets.size());

        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                1)));
        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                2)));
        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                3)));
    }

    public void testReadToManyPrefetching() throws Exception {
        createTestData("prepare");

        SelectQuery query = new SelectQuery(CollectionToMany.class, ExpressionFactory
                .matchDbExp(CollectionToMany.ID_PK_COLUMN, new Integer(1)));
        query.addPrefetch(CollectionToMany.TARGETS_PROPERTY);
        CollectionToMany o1 = (CollectionToMany) DataObjectUtils.objectForQuery(
                createDataContext(),
                query);

        Collection targets = o1.getTargets();

        assertFalse(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());

        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                1)));
        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                2)));
        assertTrue(targets.contains(DataObjectUtils.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                3)));
    }

    public void testAddToMany() throws Exception {
        createTestData("prepare");

        CollectionToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                CollectionToMany.class,
                1);

        Collection targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        CollectionToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(CollectionToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    public void testRemoveToMany() throws Exception {
        createTestData("prepare");

        CollectionToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                CollectionToMany.class,
                1);

        Collection targets = o1.getTargets();
        assertEquals(3, targets.size());

        CollectionToManyTarget target = DataObjectUtils
                .objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 2);
        o1.removeFromTargets(target);

        assertEquals(2, targets.size());
        assertFalse(o1.getTargets().contains(target));
        assertNull(target.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(2, o1.getTargets().size());
        assertFalse(o1.getTargets().contains(target));
    }

    public void testAddToManyViaReverse() throws Exception {
        createTestData("prepare");

        CollectionToMany o1 = DataObjectUtils.objectForPK(
                createDataContext(),
                CollectionToMany.class,
                1);

        Collection targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        CollectionToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(CollectionToManyTarget.class);

        newTarget.setCollectionToMany(o1);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }
}
