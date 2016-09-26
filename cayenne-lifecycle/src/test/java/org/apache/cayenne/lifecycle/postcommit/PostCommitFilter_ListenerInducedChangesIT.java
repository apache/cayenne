package org.apache.cayenne.lifecycle.postcommit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.lifecycle.changemap.AttributeChange;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.apache.cayenne.lifecycle.changemap.ObjectChange;
import org.apache.cayenne.lifecycle.changemap.ObjectChangeType;
import org.apache.cayenne.lifecycle.db.Auditable1;
import org.apache.cayenne.lifecycle.db.AuditableChild1;
import org.apache.cayenne.lifecycle.unit.AuditableServerCase;
import org.apache.cayenne.query.SelectById;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Testing capturing changes introduced by the pre-commit listeners.
 */
public class PostCommitFilter_ListenerInducedChangesIT extends AuditableServerCase {

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

		final InsertListener listener = new InsertListener();
		runtime.getDataDomain().addListener(listener);

		final Auditable1 a1 = context.newObject(Auditable1.class);
		a1.setCharProperty1("yy");

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

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
			}
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

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

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
			}
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

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {

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
			}
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

	private List<ObjectChange> sortedChanges(InvocationOnMock invocation) {
		assertSame(context, invocation.getArguments()[0]);

		ChangeMap changes = (ChangeMap) invocation.getArguments()[1];

		List<ObjectChange> sortedChanges = new ArrayList<>(changes.getUniqueChanges());
		Collections.sort(sortedChanges, new Comparator<ObjectChange>() {
			public int compare(ObjectChange o1, ObjectChange o2) {
				return o1.getPostCommitId().getEntityName().compareTo(o2.getPostCommitId().getEntityName());
			}
		});

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
