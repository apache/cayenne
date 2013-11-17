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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.CollectionToMany;
import org.apache.cayenne.testdo.relationship.CollectionToManyTarget;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CDOCollectionRelationshipTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("COLLECTION_TO_MANY_TARGET");
        dbHelper.deleteAll("COLLECTION_TO_MANY");

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

    public void testReadToManyPrefetching() throws Exception {

        SelectQuery query = new SelectQuery(CollectionToMany.class, ExpressionFactory
                .matchDbExp(CollectionToMany.ID_PK_COLUMN, new Integer(1)));
        query.addPrefetch(CollectionToMany.TARGETS_PROPERTY);
        CollectionToMany o1 = (CollectionToMany) Cayenne.objectForQuery(context, query);

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
