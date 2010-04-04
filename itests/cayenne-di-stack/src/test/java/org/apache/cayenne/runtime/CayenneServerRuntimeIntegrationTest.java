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
package org.apache.cayenne.runtime;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.CayenneServerRuntimeCase;
import org.apache.cayenne.configuration.server.RuntimeName;
import org.apache.cayenne.itest.di_stack.Table1;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.DBHelper;

public class CayenneServerRuntimeIntegrationTest extends
		CayenneServerRuntimeCase {

	@Override
	protected RuntimeName getRuntimeName() {
		return RuntimeName.DEFAULT;
	}

	public void testGetDomain_singleton() {

		DataDomain domain1 = runtime.getDataDomain();
		assertNotNull(domain1);

		DataDomain domain2 = runtime.getDataDomain();
		assertNotNull(domain2);

		assertSame(domain1, domain2);
	}

	public void testNewContext_notSingleton() {

		ObjectContext context1 = runtime.newContext();
		assertNotNull(context1);

		ObjectContext context2 = runtime.newContext();
		assertNotNull(context2);

		assertNotSame(context1, context2);
	}

	public void testNewContext_separateObjects() throws Exception {
		DBHelper dbUtils = getDbUtils();
		dbUtils.deleteAll("TABLE1");
		dbUtils.insert("TABLE1", new String[] { "ID", "NAME" }, new Object[] {
				1, "Abc" });

		SelectQuery query = new SelectQuery(Table1.class);

		ObjectContext context1 = runtime.newContext();
		ObjectContext context2 = runtime.newContext();

		Table1 o1 = (Table1) Cayenne.objectForQuery(context1, query);
		Table1 o2 = (Table1) Cayenne.objectForQuery(context2, query);

		assertNotNull(o1);
		assertNotNull(o2);
		assertEquals("Abc", o1.getName());
		assertNotSame(o1, o2);
		assertEquals(o1.getObjectId(), o2.getObjectId());

	}
}
