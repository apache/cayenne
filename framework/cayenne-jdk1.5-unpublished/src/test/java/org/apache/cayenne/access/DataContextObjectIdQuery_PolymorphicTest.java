package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

import java.sql.SQLException;
import java.sql.Types;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class DataContextObjectIdQuery_PolymorphicTest extends ServerCase {

	@Inject
	private DataContext context1;

	@Inject
	private DataContext context2;

	@Inject
	private DataChannelInterceptor queryInterceptor;

	@Inject
    protected DBHelper dbHelper;

	private TableHelper tPerson;

	@Override
    protected void setUpAfterInjection() throws Exception {
    	dbHelper.deleteAll("PERSON");
		tPerson = new TableHelper(dbHelper, "PERSON").setColumns("PERSON_ID", "NAME", "PERSON_TYPE")
				.setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);
	}

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

	public void testPolymorphicSharedCache_AfterCayenneInsert() throws SQLException {


		// see CAY-2101... we are trying to get a snapshot from a new object in the shared cache, and then read this
		// object via a relationship, so that shared cache is consulted
		Employee e = context1.newObject(Employee.class);
		e.setName("E1");
		e.setSalary(1234.01f);

		context1.commitChanges();


		final ObjectIdQuery q1 = new ObjectIdQuery(
				new ObjectId("AbstractPerson", "PERSON_ID", Cayenne.intPKForObject(e)),
				false,
				ObjectIdQuery.CACHE);


		queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {
			
			public void execute() {
				// use different context to ensure we hit shared cache
				AbstractPerson ap1 = (AbstractPerson) Cayenne.objectForQuery(context2, q1);
				assertTrue(ap1 instanceof Employee);
			}
		});
	}
}
