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
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.map_to_many.ClientIdMapToMany;
import org.apache.cayenne.testdo.map_to_many.ClientIdMapToManyTarget;
import org.apache.cayenne.testdo.map_to_many.IdMapToMany;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.MAP_TO_MANY_PROJECT)
public class CayenneContextMapRelationshipIT extends ClientCase {

    @Inject
    private CayenneContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMapToMany;
    private TableHelper tMapToManyTarget;

    @Before
    public void setUp() throws Exception {
        tMapToMany = new TableHelper(dbHelper, "ID_MAP_TO_MANY");
        tMapToMany.setColumns("ID");

        tMapToManyTarget = new TableHelper(dbHelper, "ID_MAP_TO_MANY_TARGET");
        tMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID");
    }

    private void createTwoMapToManysWithTargetsDataSet() throws Exception {
        tMapToMany.insert(1).insert(2);
        tMapToManyTarget.insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 2);
    }

    @Test
    public void testReadToMany() throws Exception {
        createTwoMapToManysWithTargetsDataSet();

        ObjectId id = ObjectId.of("IdMapToMany", IdMapToMany.ID_PK_COLUMN, 1);
        ClientIdMapToMany o1 = (ClientIdMapToMany) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map<Object, ClientIdMapToManyTarget> targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get(1));
        assertNotNull(targets.get(2));
        assertNotNull(targets.get(3));
    }

    @Test
    public void testAddToMany() throws Exception {
        createTwoMapToManysWithTargetsDataSet();

        ObjectId id = ObjectId.of("IdMapToMany", IdMapToMany.ID_PK_COLUMN, 1);
        ClientIdMapToMany o1 = (ClientIdMapToMany) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map<Object, ClientIdMapToManyTarget> targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        ClientIdMapToManyTarget newTarget = o1.getObjectContext().newObject(
                ClientIdMapToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertSame(o1, newTarget.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());

        int newId = Cayenne.intPKForObject(newTarget);
        assertSame(newTarget, o1.getTargets().get(newId));

        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, newTarget.getPersistenceState());
    }
}
