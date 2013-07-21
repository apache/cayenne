/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.Address;
import org.apache.cayenne.testdo.inherit.BaseEntity;
import org.apache.cayenne.testdo.inherit.ClientCompany;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.testdo.inherit.Department;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.testdo.inherit.PersonNotes;
import org.apache.cayenne.testdo.inherit.RelatedEntity;
import org.apache.cayenne.testdo.inherit.SubEntity;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class SingleTableInheritanceTest extends ServerCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private DataContext context;

    @Inject
    private DataContext context2;

    @Inject
    private DataChannelInterceptor queryBlocker;

    private TableHelper tPerson;
    private TableHelper tAddress;
    private TableHelper tClientCompany;
    private TableHelper tDepartment;

    @Override
    protected void setUpAfterInjection() throws Exception {
        tPerson = new TableHelper(dbHelper, "PERSON");
        tPerson.setColumns(
                "PERSON_ID",
                "NAME",
                "PERSON_TYPE",
                "SALARY",
                "CLIENT_COMPANY_ID",
                "DEPARTMENT_ID").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.CHAR,
                Types.FLOAT,
                Types.INTEGER,
                Types.INTEGER);

        tAddress = new TableHelper(dbHelper, "ADDRESS");
        tAddress.setColumns("ADDRESS_ID", "CITY", "PERSON_ID");

        tClientCompany = new TableHelper(dbHelper, "CLIENT_COMPANY");
        tClientCompany.setColumns("CLIENT_COMPANY_ID", "NAME");

        tDepartment = new TableHelper(dbHelper, "DEPARTMENT");
        tDepartment.setColumns("DEPARTMENT_ID", "NAME");

        // manually break circular deps
        tPerson.update().set("DEPARTMENT_ID", null, Types.INTEGER).execute();

        dbHelper.deleteAll("ADDRESS");
        dbHelper.deleteAll("DEPARTMENT");
        dbHelper.deleteAll("PERSON_NOTES");
        dbHelper.deleteAll("PERSON");
        dbHelper.deleteAll("CLIENT_COMPANY");
    }

    private void create2PersonDataSet() throws Exception {
        tPerson.insert(1, "E1", "EE", null, null, null);
        tPerson.insert(2, "E2", "EM", null, null, null);
    }

    private void create5PersonDataSet() throws Exception {
        tPerson.insert(1, "E1", "EE", null, null, null);
        tPerson.insert(2, "E2", "EM", null, null, null);
        tPerson.insert(3, "E3", "EE", null, null, null);
        tPerson.insert(4, "E4", "EM", null, null, null);
        tPerson.insert(5, "E5", "EE", null, null, null);
    }

    private void createSelectDataSet() throws Exception {
        tPerson.insert(1, "e1", "EE", 20000, null, null);
        tPerson.insert(2, "e2", "EE", 25000, null, null);
        tPerson.insert(3, "e3", "EE", 28000, null, null);
        tPerson.insert(4, "m1", "EM", 30000, null, null);
        tPerson.insert(5, "m2", "EM", 40000, null, null);

        tClientCompany.insert(1, "Citibank");
        tPerson.insert(6, "c1", "C", null, 1, null);
    }

    private void createEmployeeAddressDataSet() throws Exception {
        tPerson.insert(1, "e1", "EE", 20000, null, null);
        tAddress.insert(1, "New York", 1);
    }

    private void createManagerAddressDataSet() throws Exception {
        tPerson.insert(4, "m1", "EM", 30000, null, null);
        tAddress.insert(1, "New York", 4);
    }

    private void createRepCompanyDataSet() throws Exception {
        tClientCompany.insert(1, "Citibank");
        tPerson.insert(6, "c1", "C", null, 1, null);
    }

    private void createDepartmentEmployeesDataSet() throws Exception {
        tDepartment.insert(1, "Accounting");

        tPerson.insert(7, "John", "EE", 25000, null, 1);
        tPerson.insert(8, "Susan", "EE", 50000, null, 1);
        tPerson.insert(9, "Kelly", "EM", 100000, null, 1);
    }

    public void testMatchingOnSuperAttributes() throws Exception {
        create2PersonDataSet();

        // fetch on leaf, but match on a super attribute
        SelectQuery select = new SelectQuery(Manager.class);
        select.andQualifier(ExpressionFactory
                .matchExp(AbstractPerson.NAME_PROPERTY, "E2"));

        List<Manager> results = context.performQuery(select);
        assertEquals(1, results.size());
        assertEquals("E2", results.get(0).getName());
    }

    public void testMatchingOnSuperAttributesWithPrefetch() throws Exception {
        create2PersonDataSet();

        // fetch on leaf, but match on a super attribute
        SelectQuery select = new SelectQuery(Employee.class);
        select.addPrefetch(Employee.TO_DEPARTMENT_PROPERTY);
        select.andQualifier(ExpressionFactory
                .matchExp(AbstractPerson.NAME_PROPERTY, "E2"));

        List<Manager> results = context.performQuery(select);
        assertEquals(1, results.size());
        assertEquals("E2", results.get(0).getName());
    }

    public void testPaginatedQueries() throws Exception {

        create5PersonDataSet();

        SelectQuery select = new SelectQuery(AbstractPerson.class);
        select.addOrdering(
                "db:" + AbstractPerson.PERSON_ID_PK_COLUMN,
                SortOrder.ASCENDING);
        select.setPageSize(3);

        List<AbstractPerson> results = context.performQuery(select);
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof Employee);

        // this is where things would blow up per CAY-1142
        assertTrue(results.get(1) instanceof Manager);

        assertTrue(results.get(3) instanceof Manager);
        assertTrue(results.get(4) instanceof Employee);
    }

    public void testRelationshipToAbstractSuper() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (1, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (1, 'AA', 1)"));

        PersonNotes note = Cayenne.objectForPK(context, PersonNotes.class, 1);
        assertNotNull(note);
        assertNotNull(note.getPerson());
        assertTrue(note.getPerson() instanceof Employee);
    }

    public void testRelationshipAbstractFromSuperPrefetchingJoint() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (3, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (3, 'AA', 3)"));
        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (4, 'BB', 3)"));

        SelectQuery query = new SelectQuery(AbstractPerson.class);
        query.addPrefetch(AbstractPerson.NOTES_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final AbstractPerson person = (AbstractPerson) Cayenne.objectForQuery(
                context,
                query);

        assertTrue(person instanceof Employee);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, person.getNotes().size());

                String[] names = new String[2];
                names[0] = person.getNotes().get(0).getNotes();
                names[1] = person.getNotes().get(1).getNotes();
                List<String> nameSet = Arrays.asList(names);

                assertTrue(nameSet.contains("AA"));
                assertTrue(nameSet.contains("BB"));
            }
        });
    }

    public void testRelationshipAbstractFromSuperPrefetchingDisjoint() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (3, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (3, 'AA', 3)"));
        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (4, 'BB', 3)"));

        SelectQuery query = new SelectQuery(AbstractPerson.class);
        query.addPrefetch(AbstractPerson.NOTES_PROPERTY);

        final AbstractPerson person = (AbstractPerson) Cayenne.objectForQuery(
                context,
                query);
        assertTrue(person instanceof Employee);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals(2, person.getNotes().size());

                String[] names = new String[2];
                names[0] = person.getNotes().get(0).getNotes();
                names[1] = person.getNotes().get(1).getNotes();
                List<String> nameSet = Arrays.asList(names);

                assertTrue(nameSet.contains("AA"));
                assertTrue(nameSet.contains("BB"));
            }
        });
    }

    public void testRelationshipAbstractToSuperPrefetchingDisjoint() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (2, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (2, 'AA', 2)"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (3, 'BB', 2)"));

        SelectQuery query = new SelectQuery(PersonNotes.class);
        query.addPrefetch(PersonNotes.PERSON_PROPERTY);
        query.addOrdering(PersonNotes.NOTES_PROPERTY, SortOrder.ASCENDING);

        List<PersonNotes> notes = context.performQuery(query);
        assertEquals(2, notes.size());
        final PersonNotes note = notes.get(0);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals("AA", note.getPerson().getName());
            }
        });
    }

    public void testRelationshipAbstractToSuperPrefetchingJoint() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (3, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (3, 'AA', 3)"));

        SelectQuery query = new SelectQuery(PersonNotes.class);
        query.addPrefetch(PersonNotes.PERSON_PROPERTY).setSemantics(
                PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);

        final PersonNotes note = (PersonNotes) Cayenne.objectForQuery(context, query);

        queryBlocker.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                assertEquals("AA", note.getPerson().getName());
            }
        });

    }

    public void testSave() throws Exception {
        ClientCompany company = context.newObject(ClientCompany.class);
        company.setName("Boeing");

        CustomerRepresentative rep = context.newObject(CustomerRepresentative.class);
        rep.setName("Joe Schmoe");
        rep.setToClientCompany(company);
        rep.setPersonType("C");

        Employee employee = context.newObject(Employee.class);
        employee.setName("Our Joe Schmoe");
        employee.setPersonType("E");

        context.commitChanges();
        context.invalidateObjects(company, rep, employee);

        SelectQuery query = new SelectQuery(CustomerRepresentative.class);
        List<?> reps = context2.performQuery(query);

        assertEquals(1, reps.size());
        assertEquals(1, countObjectOfClass(reps, CustomerRepresentative.class));
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testEmployeeAddress() throws Exception {
        createEmployeeAddressDataSet();

        List<?> addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        assertSame(Employee.class, address.getToEmployee().getClass());
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testManagerAddress() throws Exception {
        createManagerAddressDataSet();

        List<?> addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        Employee e = address.getToEmployee();

        assertSame(Manager.class, e.getClass());
    }

    public void testCAY592() throws Exception {
        createManagerAddressDataSet();

        List<?> addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        Employee e = address.getToEmployee();

        // CAY-592 - make sure modification of the address in a parallel context
        // doesn't mess up the Manager

        e = (Employee) Cayenne.objectForPK(context2, e.getObjectId());
        address = e.getAddresses().get(0);

        assertSame(e, address.getToEmployee());
        address.setCity("XYZ");
        assertSame(e, address.getToEmployee());
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testRepCompany() throws Exception {
        createRepCompanyDataSet();

        List<?> companies = context.performQuery(new SelectQuery(ClientCompany.class));

        assertEquals(1, companies.size());
        ClientCompany company = (ClientCompany) companies.get(0);
        List<?> reps = company.getRepresentatives();
        assertEquals(1, reps.size());
        assertSame(CustomerRepresentative.class, reps.get(0).getClass());
    }

    /**
     * Tests that to-many relationship produces correct subclasses.
     */
    public void testDepartmentEmployees() throws Exception {
        createDepartmentEmployeesDataSet();

        List<?> departments = context.performQuery(new SelectQuery(Department.class));

        assertEquals(1, departments.size());
        Department dept = (Department) departments.get(0);
        List<?> employees = dept.getEmployees();
        assertEquals(3, employees.size());
        assertEquals(3, countObjectOfClass(employees, Employee.class));
        assertEquals(1, countObjectOfClass(employees, Manager.class));
    }

    public void testSelectInheritanceResolving() throws Exception {
        createSelectDataSet();

        SelectQuery query = new SelectQuery(AbstractPerson.class);
        List<?> abstractPpl = context.performQuery(query);
        assertEquals(6, abstractPpl.size());

        assertEquals(1, countObjectOfClass(abstractPpl, CustomerRepresentative.class));
        assertEquals(5, countObjectOfClass(abstractPpl, Employee.class));
        assertEquals(2, countObjectOfClass(abstractPpl, Manager.class));
    }

    /**
     * Test for CAY-1008: Reverse relationships may not be correctly set if inheritance is
     * used.
     */
    public void testCAY1008() {
        RelatedEntity related = context.newObject(RelatedEntity.class);

        BaseEntity base = context.newObject(BaseEntity.class);
        base.setToRelatedEntity(related);

        assertEquals(1, related.getBaseEntities().size());
        assertEquals(0, related.getSubEntities().size());

        SubEntity sub = context.newObject(SubEntity.class);
        sub.setToRelatedEntity(related);

        assertEquals(2, related.getBaseEntities().size());

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, related.getSubEntities().size());
    }

    /**
     * Test for CAY-1009: Bogus runtime relationships can mess up commit.
     */
    public void testCAY1009() {

        // We should have only one relationship. DirectToSubEntity -> SubEntity.

        // this fails as a result of 'EntityResolver().applyObjectLayerDefaults()'
        // creating incorrect relationships
        // assertEquals(1, context
        // .getEntityResolver()
        // .getObjEntity("DirectToSubEntity")
        // .getRelationships()
        // .size());

        // We should still just have the one mapped relationship, but we in fact now have
        // two:
        // DirectToSubEntity -> BaseEntity and DirectToSubEntity -> SubEntity.

        // TODO: andrus 2008/03/28 - this fails...
        // assertEquals(1, context.getEntityResolver().getObjEntity("DirectToSubEntity")
        // .getRelationships().size());
        //
        // DirectToSubEntity direct = context.newObject(DirectToSubEntity.class);
        //
        // SubEntity sub = context.newObject(SubEntity.class);
        // sub.setToDirectToSubEntity(direct);
        //
        // assertEquals(1, direct.getSubEntities().size());
        //
        // context.deleteObject(sub);
        // assertEquals(0, direct.getSubEntities().size());
    }

    /**
     * Returns a number of objects of a particular class and subclasses in the list.
     */
    private int countObjectOfClass(List<?> objects, Class<?> aClass) {
        int i = 0;

        for (Object next : objects) {
            if (aClass.isAssignableFrom(next.getClass())) {
                i++;
            }
        }
        return i;
    }
}
