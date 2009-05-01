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
package org.apache.cayenne.gen;

import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMultiTier;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class ClientDataMapGeneratedQueryRunTest extends CayenneCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testPerformGeneratedQuery() {

        ClientConnection connection = new LocalConnection(new ClientServerChannel(
                getDomain()));
        ClientChannel channel = new ClientChannel(connection);
        CayenneContext context = new CayenneContext(channel);

        ClientMtTable1 obj = context.newObject(ClientMtTable1.class);

        obj.setGlobalAttribute1("A1");
        context.commitChanges();

        List<ClientMtTable1> result = ClientMultiTier.getInstance().performAllMtTable1(
                context);
        assertEquals("A1", result.get(0).getGlobalAttribute1());
    }
}
