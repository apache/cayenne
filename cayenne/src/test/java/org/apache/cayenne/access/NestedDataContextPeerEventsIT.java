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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.util.RuntimeCaseSyncModule;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.ExtraModules;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
@ExtraModules(RuntimeCaseSyncModule.class)
public class NestedDataContextPeerEventsIT extends RuntimeCase {

	@Inject
	private DataContext context;

	@Inject
	private CayenneRuntime runtime;

    @Test
	public void testPeerObjectUpdatedTempOID() throws Exception {

		ObjectContext peer1 = runtime.newContext(context);
		final Artist a1 = peer1.newObject(Artist.class);
		a1.setArtistName("Y");

		ObjectId a1TempId = a1.getObjectId();
		assertTrue(a1TempId.isTemporary());

		ObjectContext peer2 = runtime.newContext(context);
		final Artist a2 = peer2.localObject(a1);

		assertEquals(a1TempId, a2.getObjectId());

		peer1.commitChanges();

		// pause to let the context events propagate
		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				assertFalse(a1.getObjectId().isTemporary());
				assertFalse(a2.getObjectId().isTemporary());
				assertEquals(a2.getObjectId(), a1.getObjectId());
			}
		}.runTest(2000);
	}

    @Test
	public void testPeerObjectUpdatedSimpleProperty() throws Exception {
		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");
		context.commitChanges();

		ObjectContext peer1 = runtime.newContext(context);
		Artist a1 = peer1.localObject(a);

		final ObjectContext peer2 = runtime.newContext(context);
		final Artist a2 = peer2.localObject(a);

		a1.setArtistName("Y");
		assertEquals("X", a2.getArtistName());
		peer1.commitChangesToParent();

		// pause to let the context events propagate
		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				assertEquals("Y", a2.getArtistName());
				assertFalse(
						"Peer data context became dirty on event processing",
						peer2.hasChanges());
			}
		}.runTest(2000);

	}

    @Test
	public void testPeerObjectUpdatedToOneRelationship() throws Exception {

		Artist a = context.newObject(Artist.class);
		Artist altA = context.newObject(Artist.class);

		Painting p = context.newObject(Painting.class);
		p.setToArtist(a);
		p.setPaintingTitle("PPP");
		a.setArtistName("X");
		altA.setArtistName("Y");
		context.commitChanges();

		ObjectContext peer1 = runtime.newContext(context);
		Painting p1 = peer1.localObject(p);
		Artist altA1 = peer1.localObject(altA);

		final ObjectContext peer2 = runtime.newContext(context);
		final Painting p2 = peer2.localObject(p);
		final Artist altA2 = peer2.localObject(altA);
		Artist a2 = peer2.localObject(a);

		p1.setToArtist(altA1);
		assertSame(a2, p2.getToArtist());
		peer1.commitChangesToParent();

		// pause to let the context events propagate
		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				assertEquals(altA2, p2.getToArtist());

				assertFalse(
						"Peer data context became dirty on event processing",
						peer2.hasChanges());
			}
		}.runTest(2000);

	}

    @Test
	public void testPeerObjectUpdatedToManyRelationship() throws Exception {

		Artist a = context.newObject(Artist.class);
		a.setArtistName("X");

		Painting px = context.newObject(Painting.class);
		px.setToArtist(a);
		px.setPaintingTitle("PX");

		Painting py = context.newObject(Painting.class);
		py.setPaintingTitle("PY");

		context.commitChanges();
		// pause to let the context events propagate
		Thread.sleep(500);

		ObjectContext peer1 = runtime.newContext(context);
		Painting py1 = peer1.localObject(py);
		Artist a1 = peer1.localObject(a);

		final ObjectContext peer2 = runtime.newContext(context);
		final Painting py2 = peer2.localObject(py);
		final Artist a2 = peer2.localObject(a);

		a1.addToPaintingArray(py1);
		assertEquals(1, a2.getPaintingArray().size());
		assertFalse(a2.getPaintingArray().contains(py2));
		peer1.commitChangesToParent();
		// pause to let the context events propagate
		new ParallelTestContainer() {

			@Override
			protected void assertResult() throws Exception {
				assertEquals(2, a2.getPaintingArray().size());
				assertTrue(a2.getPaintingArray().contains(py2));

				assertFalse(
						"Peer data context became dirty on event processing",
						peer2.hasChanges());
			}
		}.runTest(2000);

	}
}
