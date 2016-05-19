package org.apache.cayenne.access;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.Types;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_people.AbstractPerson;
import org.apache.cayenne.testdo.inheritance_people.Manager;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.PeopleProjectCase;
import org.junit.Before;
import org.junit.Test;

public class DataContextObjectIdQuery_PolymorphicIT extends PeopleProjectCase {

	@Inject
	private DataContext context1;

	@Inject
	private DataContext context2;

	@Inject
	private DataChannelInterceptor queryInterceptor;

	private TableHelper tPerson;

	@Before
	public void before() {
		tPerson = new TableHelper(dbHelper, "PERSON").setColumns("PERSON_ID", "NAME", "PERSON_TYPE")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);
	}

	@Test
	public void testPolymorphicSharedCache() throws SQLException {

		tPerson.insert(1, "P1", "EM");

		final ObjectIdQuery q1 = new ObjectIdQuery(new ObjectId("AbstractPerson", "PERSON_ID", 1), false,
				ObjectIdQuery.CACHE);

		AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
		assertTrue(ap1 instanceof Manager);

		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				// use different context to ensure we hit shared cache
				AbstractPerson ap2 = (AbstractPerson) Cayenne.objectForQuery(context2, q1);
				assertTrue(ap2 instanceof Manager);
			}
		});
	}

	@Test
	public void testPolymorphicLocalCache() throws SQLException {

		tPerson.insert(1, "P1", "EM");

		final ObjectIdQuery q1 = new ObjectIdQuery(new ObjectId("AbstractPerson", "PERSON_ID", 1), false,
				ObjectIdQuery.CACHE);

		AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
		assertTrue(ap1 instanceof Manager);

		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

			@Override
			public void execute() {
				// use same context to ensure we hit local cache
				// note that this does not guarantee test correctness. If local
				// cache polymorphic ID lookup is broken, shared cache will pick
				// it up
				AbstractPerson ap2 = (AbstractPerson) Cayenne.objectForQuery(context1, q1);
				assertTrue(ap2 instanceof Manager);
			}
		});
	}
}
