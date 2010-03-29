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

import org.apache.art.Artist;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CayenneContextRefreshQueryTest extends CayenneCase {

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
    
    public void testRefreshToMany() throws Exception {

        deleteTestData();
        createTestData("testRefreshObjectToMany");

        CayenneContext context = createClientContext();

        ClientMtTable1 a = DataObjectUtils.objectForPK(context, ClientMtTable1.class, 1);
        assertEquals(2, a.getTable2Array().size());

        createTestData("testRefreshObjectToManyUpdate");

        RefreshQuery refresh = new RefreshQuery(a);
        context.performGenericQuery(refresh);
        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        assertEquals(1, a.getTable2Array().size());
    }
}
