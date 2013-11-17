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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.inherit.Department;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestClosure;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class DeepMergeOperationInheritanceTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    @Inject
    protected DataChannelInterceptor queryInterceptor;

    public void testDeepMergeExistingSubclass() {

        final Department d1 = context.newObject(Department.class);
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

        final DeepMergeOperation op = new DeepMergeOperation(context1);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Department d2 = (Department) op.merge(d1);
                assertNotNull(d2);
                assertEquals(PersistenceState.COMMITTED, d2.getPersistenceState());

                for (Employee ex : d2.getEmployees()) {
                    if ("E2".equals(ex.getName())) {
                        assertTrue(ex instanceof Manager);
                    }
                    else {
                        assertFalse(ex instanceof Manager);
                    }
                }
            }
        });

    }

    public void testDeepMergeNonExistentSubclass() {

        final Department d1 = context.newObject(Department.class);
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
        final DeepMergeOperation op = new DeepMergeOperation(context1);

        queryInterceptor.runWithQueriesBlocked(new UnitTestClosure() {

            public void execute() {
                Department d2 = (Department) op.merge(d1);
                assertNotNull(d2);
                assertEquals(PersistenceState.COMMITTED, d2.getPersistenceState());

                for (Employee ex : d2.getEmployees()) {
                    if ("E2".equals(ex.getName())) {
                        assertTrue(ex instanceof Manager);
                    }
                    else {
                        assertFalse(ex instanceof Manager);
                    }
                }
            }
        });
    }
}
