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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.itest.di_stack.Table1;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;

public class ServerRuntimeIntegrationTest extends ServerRuntimeCase {

	@Override
	protected RuntimeName getRuntimeName() {
		return RuntimeName.DEFAULT;
	}

	public void testGetDomain_singleton() {

		DataDomain domain1 = runtime.getDataDomain();
		assertNotNull(domain1);

		EventManager eventManager1 = domain1.getEventManager();
		assertNotNull(eventManager1);

		EntitySorter sorter1 = domain1.getEntitySorter();
		assertNotNull(sorter1);

		DataDomain domain2 = runtime.getDataDomain();
		assertNotNull(domain2);

		EventManager eventManager2 = domain2.getEventManager();
		assertNotNull(eventManager2);

		EntitySorter sorter2 = domain2.getEntitySorter();
		assertNotNull(sorter2);

		assertSame(domain1, domain2);
		assertSame(eventManager1, eventManager2);
		assertSame(sorter1, sorter2);
	}

	public void testContext_notSingleton() {

		ObjectContext context1 = runtime.getContext();
		assertNotNull(context1);
		assertTrue(context1 instanceof DataContext);

		DataDomain domain1 = (DataDomain) context1.getChannel();
		EventManager eventManager1 = domain1.getEventManager();
		EntitySorter sorter1 = domain1.getEntitySorter();

		ObjectContext context2 = runtime.getContext();
		assertNotNull(context2);
		assertTrue(context2 instanceof DataContext);
		DataDomain domain2 = (DataDomain) context2.getChannel();
		EventManager eventManager2 = domain2.getEventManager();
		EntitySorter sorter2 = domain2.getEntitySorter();

		assertNotSame(context1, context2);

		assertSame(domain1, domain2);
		assertSame(eventManager1, eventManager2);
		assertSame(sorter1, sorter2);
	}

	public void testNewContext_separateObjects() throws Exception {
		DBHelper dbUtils = getDbUtils();
		dbUtils.deleteAll("TABLE1");
		dbUtils.insert("TABLE1", new String[] { "ID", "NAME" }, new Object[] {
				1, "Abc" }, null);

		SelectQuery query = new SelectQuery(Table1.class);

		ObjectContext context1 = runtime.getContext();
		ObjectContext context2 = runtime.getContext();

		Table1 o1 = (Table1) Cayenne.objectForQuery(context1, query);
		Table1 o2 = (Table1) Cayenne.objectForQuery(context2, query);

		assertNotNull(o1);
		assertNotNull(o2);
		assertEquals("Abc", o1.getName());
		assertNotSame(o1, o2);
		assertEquals(o1.getObjectId(), o2.getObjectId());
	}

	public void testShutdown() throws Exception {

		// create a context and save some objects to warm up the stack...
		ObjectContext context1 = runtime.getContext();

		Table1 t1 = context1.newObject(Table1.class);
		t1.setName("XmKK");
		context1.commitChanges();

		// ensure that some of the services that require shutdown are
		// instantiated and check their state before and after
		EventManager em = runtime.getInjector().getInstance(EventManager.class);
		assertNotNull(em);
		assertTrue(em instanceof DefaultEventManager);
		assertFalse(((DefaultEventManager) em).isStopped());

		runtime.getInjector().shutdown();

		assertTrue(((DefaultEventManager) em).isStopped());
	}
}
