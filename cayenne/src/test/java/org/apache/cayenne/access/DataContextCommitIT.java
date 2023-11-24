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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.NullTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextCommitIT extends RuntimeCase {

	@Inject
	private DataContext context;

	@Test
	public void testFlushToParent_Commit_New() {

		// commit new object
		Artist a = context.newObject(Artist.class);
		a.setArtistName("Test");

		assertTrue(context.hasChanges());

		ObjectId beforeId = a.getObjectId();
		GraphDiff diff = context.flushToParent(true);
		ObjectId afterId = a.getObjectId();

		assertNotNull(diff);
		assertFalse(context.hasChanges());
		assertNotEquals(beforeId, afterId);

		GraphChangeHandler diffChecker = mock(GraphChangeHandler.class);

		diff.apply(diffChecker);

		verify(diffChecker).nodeIdChanged(beforeId, afterId);
		verifyNoMoreInteractions(diffChecker);

	}

	@Test
	public void testFlushToParent_Commit_Mix() {

		Artist a = context.newObject(Artist.class);
		a.setArtistName("Test");
		context.flushToParent(true);

		// commit a mix of new and modified
		Painting p = context.newObject(Painting.class);
		p.setPaintingTitle("PT");
		p.setToArtist(a);
		a.setArtistName("Test_");

		ObjectId beforeId = p.getObjectId();
		GraphDiff diff = context.flushToParent(true);
		ObjectId afterId = p.getObjectId();

		assertNotNull(diff);
		assertFalse(context.hasChanges());

		GraphChangeHandler diffChecker = mock(GraphChangeHandler.class);

		diff.apply(diffChecker);
		verify(diffChecker).nodeIdChanged(beforeId, afterId);
		verifyNoMoreInteractions(diffChecker);
	}

	@Test
	public void testFlushToParent_NewNoAttributes() {

		// commit new object with uninitialized attributes

		context.newObject(NullTestEntity.class);

		assertTrue(context.hasChanges());

		GraphDiff diff3 = context.flushToParent(true);
		assertNotNull(diff3);
		assertFalse(context.hasChanges());
	}
}
