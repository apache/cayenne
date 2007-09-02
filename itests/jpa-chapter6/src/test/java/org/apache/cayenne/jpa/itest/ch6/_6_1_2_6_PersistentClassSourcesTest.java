package org.apache.cayenne.jpa.itest.ch6;

import org.apache.cayenne.itest.jpa.EntityManagerCase;

public class _6_1_2_6_PersistentClassSourcesTest extends EntityManagerCase {

	public void testLoadUndeclared() throws Exception {
		getDbHelper().deleteAll("UndeclaredEntity1");

//		UndeclaredEntity1 e = new UndeclaredEntity1();
//		getEntityManager().persist(e);
//		getEntityManager().getTransaction().commit();
//
//		assertEquals(1, getDbHelper().getRowCount("UndeclaredEntity1"));
	}
}
