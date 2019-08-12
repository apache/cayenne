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

package org.apache.cayenne.remote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.MockPersistentObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.CayenneEvent;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.MockEventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.util.GenericResponse;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClientChannelTest {

    private List<DefaultEventManager> managers = new ArrayList<>();

    @After
    public void cleanUp() {
        if(managers.size() == 0) {
            return;
        }
        for(DefaultEventManager manager : managers) {
            manager.shutdown();
        }
        managers.clear();
    }

    @Test
    public void testOnQuerySelect() {

        final MockPersistentObject o1 = new MockPersistentObject();
        ObjectId oid1 = ObjectId.of("test_entity");
        o1.setObjectId(oid1);

        ClientConnection connection = mock(ClientConnection.class);
        when(connection.sendMessage(any())).thenAnswer(
                invocation -> {
                    ClientMessage arg = (ClientMessage) invocation.getArguments()[0];

                    if (arg instanceof BootstrapMessage) {
                        return new EntityResolver();
                    }
                    else {
                        return new GenericResponse(Arrays.asList(o1));
                    }
                });

        ClientChannel channel = new ClientChannel(
                connection,
                false,
                new MockEventManager(),
                false);

        CayenneContext context = new CayenneContext(channel);
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection<DataMap> entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        QueryResponse response = channel.onQuery(context, ObjectSelect.query(MockPersistentObject.class));
        assertNotNull(response);
        List<?> list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        Persistent o1_1 = (Persistent) list.get(0);

        assertEquals(o1.getObjectId(), o1_1.getObjectId());

        // ObjectContext must be injected
        assertEquals(context, o1_1.getObjectContext());
        assertSame(o1_1, context.getGraphManager().getNode(oid1));
    }

    @Test
    public void testOnQuerySelectOverrideCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection<DataMap> entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ObjectId oid = ObjectId.of("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        context.getGraphManager().registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);

        MockClientConnection connection = new MockClientConnection(new GenericResponse(
                Arrays.asList(o2)));

        ClientChannel channel = new ClientChannel(
                connection,
                false,
                new MockEventManager(),
                false);

        context.setChannel(channel);
        QueryResponse response = channel.onQuery(context, ObjectSelect.query(MockPersistentObject.class));
        assertNotNull(response);

        List<?> list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.getGraphManager().getNode(oid));
    }

    @Test
    public void testOnQuerySelectOverrideModifiedCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());
        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection<DataMap> entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);
        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ObjectId oid = ObjectId.of("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        o1.setPersistenceState(PersistenceState.MODIFIED);
        context.getGraphManager().registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);
        MockClientConnection connection = new MockClientConnection(new GenericResponse(
                Arrays.asList(o2)));

        ClientChannel channel = new ClientChannel(
                connection,
                false,
                new MockEventManager(),
                false);

        context.setChannel(channel);
        QueryResponse response = channel.onQuery(context, ObjectSelect.query(MockPersistentObject.class));
        assertNotNull(response);
        assertEquals(1, response.size());
        List<?> list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.getGraphManager().getNode(oid));
    }

    @Test
    public void testEventBridgeFailure() throws Exception {
        MockClientConnection connection = new MockClientConnection() {

            @Override
            public EventBridge getServerEventBridge() throws CayenneRuntimeException {
                return new EventBridge(Collections.emptyList(), "ext") {

                    @Override
                    protected void sendExternalEvent(CayenneEvent localEvent)
                            throws Exception {
                    }

                    @Override
                    protected void shutdownExternal() throws Exception {
                    }

                    @Override
                    protected void startupExternal() throws Exception {
                        // intentionally throw an exception
                        throw new CayenneRuntimeException("Test failure");
                    }
                };
            }
        };

        // default constructor must fail
        try {
            new ClientChannel(connection, false, new MockEventManager(), false);
            fail("Channel didn't throw on broken EventBridge");
        } catch (CayenneRuntimeException e) {
            // expected
        }

        try {
            DefaultEventManager manager = new DefaultEventManager(2);
            managers.add(manager);
            new ClientChannel(connection, false, manager, false);
            fail("Channel didn't throw on broken EventBridge");
        } catch (CayenneRuntimeException e) {
            // expected
        }

        try {
            DefaultEventManager manager = new DefaultEventManager(2);
            managers.add(manager);
            new ClientChannel(connection, false, manager, true);
        } catch (CayenneRuntimeException e) {
            fail("Channel threw on broken EventBridge");
        }
    }
}
