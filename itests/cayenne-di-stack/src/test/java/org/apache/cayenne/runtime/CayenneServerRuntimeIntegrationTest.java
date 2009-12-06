package org.apache.cayenne.runtime;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.itest.ItestDBUtils;
import org.apache.cayenne.itest.di_stack.Table1;
import org.apache.cayenne.query.SelectQuery;

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
		ItestDBUtils dbUtils = getDbUtils();
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
