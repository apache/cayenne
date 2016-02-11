package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToMany;
import org.apache.cayenne.testdo.relationships_collection_to_many.CollectionToManyTarget;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.RELATIONSHIPS_COLLECTION_TO_MANY_PROJECT)
public class CayenneDataObjectSetToManyCollectionIT extends ServerCase {

	@Inject
	private ObjectContext context;

	@Inject
	private DBHelper dbHelper;

	@Before
	public void setUp() throws Exception {
		TableHelper tCollectionToMany = new TableHelper(dbHelper, "COLLECTION_TO_MANY");
		tCollectionToMany.setColumns("ID");

		TableHelper tCollectionToManyTarget = new TableHelper(dbHelper, "COLLECTION_TO_MANY_TARGET");
		tCollectionToManyTarget.setColumns("ID", "COLLECTION_TO_MANY_ID");

		// single data set for all tests
		tCollectionToMany.insert(1).insert(2);
		tCollectionToManyTarget.insert(1, 1).insert(2, 1).insert(3, 1).insert(4, 2);
	}

	@Test
	public void testReadToMany() throws Exception {

		CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);

		Collection<?> targets = o1.getTargets();

		assertNotNull(targets);
		assertTrue(((ValueHolder) targets).isFault());

		assertEquals(3, targets.size());

		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 1)));
		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 2)));
		assertTrue(targets.contains(Cayenne.objectForPK(o1.getObjectContext(), CollectionToManyTarget.class, 3)));
	}

	/**
	 * Testing if collection type is Collection, everything should work fine without an
	 * runtimexception
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRelationCollectionTypeCollection() throws Exception {
		CollectionToMany o1 = Cayenne.objectForPK(context, CollectionToMany.class, 1);
		assertTrue(o1.readProperty(CollectionToMany.TARGETS_PROPERTY) instanceof Collection);
		boolean catchedSomething = false;
		try {
			o1.setToManyTarget(CollectionToMany.TARGETS_PROPERTY, new ArrayList<CollectionToMany>(0), true);
		} catch (RuntimeException e) {
			catchedSomething = true;
		}
		assertEquals(catchedSomething, false);
		assertEquals(o1.getTargets().size(), 0);
	}
}
