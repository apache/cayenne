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
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.MockEventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.remote.BootstrapMessage;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.GenericResponse;
import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class CayenneContextIT extends ClientCase {

	@Inject
	private ObjectContext serverContext;

	private DefaultEventManager eventManager;

	@After
	public void cleanUp() {
		if(eventManager != null) {
			eventManager.shutdown();
			eventManager = null;
		}
	}

	@Test
	public void testConstructor() {

		CayenneContext context = new CayenneContext();

		// test default property parameters
		assertNotNull(context.getGraphManager());
		assertNull(context.channel);

		MockDataChannel channel = new MockDataChannel();
		context.setChannel(channel);
		assertSame(channel, context.getChannel());
	}

	@Test
	public void testChannel() {
		MockDataChannel channel = new MockDataChannel();
		CayenneContext context = new CayenneContext(channel);

		assertSame(channel, context.getChannel());
	}

	@Test
	public void testCommitUnchanged() {

		MockDataChannel channel = new MockDataChannel();
		CayenneContext context = new CayenneContext(channel);

		// no context changes so no connector access is expected
		context.commitChanges();
		assertTrue(channel.getRequestObjects().isEmpty());
	}

	@Test
	public void testCommitCommandExecuted() {

		MockDataChannel channel = new MockDataChannel(mock(GraphDiff.class));
		channel.setEntityResolver(serverContext.getEntityResolver().getClientEntityResolver());
		CayenneContext context = new CayenneContext(channel);

		// test that a command is being sent via connector on commit...

		context.internalGraphManager().nodePropertyChanged(ObjectId.of("MtTable1"), "x", "y", "z");

		context.commitChanges();
		assertEquals(1, channel.getRequestObjects().size());

		// expect a sync/commit chain
		Object mainMessage = channel.getRequestObjects().iterator().next();
		assertTrue(mainMessage instanceof GraphDiff);
	}

	@Test
	public void testCommitChangesNew() {
		final CompoundDiff diff = new CompoundDiff();
		final Object newObjectId = ObjectId.of("test", "key", "generated");
		eventManager = new DefaultEventManager(0);

		// test that ids that are passed back are actually propagated to the
		// right
		// objects...

		MockDataChannel channel = new MockDataChannel() {

			@Override
			public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {

				return diff;
			}

			// must provide a channel with working event manager
			@Override
			public EventManager getEventManager() {
				return eventManager;
			}
		};

		CayenneContext context = new CayenneContext(channel);
		ObjEntity entity = new ObjEntity("test_entity");
		entity.setClassName(MockPersistentObject.class.getName());

		DataMap dataMap = new DataMap("test");
		dataMap.addObjEntity(entity);
		Collection<DataMap> entities = Collections.singleton(dataMap);
		context.setEntityResolver(new EntityResolver(entities));
		Persistent object = context.newObject(MockPersistentObject.class);

		// record change here to make it available to the anonymous connector
		// method..
		diff.add(new NodeIdChangeOperation(object.getObjectId(), newObjectId));

		// check that a generated object ID is assigned back to the object...
		assertNotSame(newObjectId, object.getObjectId());
		context.commitChanges();
		assertSame(newObjectId, object.getObjectId());
		assertSame(object, context.graphManager.getNode(newObjectId));
	}

	@Test
	public void testNewObject() {

		CayenneContext context = new CayenneContext(new MockDataChannel());

		ObjEntity entity = new ObjEntity("test_entity");
		entity.setClassName(MockPersistentObject.class.getName());

		DataMap dataMap = new DataMap("test");
		dataMap.addObjEntity(entity);
		Collection<DataMap> entities = Collections.singleton(dataMap);
		context.setEntityResolver(new EntityResolver(entities));

		Persistent object = context.newObject(MockPersistentObject.class);
		assertNotNull(object);
		assertTrue(object instanceof MockPersistentObject);
		assertEquals(PersistenceState.NEW, object.getPersistenceState());
		assertSame(context, object.getObjectContext());
		assertTrue(context.internalGraphManager().dirtyNodes(PersistenceState.NEW).contains(object));
		assertNotNull(object.getObjectId());
		assertTrue(object.getObjectId().isTemporary());
	}

	@Test
	public void testDeleteObject() {

		CayenneContext context = new CayenneContext(new MockDataChannel());
		ObjEntity entity = new ObjEntity("test_entity");
		entity.setClassName(MockPersistentObject.class.getName());

		DataMap dataMap = new DataMap("test");
		dataMap.addObjEntity(entity);
		Collection<DataMap> entities = Collections.singleton(dataMap);
		context.setEntityResolver(new EntityResolver(entities));

		// TRANSIENT ... should quietly ignore it
		Persistent transientObject = new MockPersistentObject();
		context.deleteObjects(transientObject);
		assertEquals(PersistenceState.TRANSIENT, transientObject.getPersistenceState());

		// NEW ... should make it TRANSIENT
		// create via context to make sure that object store would register it
		Persistent newObject = context.newObject(MockPersistentObject.class);
		assertNotNull(newObject.getObjectContext());
		context.deleteObjects(newObject);
		assertNull(newObject.getObjectContext());
		assertEquals(PersistenceState.TRANSIENT, newObject.getPersistenceState());
		assertFalse(context.internalGraphManager().dirtyNodes().contains(newObject.getObjectId()));

		// see CAY-547 for details...
		assertFalse(context.internalGraphManager().dirtyNodes().contains(null));

		// COMMITTED
		Persistent committed = new MockPersistentObject();
		committed.setPersistenceState(PersistenceState.COMMITTED);
		committed.setObjectId(ObjectId.of("test_entity", "key", "value1"));
		committed.setObjectContext(context);
		context.deleteObjects(committed);
		assertEquals(PersistenceState.DELETED, committed.getPersistenceState());

		// MODIFIED
		Persistent modified = new MockPersistentObject();
		modified.setPersistenceState(PersistenceState.MODIFIED);
		modified.setObjectId(ObjectId.of("test_entity", "key", "value2"));
		modified.setObjectContext(context);
		context.deleteObjects(modified);
		assertEquals(PersistenceState.DELETED, modified.getPersistenceState());

		// DELETED
		Persistent deleted = new MockPersistentObject();
		deleted.setPersistenceState(PersistenceState.DELETED);
		deleted.setObjectId(ObjectId.of("test_entity", "key", "value3"));
		deleted.setObjectContext(context);
		context.deleteObjects(deleted);
		assertEquals(PersistenceState.DELETED, committed.getPersistenceState());
	}

	@Test
	public void testBeforePropertyReadShouldInflateHollow() {

        ObjectId gid = ObjectId.of("MtTable1", "a", "b");
		final ClientMtTable1 inflated = new ClientMtTable1();
		inflated.setPersistenceState(PersistenceState.COMMITTED);
		inflated.setObjectId(gid);
		inflated.setGlobalAttribute1("abc");

		ClientConnection connection = mock(ClientConnection.class);
		when(connection.sendMessage((ClientMessage) any())).thenAnswer(new Answer<Object>() {

			public Object answer(InvocationOnMock invocation) {
				ClientMessage arg = (ClientMessage) invocation.getArguments()[0];

				if (arg instanceof BootstrapMessage) {
					return new EntityResolver();
				} else {
					return new GenericResponse(Arrays.asList(inflated));
				}
			}
		});

		ClientChannel channel = new ClientChannel(connection, false, new MockEventManager(), false);

		// check that a HOLLOW object is infalted on "beforePropertyRead"
		ClientMtTable1 hollow = new ClientMtTable1();
		hollow.setPersistenceState(PersistenceState.HOLLOW);
		hollow.setObjectId(gid);

		final boolean[] selectExecuted = new boolean[1];
		CayenneContext context = new CayenneContext(channel) {

			@Override
			public List<?> performQuery(Query query) {
				selectExecuted[0] = true;
				return super.performQuery(query);
			}
		};

		context.setEntityResolver(serverContext.getEntityResolver().getClientEntityResolver());

		context.graphManager.registerNode(hollow.getObjectId(), hollow);

		// testing this...
		context.prepareForAccess(hollow, ClientMtTable1.GLOBAL_ATTRIBUTE1.getName(), false);
		assertTrue(selectExecuted[0]);
		assertSame(hollow, context.getGraphManager().getNode(gid));
		assertEquals(inflated.getGlobalAttribute1Direct(), hollow.getGlobalAttribute1Direct());
		assertEquals(PersistenceState.COMMITTED, hollow.getPersistenceState());
	}

	@Test
	public void testBeforeHollowDeleteShouldChangeStateToCommited() {

        ObjectId gid = ObjectId.of("MtTable1", "a", "b");
		final ClientMtTable1 inflated = new ClientMtTable1();
		inflated.setPersistenceState(PersistenceState.COMMITTED);
		inflated.setObjectId(gid);
		inflated.setGlobalAttribute1("abc");

		ClientConnection connection = mock(ClientConnection.class);
		when(connection.sendMessage((ClientMessage) any())).thenAnswer(new Answer<Object>() {

			public Object answer(InvocationOnMock invocation) {
				ClientMessage arg = (ClientMessage) invocation.getArguments()[0];

				if (arg instanceof BootstrapMessage) {
					return new EntityResolver();
				} else {
					return new GenericResponse(Arrays.asList(inflated));
				}
			}
		});
		ClientChannel channel = new ClientChannel(connection, false, new MockEventManager(), false);

		CayenneContext context = new CayenneContext(channel);
		context.setEntityResolver(serverContext.getEntityResolver().getClientEntityResolver());
		ClientMtTable1 hollow = context.localObject(inflated);
		assertEquals(PersistenceState.HOLLOW, hollow.getPersistenceState());

		// testing this...
		context.deleteObjects(hollow);
		assertSame(hollow, context.getGraphManager().getNode(gid));
		assertEquals(inflated.getGlobalAttribute1Direct(), hollow.getGlobalAttribute1Direct());
		assertEquals(PersistenceState.DELETED, hollow.getPersistenceState());
	}

}
