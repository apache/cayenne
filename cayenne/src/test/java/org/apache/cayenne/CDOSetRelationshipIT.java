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
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_set_to_many.SetToMany;
import org.apache.cayenne.testdo.relationships_set_to_many.SetToManyTarget;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_SET_TO_MANY_PROJECT)
public class CDOSetRelationshipIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tSetToMany;
    protected TableHelper tSetToManyTarget;

    @Before
    public void setUp() throws Exception {
        tSetToMany = new TableHelper(dbHelper, "SET_TO_MANY");
        tSetToMany.setColumns("ID");

        tSetToManyTarget = new TableHelper(dbHelper, "SET_TO_MANY_TARGET");
        tSetToManyTarget.setColumns("ID", "SET_TO_MANY_ID");
    }

    protected void createTestDataSet() throws Exception {
        tSetToMany.insert(1);
        tSetToMany.insert(2);
        tSetToManyTarget.insert(1, 1);
        tSetToManyTarget.insert(2, 1);
        tSetToManyTarget.insert(3, 1);
        tSetToManyTarget.insert(4, 2);
    }

    @Test
    public void testReadToMany() throws Exception {
        createTestDataSet();

        SetToMany o1 = Cayenne.objectForPK(context, SetToMany.class, 1);

        Set targets = o1.getTargets();

        assertNotNull(targets);
        assertTrue(((ValueHolder) targets).isFault());

        assertEquals(3, targets.size());

        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                1)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                2)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                3)));
    }

    @Test
    public void testReadToManyPrefetching() throws Exception {
        createTestDataSet();

        SetToMany o1 = SelectById.query(SetToMany.class, 1).prefetch(SetToMany.TARGETS.disjoint()).selectOne(context);

        Set targets = o1.getTargets();

        assertFalse(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());

        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                1)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                2)));
        assertTrue(targets.contains(Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                3)));
    }

    @Test
    public void testAddToMany() throws Exception {
        createTestDataSet();

        SetToMany o1 = Cayenne.objectForPK(context, SetToMany.class, 1);

        Set targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        SetToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(SetToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getSetToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

    @Test
    public void testRemoveToMany() throws Exception {
        createTestDataSet();

        SetToMany o1 = Cayenne.objectForPK(context, SetToMany.class, 1);

        Set targets = o1.getTargets();
        assertEquals(3, targets.size());

        SetToManyTarget target = Cayenne.objectForPK(
                o1.getObjectContext(),
                SetToManyTarget.class,
                2);
        o1.removeFromTargets(target);

        assertEquals(2, targets.size());
        assertFalse(o1.getTargets().contains(target));
        assertNull(target.getSetToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(2, o1.getTargets().size());
        assertFalse(o1.getTargets().contains(target));
    }

    @Test
    public void testAddToManyViaReverse() throws Exception {
        createTestDataSet();

        SetToMany o1 = Cayenne.objectForPK(context, SetToMany.class, 1);

        Set targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        SetToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(SetToManyTarget.class);

        newTarget.setSetToMany(o1);
        assertEquals(4, targets.size());
        assertTrue(o1.getTargets().contains(newTarget));
        assertSame(o1, newTarget.getSetToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
    }

}
