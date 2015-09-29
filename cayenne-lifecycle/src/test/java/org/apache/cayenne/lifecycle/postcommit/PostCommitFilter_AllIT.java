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
package org.apache.cayenne.lifecycle.postcommit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.Map;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.apache.cayenne.lifecycle.changemap.ObjectChange;
import org.apache.cayenne.lifecycle.changemap.ObjectChangeType;
import org.apache.cayenne.lifecycle.changemap.ObjectPropertyChange;
import org.apache.cayenne.lifecycle.db.Auditable1;
import org.apache.cayenne.lifecycle.unit.LifecycleServerCase;
import org.apache.cayenne.query.SelectById;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PostCommitFilter_AllIT extends LifecycleServerCase {

	protected ObjectContext context;
	protected PostCommitListener mockListener;

	@Override
	protected ServerRuntimeBuilder configureCayenne() {
		this.mockListener = mock(PostCommitListener.class);
		return super.configureCayenne().addModule(PostCommitModuleBuilder.builder().listener(mockListener).build());
	}

	@Before
	public void before() {
		context = runtime.newContext();
	}

	@Test
	public void testPostCommit_Insert() throws SQLException {

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(2, changes.getChanges().size());
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange c = changes.getUniqueChanges().iterator().next();
				assertNotNull(c);
				assertEquals(ObjectChangeType.INSERT, c.getChangeType());
				assertEquals(0, c.getChanges().size());
				Map<String, Object> snapshot = c.getSnapshot();
				assertNotNull(snapshot);
				assertEquals("yy", snapshot.get(Auditable1.CHAR_PROPERTY1_PROPERTY));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable1 a1 = context.newObject(Auditable1.class);
		a1.setCharProperty1("yy");
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Update() throws SQLException {
		auditable1.insert(1, "xx");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange c = changes.getChanges().get(new ObjectId("Auditable1", Auditable1.ID_PK_COLUMN, 1));
				assertNotNull(c);
				assertEquals(ObjectChangeType.UPDATE, c.getChangeType());
				assertEquals(1, c.getChanges().size());
				ObjectPropertyChange pc = c.getChanges().get(Auditable1.CHAR_PROPERTY1_PROPERTY);
				assertNotNull(pc);
				assertEquals("xx", pc.getOldValue());
				assertEquals("yy", pc.getNewValue());

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);
		a1.setCharProperty1("yy");
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Delete() throws SQLException {
		auditable1.insert(1, "xx");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange c = changes.getChanges().get(new ObjectId("Auditable1", Auditable1.ID_PK_COLUMN, 1));
				assertNotNull(c);
				assertEquals(ObjectChangeType.DELETE, c.getChangeType());
				assertEquals(0, c.getChanges().size());
				Map<String, Object> snapshot = c.getSnapshot();
				assertNotNull(snapshot);
				assertEquals("xx", snapshot.get(Auditable1.CHAR_PROPERTY1_PROPERTY));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);
		context.deleteObject(a1);
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

}
