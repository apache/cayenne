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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtMapToMany;
import org.apache.cayenne.testdo.mt.ClientMtMapToManyTarget;
import org.apache.cayenne.testdo.mt.MtMapToMany;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CayenneContextMapRelationshipTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    private CayenneContext createClientContext() {
        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        LocalConnection connection = new LocalConnection(serverChannel);
        ClientChannel clientChannel = new ClientChannel(connection);
        return new CayenneContext(clientChannel);
    }

    @Override
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testReadToMany() throws Exception {
        createTestData("prepare");

        ObjectContext context = createClientContext();

        ObjectId id = new ObjectId("MtMapToMany", MtMapToMany.ID_PK_COLUMN, 1);
        ClientMtMapToMany o1 = (ClientMtMapToMany) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map targets = o1.getTargets();

        assertTrue(((ValueHolder) targets).isFault());

        assertNotNull(targets);
        assertEquals(3, targets.size());
        assertNotNull(targets.get(new Integer(1)));
        assertNotNull(targets.get(new Integer(2)));
        assertNotNull(targets.get(new Integer(3)));
    }

    public void testAddToMany() throws Exception {
        createTestData("prepare");

        ObjectContext context = createClientContext();

        ObjectId id = new ObjectId("MtMapToMany", MtMapToMany.ID_PK_COLUMN, 1);
        ClientMtMapToMany o1 = (ClientMtMapToMany) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(id));

        Map targets = o1.getTargets();
        assertNotNull(targets);
        assertEquals(3, targets.size());

        ClientMtMapToManyTarget newTarget = o1
                .getObjectContext()
                .newObject(ClientMtMapToManyTarget.class);

        o1.addToTargets(newTarget);
        assertEquals(4, targets.size());
        assertSame(o1, newTarget.getMapToMany());

        o1.getObjectContext().commitChanges();

        o1.getObjectContext().performGenericQuery(new RefreshQuery());
        assertEquals(4, o1.getTargets().size());
        
        int newId = DataObjectUtils.intPKForObject(newTarget);
        assertSame(newTarget, o1.getTargets().get(new Integer(newId)));
        
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, newTarget.getPersistenceState());
    }
}
