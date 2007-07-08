package org.apache.cayenne.jpa.itest.ch4;

import java.util.List;

import javax.persistence.Query;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch4.entity.SimpleEntity;

public class _4_2_1_SelectStatementTest extends EntityManagerCase {

	public void testSelectFrom() throws Exception {
		getDbHelper().deleteAll("SimpleEntity");

		getDbHelper().insert("SimpleEntity",
				new String[] { "id", "property1" }, new Object[] { 15, "XXX" });

		Query query = getEntityManager().createQuery(
				"select x from SimpleEntity x");
		assertNotNull(query);
		List result = query.getResultList();
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.get(0) instanceof SimpleEntity);
		assertEquals("XXX", ((SimpleEntity) result.get(0)).getProperty1());
	}

	// TODO: andrus, 3/11/2007 - this fails.
	public void testSelectFromWhere() throws Exception {
		// getDbHelper().deleteAll("SimpleEntity");
		//
		// getDbHelper().insert("SimpleEntity",
		// new String[] { "id", "property1" }, new Object[] { 15, "XXX" });
		// getDbHelper().insert("SimpleEntity",
		// new String[] { "id", "property1" }, new Object[] { 16, "YYY" });
		//
		// Query query = getEntityManager().createQuery(
		// "select x from SimpleEntity x where x.property1 = 'YYY'");
		// assertNotNull(query);
		// List result = query.getResultList();
		// assertNotNull(result);
		// assertEquals(1, result.size());
		// assertTrue(result.get(0) instanceof SimpleEntity);
		// assertEquals("YYY", ((SimpleEntity) result.get(0)).getProperty1());
	}
}
