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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
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
import org.apache.cayenne.unit.PeopleCase;

/**
 * Testing Cayenne behavior with DataObject inheritance hierarchies.
 */
public class InheritanceTest extends PeopleCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testRelationshipToAbstractSuper() {
        context
                .performGenericQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (1, 'AA', 'EE')"));

        context.performGenericQuery(new SQLTemplate(
                PersonNotes.class,
                "INSERT INTO PERSON_NOTES (ID, NOTES, PERSON_ID) VALUES (1, 'AA', 1)"));

        PersonNotes note = DataObjectUtils.objectForPK(context, PersonNotes.class, 1);
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

        AbstractPerson person = (AbstractPerson) DataObjectUtils.objectForQuery(
                createDataContext(),
                query);

        assertTrue(person instanceof Employee);

        blockQueries();
        try {
            assertEquals(2, person.getNotes().size());

            String[] names = new String[2];
            names[0] = person.getNotes().get(0).getNotes();
            names[1] = person.getNotes().get(1).getNotes();
            List<String> nameSet = Arrays.asList(names);

            assertTrue(nameSet.contains("AA"));
            assertTrue(nameSet.contains("BB"));
        }
        finally {
            unblockQueries();
        }
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

        AbstractPerson person = (AbstractPerson) DataObjectUtils.objectForQuery(
                createDataContext(),
                query);

        assertTrue(person instanceof Employee);

        blockQueries();
        try {
            assertEquals(2, person.getNotes().size());

            String[] names = new String[2];
            names[0] = person.getNotes().get(0).getNotes();
            names[1] = person.getNotes().get(1).getNotes();
            List<String> nameSet = Arrays.asList(names);

            assertTrue(nameSet.contains("AA"));
            assertTrue(nameSet.contains("BB"));
        }
        finally {
            unblockQueries();
        }
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
        query.addOrdering(PersonNotes.NOTES_PROPERTY, Ordering.ASC);

        List<PersonNotes> notes = createDataContext().performQuery(query);
        assertEquals(2, notes.size());
        PersonNotes note = notes.get(0);

        blockQueries();
        try {
            assertEquals("AA", note.getPerson().getName());
        }
        finally {
            unblockQueries();
        }
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

        PersonNotes note = (PersonNotes) DataObjectUtils.objectForQuery(
                createDataContext(),
                query);

        blockQueries();
        try {
            assertEquals("AA", note.getPerson().getName());
        }
        finally {
            unblockQueries();
        }

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

        context = createDataContextWithDedicatedCache();
        SelectQuery query = new SelectQuery(CustomerRepresentative.class);
        List reps = context.performQuery(query);

        assertEquals(1, reps.size());
        assertEquals(1, countObjectOfClass(reps, CustomerRepresentative.class));
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testEmployeeAddress() throws Exception {
        createTestData("testEmployeeAddress");
        List addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        assertSame(Employee.class, address.getToEmployee().getClass());
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testManagerAddress() throws Exception {
        createTestData("testManagerAddress");
        List addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        Employee e = address.getToEmployee();

        assertSame(Manager.class, e.getClass());
    }

    public void testCAY592() throws Exception {
        createTestData("testManagerAddress");
        List addresses = context.performQuery(new SelectQuery(Address.class));

        assertEquals(1, addresses.size());
        Address address = (Address) addresses.get(0);
        Employee e = address.getToEmployee();

        // CAY-592 - make sure modification of the address in a parallel context
        // doesn't mess up the Manager
        DataContext c2 = context.getParentDataDomain().createDataContext();
        e = (Employee) DataObjectUtils.objectForPK(c2, e.getObjectId());
        address = e.getAddresses().get(0);

        assertSame(e, address.getToEmployee());
        address.setCity("XYZ");
        assertSame(e, address.getToEmployee());
    }

    /**
     * Tests that to-one relationship produces correct subclass.
     */
    public void testRepCompany() throws Exception {
        createTestData("testRepCompany");
        List companies = context.performQuery(new SelectQuery(ClientCompany.class));

        assertEquals(1, companies.size());
        ClientCompany company = (ClientCompany) companies.get(0);
        List reps = company.getRepresentatives();
        assertEquals(1, reps.size());
        assertSame(CustomerRepresentative.class, reps.get(0).getClass());
    }

    /**
     * Tests that to-many relationship produces correct subclasses.
     */
    public void testDepartmentEmployees() throws Exception {
        createTestData("testDepartmentEmployees");
        List departments = context.performQuery(new SelectQuery(Department.class));

        assertEquals(1, departments.size());
        Department dept = (Department) departments.get(0);
        List employees = dept.getEmployees();
        assertEquals(3, employees.size());
        assertEquals(3, countObjectOfClass(employees, Employee.class));
        assertEquals(1, countObjectOfClass(employees, Manager.class));
    }

    public void testSelectInheritanceResolving() throws Exception {
        createTestData("testSelect");

        // select Abstract Ppl
        SelectQuery query = new SelectQuery(AbstractPerson.class);
        List abstractPpl = context.performQuery(query);
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
        assertEquals(1, context
                .getEntityResolver()
                .getObjEntity("DirectToSubEntity")
                .getRelationships()
                .size());

        // Simulate a load from a default configuration.
        context.getEntityResolver().applyObjectLayerDefaults();

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
    protected int countObjectOfClass(List objects, Class aClass) {
        Iterator it = objects.iterator();
        int i = 0;
        while (it.hasNext()) {
            Object next = it.next();

            if (aClass.isAssignableFrom(next.getClass())) {
                i++;
            }
        }
        return i;
    }
}
