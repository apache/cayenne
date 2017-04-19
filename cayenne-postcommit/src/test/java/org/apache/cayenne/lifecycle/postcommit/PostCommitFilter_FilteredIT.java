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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.lifecycle.changemap.AttributeChange;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.apache.cayenne.lifecycle.changemap.ObjectChange;
import org.apache.cayenne.lifecycle.changemap.ObjectChangeType;
import org.apache.cayenne.lifecycle.changemap.ToManyRelationshipChange;
import org.apache.cayenne.lifecycle.db.Auditable1;
import org.apache.cayenne.lifecycle.db.Auditable2;
import org.apache.cayenne.lifecycle.db.Auditable3;
import org.apache.cayenne.lifecycle.db.Auditable4;
import org.apache.cayenne.lifecycle.db.AuditableChild1;
import org.apache.cayenne.lifecycle.unit.AuditableServerCase;
import org.apache.cayenne.query.SelectById;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PostCommitFilter_FilteredIT extends AuditableServerCase {

	protected ObjectContext context;
	protected PostCommitListener mockListener;

	@Override
	protected ServerRuntimeBuilder configureCayenne() {
		this.mockListener = mock(PostCommitListener.class);
		return super.configureCayenne().addModule(
				PostCommitModuleBuilder.builder().auditableEntitiesOnly().listener(mockListener).build());
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
				assertEquals(ObjectChangeType.INSERT, c.getType());
				assertEquals(1, c.getAttributeChanges().size());

				assertEquals(Confidential.getInstance(),
						c.getAttributeChanges().get(Auditable2.CHAR_PROPERTY2.getName()).getNewValue());

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable2 a1 = context.newObject(Auditable2.class);
		a1.setCharProperty1("yy");
		a1.setCharProperty2("zz");
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Update() throws SQLException {
		auditable2.insert(1, "P1_1", "P2_1");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange c = changes.getChanges().get(new ObjectId("Auditable2", Auditable2.ID_PK_COLUMN, 1));
				assertNotNull(c);
				assertEquals(ObjectChangeType.UPDATE, c.getType());
				assertEquals(1, c.getAttributeChanges().size());
				AttributeChange pc = c.getAttributeChanges().get(Auditable2.CHAR_PROPERTY2.getName());
				assertNotNull(pc);
				assertEquals(Confidential.getInstance(), pc.getOldValue());
				assertEquals(Confidential.getInstance(), pc.getNewValue());

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable2 a1 = SelectById.query(Auditable2.class, 1).selectOne(context);
		a1.setCharProperty1("P1_2");
		a1.setCharProperty2("P2_2");
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Delete() throws SQLException {
		auditable2.insert(1, "P1_1", "P2_1");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange c = changes.getChanges().get(new ObjectId("Auditable2", Auditable2.ID_PK_COLUMN, 1));
				assertNotNull(c);
				assertEquals(ObjectChangeType.DELETE, c.getType());
				assertEquals(1, c.getAttributeChanges().size());
				assertEquals(Confidential.getInstance(),
						c.getAttributeChanges().get(Auditable2.CHAR_PROPERTY2.getName()).getOldValue());

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		Auditable2 a1 = SelectById.query(Auditable2.class, 1).selectOne(context);
		context.deleteObject(a1);
		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_UpdateToMany() throws SQLException {
		auditable1.insert(1, "xx");
		auditableChild1.insert(1, 1, "cc1");
		auditableChild1.insert(2, null, "cc2");
		auditableChild1.insert(3, null, "cc3");

		final AuditableChild1 ac1 = SelectById.query(AuditableChild1.class, 1).selectOne(context);
		final AuditableChild1 ac2 = SelectById.query(AuditableChild1.class, 2).selectOne(context);
		final AuditableChild1 ac3 = SelectById.query(AuditableChild1.class, 3).selectOne(context);

		final Auditable1 a1 = SelectById.query(Auditable1.class, 1).selectOne(context);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNotNull(changes);
				assertEquals(1, changes.getUniqueChanges().size());

				ObjectChange a1c = changes.getChanges().get(new ObjectId("Auditable1", Auditable1.ID_PK_COLUMN, 1));
				assertNotNull(a1c);
				assertEquals(ObjectChangeType.UPDATE, a1c.getType());
				assertEquals(0, a1c.getAttributeChanges().size());

				assertEquals(1, a1c.getToManyRelationshipChanges().size());

				ToManyRelationshipChange a1c1 = a1c.getToManyRelationshipChanges().get(Auditable1.CHILDREN1.getName());
				assertNotNull(a1c1);

				assertEquals(2, a1c1.getAdded().size());
				assertTrue(a1c1.getAdded().contains(ac2.getObjectId()));
				assertTrue(a1c1.getAdded().contains(ac3.getObjectId()));

				assertEquals(1, a1c1.getRemoved().size());
				assertTrue(a1c1.getRemoved().contains(ac1.getObjectId()));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		a1.removeFromChildren1(ac1);
		a1.addToChildren1(ac2);
		a1.addToChildren1(ac3);

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_IgnoreAttributes() throws SQLException {

		auditable3.insert(1, "31", "32");

		final Auditable3 a3 = SelectById.query(Auditable3.class, 1).selectOne(context);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNull(changes.getChanges().get(new ObjectId("Auditable3", Auditable3.ID_PK_COLUMN, 1)));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		a3.setCharProperty1("33");
		a3.setCharProperty2("34");

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_IgnoreToMany() throws SQLException {

		auditable3.insert(1, "31", "32");
		auditable4.insert(11, "41", "42", 1);
		auditable4.insert(12, "43", "44", 1);

		final Auditable3 a3 = SelectById.query(Auditable3.class, 1).selectOne(context);
		final Auditable4 a41 = SelectById.query(Auditable4.class, 11).selectOne(context);
		final Auditable4 a42 = SelectById.query(Auditable4.class, 12).selectOne(context);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNull(changes.getChanges().get(new ObjectId("Auditable3", Auditable3.ID_PK_COLUMN, 1)));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		a3.removeFromAuditable4s(a41);
		a3.addToAuditable4s(a42);

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_IgnoreToOne() throws SQLException {

		auditable3.insert(1, "31", "32");
		auditable3.insert(2, "33", "34");
		auditable4.insert(11, "41", "41", 1);

		final Auditable3 a32 = SelectById.query(Auditable3.class, 2).selectOne(context);

		final Auditable4 a4 = SelectById.query(Auditable4.class, 11).selectOne(context);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

				assertSame(context, invocation.getArguments()[0]);

				ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
				assertNull(changes.getChanges().get(new ObjectId("Auditable4", Auditable4.ID_PK_COLUMN, 11)));

				return null;
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		a4.setAuditable3(a32);

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}
}
