package org.apache.cayenne.itest.cpa;

import java.util.List;

import org.apache.cayenne.query.SelectQuery;

public class ObjectContextTest extends CPAContextCase {

	public void testPerformQuery() throws Exception {

		getDbHelper().deleteAll("entity1");
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 1, "X" });
		getDbHelper().insert("entity1", new String[] { "id", "name" },
				new Object[] { 2, "Y" });

		SelectQuery query = new SelectQuery(Entity1.class);
		List results = getContext().performQuery(query);
		assertNotNull(results);
		assertEquals(2, results.size());
	}
}
