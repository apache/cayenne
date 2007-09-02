package org.apache.cayenne.jpa.itest.ch6;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch6.entity.UndeclaredEntity1;

public class _6_1_2_6_PersistentClassSourcesTest extends EntityManagerCase {

	public void testLoadImplicitFromUnitRoot() throws Exception {

		getDbHelper().deleteAll("UndeclaredEntity1");

		UndeclaredEntity1 e = new UndeclaredEntity1();
		getEntityManager().persist(e);
		getEntityManager().getTransaction().commit();

		assertEquals(1, getDbHelper().getRowCount("UndeclaredEntity1"));
	}
}
