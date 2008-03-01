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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.testdo.mt.MtTable1;
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

    public void testReturnDiffInPrePersist() {

        final List<GenericDiff> diffs = new ArrayList<GenericDiff>();
        final NoopGraphChangeHandler diffReader = new NoopGraphChangeHandler() {

            @Override
            public void nodePropertyChanged(
                    Object nodeId,
                    String property,
                    Object oldValue,
                    Object newValue) {

                super.nodePropertyChanged(nodeId, property, oldValue, newValue);
                diffs
                        .add(new GenericDiff(
                                (ObjectId) nodeId,
                                property,
                                oldValue,
                                newValue));
            }

        };

        LifecycleCallbackRegistry registry = getDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        try {

            registry.addListener(
                    LifecycleEvent.PRE_PERSIST,
                    MtTable1.class,
                    new ClientChannelServerDiffsListener1(),
                    "prePersist");

            ClientServerChannel csChannel = new ClientServerChannel(getDomain());
            ClientChannel channel = new ClientChannel(new LocalConnection(csChannel)) {

                @Override
                public GraphDiff onSync(
                        ObjectContext originatingContext,
                        GraphDiff changes,
                        int syncType) {

                    GraphDiff serverDiff = super.onSync(
                            originatingContext,
                            changes,
                            syncType);

                    assertNotNull(serverDiff);
                    serverDiff.apply(diffReader);
                    return serverDiff;
                }
            };

            CayenneContext context = new CayenneContext(channel);
            ClientMtTable1 o = context.newObject(ClientMtTable1.class);
            ObjectId tempId = o.getObjectId();
            o.setServerAttribute1("YY");
            context.commitChanges();

            assertEquals(2, diffReader.size);
            assertEquals(1, diffs.size());
            assertEquals(tempId, diffs.get(0).sourceId);
            assertEquals(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, diffs.get(0).property);
            assertNull(diffs.get(0).oldValue);
            assertEquals("XXX", diffs.get(0).newValue);
        }
        finally {
            registry.clear();
        }
    }

    public void testReturnDiffClientArcChanges() {

        final NoopGraphChangeHandler diffReader = new NoopGraphChangeHandler();

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
        ClientMtTable1 o = context.newObject(ClientMtTable1.class);
        ClientMtTable2 o2 = context.newObject(ClientMtTable2.class);
        o.addToTable2Array(o2);
        context.commitChanges();

        assertEquals(2, diffReader.size);

        diffReader.reset();

        ClientMtTable2 o3 = context.newObject(ClientMtTable2.class);
        o3.setTable1(o);
        context.commitChanges();
        assertEquals(1, diffReader.size);
    }

    class NoopGraphChangeHandler implements GraphChangeHandler {

        int size;

        void reset() {
            size = 0;
        }

        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {

            size++;
        }

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
            size++;
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
            size++;
        }

        public void nodeCreated(Object nodeId) {
            size++;
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
            size++;
        }

        public void nodeRemoved(Object nodeId) {
            size++;
        }
    }

    class GenericDiff {

        private String property;
        private Object oldValue;
        private Object newValue;
        private ObjectId sourceId;
        private ObjectId targetId;

        GenericDiff(ObjectId sourceId, String property, Object oldValue, Object newValue) {

            this(sourceId, null, property, oldValue, newValue);
        }

        GenericDiff(ObjectId sourceId, ObjectId targetId, String property,
                Object oldValue, Object newValue) {

            this.sourceId = sourceId;
            this.targetId = targetId;
            this.property = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
}
