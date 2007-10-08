/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.util;

import java.util.Iterator;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.testdo.inherit.Department;
import org.objectstyle.cayenne.testdo.inherit.Employee;
import org.objectstyle.cayenne.testdo.inherit.Manager;
import org.objectstyle.cayenne.unit.PeopleTestCase;
import org.objectstyle.cayenne.util.DeepMergeOperation;

public class DeepMergeOperationInheritanceTst extends PeopleTestCase {

    public void testDeepMergeExistingSubclass() {

        ClassDescriptor d = getDomain().getEntityResolver().getClassDescriptor(
                "Department");

        DataContext context = createDataContext();
        DataContext context1 = createDataContext();

        Department d1 = (Department) context.createAndRegisterNewObject(Department.class);
        d1.setName("D1");

        // need to do double commit as Ashwood sorter blows on Employees/Departments
        // ordering...
        context.commitChanges();

        Employee e1 = (Employee) context.createAndRegisterNewObject(Employee.class);
        e1.setName("E1");
        e1.setPersonType("EE");
        d1.addToEmployees(e1);

        Manager e2 = (Manager) context.createAndRegisterNewObject(Manager.class);
        e2.setName("E2");
        e2.setPersonType("EM");
        d1.addToEmployees(e2);

        context.commitChanges();

        // need to make sure source relationship is resolved as a result of some Ashwood
        // strangeness...
        d1.getEmployees().size();

        // resolve Employees
        context1.performQuery(new SelectQuery(Employee.class));

        DeepMergeOperation op = new DeepMergeOperation(context1);
        
        blockQueries();
        try {
            Department d2 = (Department) op.merge(d1, d);
            assertNotNull(d2);
            assertEquals(PersistenceState.COMMITTED, d2.getPersistenceState());

            Iterator it = d2.getEmployees().iterator();
            while (it.hasNext()) {
                Employee ex = (Employee) it.next();
                if ("E2".equals(ex.getName())) {
                    assertTrue(ex instanceof Manager);
                }
                else {
                    assertFalse(ex instanceof Manager);
                }
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testDeepMergeNonExistentSubclass() {

        ClassDescriptor d = getDomain().getEntityResolver().getClassDescriptor(
                "Department");
        DataContext context = createDataContext();
        DataContext context1 = createDataContext();

        Department d1 = (Department) context.createAndRegisterNewObject(Department.class);
        d1.setName("D1");

        // need to do double commit as Ashwood sorter blows on Employees/Departments
        // ordering...
        context.commitChanges();

        Employee e1 = (Employee) context.createAndRegisterNewObject(Employee.class);
        e1.setName("E1");
        e1.setPersonType("EE");
        d1.addToEmployees(e1);

        Manager e2 = (Manager) context.createAndRegisterNewObject(Manager.class);
        e2.setName("E2");
        e2.setPersonType("EM");
        d1.addToEmployees(e2);

        context.commitChanges();

        // need to make sure source relationship is resolved as a result of some Ashwood
        // strangeness...
        d1.getEmployees().size();
        DeepMergeOperation op = new DeepMergeOperation(context1);
        
        blockQueries();
        try {
            Department d2 = (Department) op.merge(d1, d);
            assertNotNull(d2);
            assertEquals(PersistenceState.COMMITTED, d2.getPersistenceState());

            Iterator it = d2.getEmployees().iterator();
            while (it.hasNext()) {
                Employee ex = (Employee) it.next();
                if ("E2".equals(ex.getName())) {
                    assertTrue(ex instanceof Manager);
                }
                else {
                    assertFalse(ex instanceof Manager);
                }
            }
        }
        finally {
            unblockQueries();
        }
    }
}
