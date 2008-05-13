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

package org.apache.cayenne.util;

import java.util.Iterator;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.inherit.Department;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;
import org.apache.cayenne.util.DeepMergeOperation;

public class DeepMergeOperationInheritanceTest extends PeopleCase {

    public void testDeepMergeExistingSubclass() {

        ClassDescriptor d = getDomain().getEntityResolver().getClassDescriptor(
                "Department");

        DataContext context = createDataContext();
        DataContext context1 = createDataContext();

        Department d1 = context.newObject(Department.class);
        d1.setName("D1");

        // need to do double commit as Ashwood sorter blows on Employees/Departments
        // ordering...
        context.commitChanges();

        Employee e1 = context.newObject(Employee.class);
        e1.setName("E1");
        e1.setPersonType("EE");
        d1.addToEmployees(e1);

        Manager e2 = context.newObject(Manager.class);
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

        Department d1 = context.newObject(Department.class);
        d1.setName("D1");

        // need to do double commit as Ashwood sorter blows on Employees/Departments
        // ordering...
        context.commitChanges();

        Employee e1 = context.newObject(Employee.class);
        e1.setName("E1");
        e1.setPersonType("EE");
        d1.addToEmployees(e1);

        Manager e2 = context.newObject(Manager.class);
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
