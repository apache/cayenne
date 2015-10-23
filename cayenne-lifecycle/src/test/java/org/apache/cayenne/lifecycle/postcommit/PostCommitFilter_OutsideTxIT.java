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
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.lifecycle.changemap.ChangeMap;
import org.apache.cayenne.lifecycle.changemap.ObjectChange;
import org.apache.cayenne.lifecycle.db.AuditLog;
import org.apache.cayenne.lifecycle.db.Auditable2;
import org.apache.cayenne.lifecycle.unit.AuditableServerCase;
import org.apache.cayenne.tx.BaseTransaction;
import org.junit.Before;
import org.junit.Test;

public class PostCommitFilter_OutsideTxIT extends AuditableServerCase {

	protected ObjectContext context;
	protected PostCommitListener listener;

	@Override
	protected ServerRuntimeBuilder configureCayenne() {
		this.listener = new PostCommitListener() {

			@Override
			public void onPostCommit(ObjectContext originatingContext, ChangeMap changes) {

				// assert we are inside transaction
				assertNull(BaseTransaction.getThreadTransaction());

				for (ObjectChange c : changes.getUniqueChanges()) {
					AuditLog log = runtime.newContext().newObject(AuditLog.class);
					log.setLog("DONE: " + c.getPostCommitId());
					log.getObjectContext().commitChanges();
				}
			}
		};
		return super.configureCayenne().addModule(
				PostCommitModuleBuilder.builder().auditableEntitiesOnly().excludeFromTransaction().listener(listener)
						.build());
	}

	@Before
	public void before() {
		this.context = runtime.newContext();
	}

	@Test
	public void testCommitLog() throws SQLException {
		Auditable2 a1 = context.newObject(Auditable2.class);
		a1.setCharProperty1("yy");
		a1.setCharProperty2("zz");

		Auditable2 a2 = context.newObject(Auditable2.class);
		a2.setCharProperty1("yy");
		a2.setCharProperty2("zz");
		context.commitChanges();

		List<Object[]> logs = auditLog.selectAll();
		assertEquals(2, logs.size());
	}

}
