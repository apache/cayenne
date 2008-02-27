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
package org.apache.cayenne.remote;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class ClientChannelServerDiffsTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testReturnIdDiff() {

        final Object[] ids = new Object[2];

        final GraphChangeHandler diffReader = new NoopGraphChangeHandler() {

            @Override
            public void nodeIdChanged(Object oldId, Object newId) {
                ids[0] = oldId;
                ids[1] = newId;
            }
        };

        ClientServerChannel csChannel = new ClientServerChannel(getDomain());
        ClientChannel channel = new ClientChannel(new LocalConnection(csChannel)) {

            @Override
            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType) {

                GraphDiff serverDiff = super
                        .onSync(originatingContext, changes, syncType);

                assertNotNull(serverDiff);
                serverDiff.apply(diffReader);
                return serverDiff;
            }
        };

        CayenneContext context = new CayenneContext(channel);
        context.newObject(ClientMtTable1.class);
        context.commitChanges();

        assertTrue(ids[0] instanceof ObjectId);
        assertTrue(((ObjectId) ids[0]).isTemporary());
        
        assertTrue(ids[1] instanceof ObjectId);
        assertFalse(((ObjectId) ids[1]).isTemporary());
    }

    class NoopGraphChangeHandler implements GraphChangeHandler {

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        }

        public void nodeCreated(Object nodeId) {
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {
        }

        public void nodeRemoved(Object nodeId) {
        }

    }
}
