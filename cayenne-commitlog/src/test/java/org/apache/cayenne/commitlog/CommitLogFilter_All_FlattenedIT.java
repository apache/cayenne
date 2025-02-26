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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.commitlog.db.E3;
import org.apache.cayenne.commitlog.db.E4;
import org.apache.cayenne.commitlog.model.ChangeMap;
import org.apache.cayenne.commitlog.model.ObjectChange;
import org.apache.cayenne.commitlog.model.ObjectChangeType;
import org.apache.cayenne.commitlog.model.ToManyRelationshipChange;
import org.apache.cayenne.commitlog.unit.FlattenedRuntimeCase;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.sql.SQLException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CommitLogFilter_All_FlattenedIT extends FlattenedRuntimeCase {

	protected ObjectContext context;
	protected CommitLogListener mockListener;

	@Override
	protected CayenneRuntimeBuilder configureCayenne() {
		this.mockListener = mock(CommitLogListener.class);
		return super.configureCayenne()
				.addModule(b -> CoreModule.extend(b)
						.setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "soft"))
				.addModule(b -> CommitLogModule.extend(b).addListener(mockListener));
	}

	@Before
	public void before() {
		context = runtime.newContext();
	}

	@Test
	public void testPostCommit_UpdateToMany() throws SQLException {
		e3.insert(1);
		e4.insert(11);
		e4.insert(12);
		e34.insert(1, 11);

		E3 e3 = SelectById.queryId(E3.class, 1).selectOne(context);
		E4 e4_1 = SelectById.queryId(E4.class, 11).selectOne(context);
		E4 e4_2 = SelectById.queryId(E4.class, 12).selectOne(context);

		doAnswer((Answer<Object>) invocation -> {

			assertSame(context, invocation.getArguments()[0]);

			ChangeMap changes = (ChangeMap) invocation.getArguments()[1];
			assertNotNull(changes);
			assertEquals(3, changes.getUniqueChanges().size());

			ObjectChange e3c = changes.getChanges().get(ObjectId.of("E3", E3.ID_PK_COLUMN, 1));
			assertNotNull(e3c);
			assertEquals(ObjectChangeType.UPDATE, e3c.getType());
			assertEquals(0, e3c.getAttributeChanges().size());
			assertEquals(1, e3c.getToManyRelationshipChanges().size());

			ToManyRelationshipChange e3c1 = e3c.getToManyRelationshipChanges().get(E3.E4S.getName());
			assertNotNull(e3c1);

			assertEquals(1, e3c1.getAdded().size());
			assertTrue(e3c1.getAdded().contains(e4_2.getObjectId()));

			assertEquals(1, e3c1.getRemoved().size());
			assertTrue(e3c1.getRemoved().contains(e4_1.getObjectId()));

			ObjectChange e41c = changes.getChanges().get(ObjectId.of("E4", E4.ID_PK_COLUMN, 11));
			assertNotNull(e41c);
			assertEquals(ObjectChangeType.UPDATE, e41c.getType());
			assertEquals(0, e41c.getAttributeChanges().size());
			assertEquals(1, e41c.getToManyRelationshipChanges().size());

			ToManyRelationshipChange e41c1 = e41c.getToManyRelationshipChanges().get(E4.E3S.getName());
			assertNotNull(e41c);

			assertEquals(0, e41c1.getAdded().size());

			assertEquals(1, e41c1.getRemoved().size());
			assertTrue(e41c1.getRemoved().contains(e3.getObjectId()));

			ObjectChange e42c = changes.getChanges().get(ObjectId.of("E4", E4.ID_PK_COLUMN, 12));
			assertNotNull(e42c);
			assertEquals(ObjectChangeType.UPDATE, e42c.getType());
			assertEquals(0, e42c.getAttributeChanges().size());
			assertEquals(1, e42c.getToManyRelationshipChanges().size());

			ToManyRelationshipChange e42c1 = e42c.getToManyRelationshipChanges().get(E4.E3S.getName());
			assertNotNull(e42c);

			assertEquals(0, e42c1.getRemoved().size());

			assertEquals(1, e42c1.getAdded().size());
			assertTrue(e42c1.getAdded().contains(e3.getObjectId()));

			return null;
		}).when(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));

		e3.removeFromE4s(e4_1);
		e3.addToE4s(e4_2);

		context.commitChanges();

		verify(mockListener).onPostCommit(any(ObjectContext.class), any(ChangeMap.class));
	}

}
