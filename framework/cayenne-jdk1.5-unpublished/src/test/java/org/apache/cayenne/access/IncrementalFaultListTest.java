package org.apache.cayenne.access;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class IncrementalFaultListTest extends ServerCase {

	@Inject
	protected DataContext context;

	public void testSerialization() throws Exception {
		SelectQuery query = new SelectQuery(Artist.class);
		query.setPageSize(10);
		
		IncrementalFaultList<Artist> i1 = new IncrementalFaultList<Artist>(context, query, 10);
		IncrementalFaultList<Artist> i2 = Util.cloneViaSerialization(i1);
		
		assertNotNull(i2);
		assertEquals(i1.getMaxFetchSize(), i2.getMaxFetchSize());
		assertEquals(i1.getClass(), i2.getClass());
	}

}
