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

import java.util.Set;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.SetToMany;
import org.apache.cayenne.testdo.relationship.SetToManyTarget;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CDOSetRelationshipTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tSetToMany;
    protected TableHelper tSetToManyTarget;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("SET_TO_MANY_TARGET");
        dbHelper.deleteAll("SET_TO_MANY");

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

    public void testReadToManyPrefetching() throws Exception {
        createTestDataSet();

        SelectQuery query = new SelectQuery(SetToMany.class, ExpressionFactory
                .matchDbExp(SetToMany.ID_PK_COLUMN, new Integer(1)));
        query.addPrefetch(SetToMany.TARGETS_PROPERTY);
        SetToMany o1 = (SetToMany) Cayenne.objectForQuery(context, query);

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
