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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtMapToMany;
import org.apache.cayenne.testdo.mt.ClientMtMapToManyTarget;
import org.apache.cayenne.testdo.mt.MtMapToMany;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextMapRelationshipTest extends ClientCase {

    @Inject
    private CayenneContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtMapToMany;
    private TableHelper tMtMapToManyTarget;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_MAP_TO_MANY_TARGET");
        dbHelper.deleteAll("MT_MAP_TO_MANY");

        tMtMapToMany = new TableHelper(dbHelper, "MT_MAP_TO_MANY");
        tMtMapToMany.setColumns("ID");

        tMtMapToManyTarget = new TableHelper(dbHelper, "MT_MAP_TO_MANY_TARGET");
        tMtMapToManyTarget.setColumns("ID", "MAP_TO_MANY_ID");
    }

    private void createTwoMapToManysWithTargetsDataSet() throws Exception {
        tMtMapToMany.insert(1).insert(2);
        tMtMapToManyTarget.insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 2);
    }

    public void testReadToMany() throws Exception {
        createTwoMapToManysWithTargetsDataSet();

        ObjectId id = new ObjectId("MtMapToMany", MtMapToMany.ID_PK_COLUMN, 1);
        ClientMtMapToMany o1 = (ClientMtMapToMany) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map<Object, ClientMtMapToManyTarget> targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get(new Integer(1)));
        assertNotNull(targets.get(new Integer(2)));
        assertNotNull(targets.get(new Integer(3)));
    }

    public void testAddToMany() throws Exception {
        createTwoMapToManysWithTargetsDataSet();

        ObjectId id = new ObjectId("MtMapToMany", MtMapToMany.ID_PK_COLUMN, 1);
        ClientMtMapToMany o1 = (ClientMtMapToMany) Cayenne.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map<Object, ClientMtMapToManyTarget> targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        ClientMtMapToManyTarget newTarget = o1.getObjectContext().newObject(
                ClientMtMapToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertSame(o1, newTarget.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());

        int newId = Cayenne.intPKForObject(newTarget);
        assertSame(newTarget, o1.getTargets().get(new Integer(newId)));

        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, newTarget.getPersistenceState());
    }
}
