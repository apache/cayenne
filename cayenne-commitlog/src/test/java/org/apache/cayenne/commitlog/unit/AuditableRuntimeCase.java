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
package org.apache.cayenne.commitlog.unit;

import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.After;
import org.junit.Before;

/**
 * A superclass of integration tests for cayenne-lifecycle.
 */
public abstract class AuditableRuntimeCase {

	protected CayenneRuntime runtime;

	protected TableHelper auditable1;
	protected TableHelper auditableChild1;
	protected TableHelper auditableChild1x;

	protected TableHelper auditable2;
	protected TableHelper auditableChild3;

	protected TableHelper auditable3;
	protected TableHelper auditable4;

	protected TableHelper auditable5;
	protected TableHelper auditableChild5;

	protected TableHelper auditLog;

	@Before
	public void startCayenne() throws Exception {
		this.runtime = configureCayenne().build();

		DBHelper dbHelper = new DBHelper(runtime.getDataSource());

		this.auditLog = new TableHelper(dbHelper, "AUDIT_LOG").setColumns("ID", "LOG");

		this.auditable1 = new TableHelper(dbHelper, "AUDITABLE1").setColumns("ID", "CHAR_PROPERTY1");

		this.auditableChild1 = new TableHelper(dbHelper, "AUDITABLE_CHILD1").setColumns("ID", "AUDITABLE1_ID",
				"CHAR_PROPERTY1");
		this.auditableChild1x = new TableHelper(dbHelper, "AUDITABLE_CHILD1X").setColumns("ID", "AUDITABLE1_ID",
				"CHAR_PROPERTY1");

		this.auditable2 = new TableHelper(dbHelper, "AUDITABLE2").setColumns("ID", "CHAR_PROPERTY1", "CHAR_PROPERTY2");

		this.auditableChild3 = new TableHelper(dbHelper, "AUDITABLE_CHILD3").setColumns("ID", "AUDITABLE2_ID",
				"CHAR_PROPERTY1", "CHAR_PROPERTY2");

		this.auditable3 = new TableHelper(dbHelper, "AUDITABLE3").setColumns("ID", "CHAR_PROPERTY1", "CHAR_PROPERTY2");
		this.auditable4 = new TableHelper(dbHelper, "AUDITABLE4").setColumns("ID", "CHAR_PROPERTY1", "CHAR_PROPERTY2",
				"AUDITABLE3_ID");

		this.auditable5 = new TableHelper(dbHelper, "AUDITABLE5").setColumns("ID", "CHAR_PROPERTY1");
		this.auditableChild5 = new TableHelper(dbHelper, "AUDITABLE_CHILD5").setColumns("ID", "AUDITABLE5_ID",
				"CHAR_PROPERTY1");

		this.auditableChild1.deleteAll();
		this.auditableChild1x.deleteAll();
		this.auditable1.deleteAll();
		this.auditableChild3.deleteAll();
		this.auditable2.deleteAll();
		this.auditable4.deleteAll();
		this.auditable3.deleteAll();
		this.auditableChild5.deleteAll();
		this.auditable5.deleteAll();

		this.auditLog.deleteAll();
	}

	protected CayenneRuntimeBuilder configureCayenne() {
		return CayenneRuntime.builder().addConfig("cayenne-lifecycle.xml");
	}

	@After
	public void shutdownCayenne() {
		if (runtime != null) {
			runtime.shutdown();
		}
	}
}
