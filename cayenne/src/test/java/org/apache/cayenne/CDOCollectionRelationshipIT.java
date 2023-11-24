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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToMany;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToManyTarget;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_COLLECTION_TO_MANY_PROJECT)
public class CDOCollectionRelationshipIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        TableHelper tCollectionToMany = new TableHelper(dbHelper, "COLLECTION_TO_MANY");
        tCollectionToMany.setColumns("ID");

        TableHelper tCollectionToManyTarget = new TableHelper(
                dbHelper,
                "COLLECTION_TO_MANY_TARGET");
        tCollectionToManyTarget.setColumns("ID", "COLLECTION_TO_MANY_ID");

        // single data set for all tests
        tCollectionToMany.insert(1).insert(2);
        tCollectionToManyTarget.insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 2);
    }

    @Test
    public void testReadToMany() throws Exception {

        CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);

        Collection<?> targets = o1.getTargets();

        assertNotNull(targets);
        assertTrue(((ValueHolder) targets).isFault());

        assertEquals(3, targets.size());

        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                1)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                2)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                3)));
    }

    @Test
    public void testReadToManyPrefetching() throws Exception {
        CollectionToMany o1 = ObjectSelect.query(CollectionToMany.class)
                .where(ExpressionFactory.matchDbExp(CollectionToMany.ID_PK_COLUMN, 1))
                .prefetch(CollectionToMany.TARGETS.disjoint())
                .selectOne(context);

        Collection<?> targets = o1.getTargets();

        assertFalse(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());

        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                1)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                2)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                3)));
    }

    @Test
    public void testAddToMany() throws Exception {

        CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);

        Collection<?> targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        CollectionToManyTarget newTarget = o1.getObjectContext().newObject(
                CollectionToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    @Test
    public void testRemoveToMany() throws Exception {

        CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);

        Collection<?> targets = o1.getTargets();
        assertEquals(3, targets.size());

        CollectionToManyTarget target = Cayenne.objectForPK(
                o1.getObjectContext(),
                CollectionToManyTarget.class,
                2);
        o1.removeFromTargets(target);

        assertEquals(2, targets.size());
        assertFalse(o1.getTargets().contains(target));
        assertNull(target.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(2, o1.getTargets().size());
        assertFalse(o1.getTargets().contains(target));
    }

    @Test
    public void testAddToManyViaReverse() throws Exception {

        CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);

        Collection<?> targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        CollectionToManyTarget newTarget = o1.getObjectContext().newObject(
                CollectionToManyTarget.class);

        newTarget.setCollectionToMany(o1);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getCollectionToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }
}
