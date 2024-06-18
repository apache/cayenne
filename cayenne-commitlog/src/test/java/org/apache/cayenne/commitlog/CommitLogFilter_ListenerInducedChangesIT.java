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

package org.apache.cayenne.commitlog;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.commitlog.db.Auditable1;
import org.apache.cayenne.commitlog.db.AuditableChild1;
import org.apache.cayenne.commitlog.model.AttributeChange;
import org.apache.cayenne.commitlog.model.ChangeMap;
import org.apache.cayenne.commitlog.model.ObjectChange;
import org.apache.cayenne.commitlog.model.ObjectChangeType;
import org.apache.cayenne.commitlog.unit.AuditableRuntimeCase;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testing capturing changes introduced by the pre-commit listeners.
 */
public class CommitLogFilter_ListenerInducedChangesIT extends AuditableRuntimeCase {

	protected ObjectContext context;
	protected CommitLogListener mockListener;

	@Override
	protected CayenneRuntimeBuilder configureCayenne() {
		this.mockListener = mock(CommitLogListener.class);
		return super.configureCayenne().addModule(
				b -> CommitLogModule.extend(b).addListener(mockListener));
	}

	@Before
	public void before() {
		context = runtime.newContext();
	}

	@Test
	public void testPostCommit_Insert() {

		final InsertListener listener = new InsertListener();
		runtime.getDataDomain().addListener(listener);

		final Auditable1 a1 = context.newObject(Auditable1.class);
		a1.setCharProperty1("yy");

		doAnswer((Answer<Object>) invocation -> {

			assertNotNull(listener.c);

			List<ObjectChange> sortedChanges = sortedChanges(invocation);

			assertEquals(2, sortedChanges.size());

			assertEquals(a1.getObjectId(), sortedChanges.get(0).getPostCommitId());
			assertEquals(ObjectChangeType.INSERT, sortedChanges.get(0).getType());

			assertEquals(listener.c.getObjectId(), sortedChanges.get(1).getPostCommitId());
			assertEquals(ObjectChangeType.INSERT, sortedChanges.get(1).getType());

			AttributeChange listenerInducedChange = sortedChanges.get(1).getAttributeChanges()
					.get(AuditableChild1.CHAR_PROPERTY1.getName());
			assertNotNull(listenerInducedChange);
			assertEquals("c1", listenerInducedChange.getNewValue());

			return null;
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Delete() throws SQLException {

		auditable1.insert(1, "yy");
		auditableChild1.insert(31, 1, "yyc");

		final DeleteListener listener = new DeleteListener();
		runtime.getDataDomain().addListener(listener);

		final Auditable1 a1 = SelectById.query(Auditable1.class, 1).prefetch(Auditable1.CHILDREN1.joint())
				.selectFirst(context);
		a1.setCharProperty1("zz");

		doAnswer((Answer<Object>) invocation -> {

			assertNotNull(listener.toDelete);
			assertEquals(1, listener.toDelete.size());

			List<ObjectChange> sortedChanges = sortedChanges(invocation);

			assertEquals(2, sortedChanges.size());

			assertEquals(ObjectChangeType.UPDATE, sortedChanges.get(0).getType());
			assertEquals(a1.getObjectId(), sortedChanges.get(0).getPostCommitId());

			assertEquals(ObjectChangeType.DELETE, sortedChanges.get(1).getType());
			assertEquals(listener.toDelete.get(0).getObjectId(), sortedChanges.get(1).getPostCommitId());

			AttributeChange listenerInducedChange = sortedChanges.get(1).getAttributeChanges()
					.get(AuditableChild1.CHAR_PROPERTY1.getName());
			assertNotNull(listenerInducedChange);
			assertEquals("yyc", listenerInducedChange.getOldValue());

			return null;
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	@Test
	public void testPostCommit_Update() throws SQLException {

		auditable1.insert(1, "yy");
		auditableChild1.insert(31, 1, "yyc");

		final UpdateListener listener = new UpdateListener();
		runtime.getDataDomain().addListener(listener);

		final Auditable1 a1 = SelectById.query(Auditable1.class, 1).prefetch(Auditable1.CHILDREN1.joint())
				.selectFirst(context);
		a1.setCharProperty1("zz");

		doAnswer((Answer<Object>) invocation -> {

			assertNotNull(listener.toUpdate);
			assertEquals(1, listener.toUpdate.size());

			List<ObjectChange> sortedChanges = sortedChanges(invocation);

			assertEquals(2, sortedChanges.size());

			assertEquals(ObjectChangeType.UPDATE, sortedChanges.get(0).getType());
			assertEquals(a1.getObjectId(), sortedChanges.get(0).getPostCommitId());

			assertEquals(ObjectChangeType.UPDATE, sortedChanges.get(1).getType());
			assertEquals(listener.toUpdate.get(0).getObjectId(), sortedChanges.get(1).getPostCommitId());

			AttributeChange listenerInducedChange = sortedChanges.get(1).getAttributeChanges()
					.get(AuditableChild1.CHAR_PROPERTY1.getName());
			assertNotNull(listenerInducedChange);
			assertEquals("yyc", listenerInducedChange.getOldValue());
			assertEquals("yyc_", listenerInducedChange.getNewValue());

			return null;
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	private List<ObjectChange> sortedChanges(InvocationOnMock invocation) {
		assertSame(context, invocation.getArguments()[0]);

		ChangeMap changes = (ChangeMap) invocation.getArguments()[1];

		List<ObjectChange> sortedChanges = new ArrayList<>(changes.getUniqueChanges());
		sortedChanges.sort(Comparator.comparing(o -> o.getPostCommitId().getEntityName()));

		return sortedChanges;
	}

	static class InsertListener {

		private AuditableChild1 c;

		@PrePersist(Auditable1.class)
		public void prePersist(Auditable1 a) {

			c = a.getObjectContext().newObject(AuditableChild1.class);
			c.setCharProperty1("c1");
			c.setParent(a);
		}
	}

	static class DeleteListener {

		private List<AuditableChild1> toDelete;

		@PreUpdate(Auditable1.class)
		public void prePersist(Auditable1 a) {

			toDelete = new ArrayList<>(a.getChildren1());
			for (AuditableChild1 c : toDelete) {
				c.getObjectContext().deleteObject(c);
			}
		}
	}

	static class UpdateListener {

		private List<AuditableChild1> toUpdate;

		@PreUpdate(Auditable1.class)
		public void prePersist(Auditable1 a) {

			toUpdate = new ArrayList<>(a.getChildren1());
			for (AuditableChild1 c : toUpdate) {
				c.setCharProperty1(c.getCharProperty1() + "_");
			}
		}
	}

}
