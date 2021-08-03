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

package org.apache.cayenne.access;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelListener;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphEvent;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCaseContextsSync;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.EventUtil;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests that DataContext sends DataChannel events.
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextDataChannelEventsIT extends ServerCaseContextsSync {

	@Inject
	private DataContext context;

	@Inject
	private DataContext peer;

	@Inject
	private ServerRuntime runtime;

	@Test
	public void testCommitEvent() throws Exception {
		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");
		context.commitChanges();

		// Construct mock object
		final DataChannelListener listener = mock(DataChannelListener.class);
		EventUtil.listenForChannelEvents(context, listener);

		a.setArtistName("Y");
		context.commitChanges();

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				Mockito.verify(listener, Mockito.atLeastOnce()).graphFlushed(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphChanged(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphRolledback(any(GraphEvent.class));
			}
		}.runTest(10000);

	}

	@Test
	public void testRollbackEvent() throws Exception {
		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");
		context.commitChanges();

		// Construct mock object
		final DataChannelListener listener = mock(DataChannelListener.class);
		EventUtil.listenForChannelEvents(context, listener);

		a.setArtistName("Y");
		context.rollbackChanges();

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				Mockito.verify(listener, Mockito.never()).graphFlushed(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphChanged(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.atLeastOnce()).graphRolledback(any(GraphEvent.class));
			}
		}.runTest(10000);
	}

	@Test
	public void testChangeEventOnChildChange() throws Exception {
		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");
		context.commitChanges();

		// Construct mock object
		final DataChannelListener listener = mock(DataChannelListener.class);
		EventUtil.listenForChannelEvents(context, listener);

		ObjectContext childContext = runtime.newContext(context);

		Artist a1 = childContext.localObject(a);

		a1.setArtistName("Y");
		childContext.commitChangesToParent();

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				Mockito.verify(listener, Mockito.never()).graphFlushed(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.atLeastOnce()).graphChanged(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphRolledback(any(GraphEvent.class));
			}
		}.runTest(10000);
	}

	@Test
	public void testChangeEventOnPeerChange() throws Exception {
		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");
		context.commitChanges();

		// Construct mock object
		final DataChannelListener listener = mock(DataChannelListener.class);
		EventUtil.listenForChannelEvents(context, listener);

		Artist a1 = peer.localObject(a);

		a1.setArtistName("Y");
		peer.commitChangesToParent();

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				Mockito.verify(listener, Mockito.never()).graphFlushed(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.atLeastOnce()).graphChanged(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphRolledback(any(GraphEvent.class));
			}
		}.runTest(10000);
	}

	@Test
	public void testChangeEventOnPeerChangeSecondNestingLevel() throws Exception {
		ObjectContext childPeer1 = runtime.newContext(context);

		Artist a = childPeer1.newObject(Artist.class);
		a.setArtistName("X");
		childPeer1.commitChanges();

		// Construct mock object
		final DataChannelListener listener = mock(DataChannelListener.class);
		EventUtil.listenForChannelEvents((DataChannel) childPeer1, listener);

		ObjectContext childPeer2 = runtime.newContext(context);

		Artist a1 = childPeer2.localObject(a);

		a1.setArtistName("Y");
		childPeer2.commitChangesToParent();

		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				Mockito.verify(listener, Mockito.never()).graphFlushed(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.atLeastOnce()).graphChanged(any(GraphEvent.class));
				Mockito.verify(listener, Mockito.never()).graphRolledback(any(GraphEvent.class));
			}
		}.runTest(10000);
	}
}
