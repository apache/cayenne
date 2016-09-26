package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.PersonNotes;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.PeopleProjectCase;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertTrue;

public class DataContextRelationshipQuery_PolymorphicIT extends PeopleProjectCase {

    @Inject
    private DataContext context1;

    @Inject
    private DataContext context2;

    @Inject
    private DataChannelInterceptor queryInterceptor;

    private TableHelper tPerson;
    private TableHelper tPersonNotes;

    @Before
    public void before() {
        tPerson = new TableHelper(dbHelper, "PERSON").setColumns("PERSON_ID", "NAME", "PERSON_TYPE")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.CHAR);

        tPersonNotes = new TableHelper(dbHelper, "PERSON_NOTES").setColumns("ID", "PERSON_ID", "NOTES");
    }

    @Test
    public void testPolymorphicSharedCache() throws SQLException {


        // see CAY-2101... we are trying to get a snapshot from a new object in the shared cache, and then read this
        // object via a relationship, so that shared cache is consulted
        Employee e = context1.newObject(Employee.class);
        e.setName("E1");
        e.setSalary(1234.01f);
        PersonNotes n = context1.newObject(PersonNotes.class);
        n.setNotes("N1");
        n.setPerson(e);

        context1.commitChanges();

        // use different context to ensure we hit shared cache for relationship resolving
        final PersonNotes nPeer = Cayenne.objectForPK(context2, PersonNotes.class, Cayenne.intPKForObject(n));

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            @Override
            public void execute() {
                assertTrue(nPeer.getPerson() instanceof Employee);
            }
        });
    }
}
