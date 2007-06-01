/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.testdo.inherit.AbstractPerson;
import org.objectstyle.cayenne.testdo.inherit.Address;
import org.objectstyle.cayenne.testdo.inherit.ClientCompany;
import org.objectstyle.cayenne.testdo.inherit.CustomerRepresentative;
import org.objectstyle.cayenne.testdo.inherit.Department;
import org.objectstyle.cayenne.testdo.inherit.Employee;
import org.objectstyle.cayenne.testdo.inherit.Manager;
import org.objectstyle.cayenne.unit.PeopleTestCase;

/**
 * Testing Cayenne behavior with DataObject inheritance 
 * hierarchies.
 * 
 * @author Andrei Adamchik
 */
public class InheritanceTst extends PeopleTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testSave() throws Exception {
        ClientCompany company =
            (ClientCompany) context.createAndRegisterNewObject(ClientCompany.class);
        company.setName("Boeing");

        CustomerRepresentative rep =
            (CustomerRepresentative) context.createAndRegisterNewObject(
                CustomerRepresentative.class);
        rep.setName("Joe Schmoe");
        rep.setToClientCompany(company);
        rep.setPersonType("C");

        Employee employee = (Employee) context.createAndRegisterNewObject(Employee.class);
        employee.setName("Our Joe Schmoe");
        employee.setPersonType("E");

        context.commitChanges();

        context = createDataContextWithLocalCache();
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
        assertSame(Manager.class, address.getToEmployee().getClass());
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

    public void testSelectNoInheritanceResolving() throws Exception {
        createTestData("testSelect");

        // select Abstract Ppl
        SelectQuery query = new SelectQuery(AbstractPerson.class);
        query.setResolvingInherited(false);
        assertFalse(query.isResolvingInherited());
        List abstractPpl = context.performQuery(query);
        assertEquals(6, abstractPpl.size());
        assertEquals(0, countObjectOfClass(abstractPpl, CustomerRepresentative.class));
        assertEquals(0, countObjectOfClass(abstractPpl, Employee.class));
        assertEquals(0, countObjectOfClass(abstractPpl, Manager.class));
    }

    public void testSelectInheritanceResolving() throws Exception {
        createTestData("testSelect");

        // select Abstract Ppl
        SelectQuery query = new SelectQuery(AbstractPerson.class);
        query.setResolvingInherited(true);
        assertTrue(query.isResolvingInherited());
        List abstractPpl = context.performQuery(query);
        assertEquals(6, abstractPpl.size());

        assertEquals(1, countObjectOfClass(abstractPpl, CustomerRepresentative.class));
        assertEquals(5, countObjectOfClass(abstractPpl, Employee.class));
        assertEquals(2, countObjectOfClass(abstractPpl, Manager.class));
    }

    /**
     * Returns a number of objects of a particular class and subclasses
     * in the list.
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
