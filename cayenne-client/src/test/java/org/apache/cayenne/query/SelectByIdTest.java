package org.apache.cayenne.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

public class SelectByIdTest {

	@Test
	public void testSerializabilityWithHessian() throws Exception {
		SelectById<Artist> o = SelectById.query(Artist.class, 5);
		Object clone = HessianUtil.cloneViaClientServerSerialization(o, new EntityResolver());

		assertTrue(clone instanceof SelectById);
		SelectById<?> c1 = (SelectById<?>) clone;

		assertNotSame(o, c1);
		assertEquals(o.entityType, c1.entityType);
		assertEquals(o.singleId, c1.singleId);
	}
}
