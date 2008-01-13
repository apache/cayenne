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

import java.util.List;

import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;

/**
 * @author Andrus Adamchik
 */
public class DataContextQualifiedEntityTest extends PeopleCase {
    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testSelect() throws Exception {
        createTestData("test");

        // just check that an appropriate qualifier was applied
        // no inheritance checks in this case...

        // select Abstract Ppl
        List abstractPpl = context.performQuery(new SelectQuery(AbstractPerson.class));
        assertEquals(6, abstractPpl.size());

        // select Customer Reps
        List customerReps =
            context.performQuery(new SelectQuery(CustomerRepresentative.class));
        assertEquals(1, customerReps.size());

        // select Employees
        List employees = context.performQuery(new SelectQuery(Employee.class));
        assertEquals(5, employees.size());

        // select Managers
        List managers = context.performQuery(new SelectQuery(Manager.class));
        assertEquals(2, managers.size());
    }

    public void testPrefetch() throws Exception {
        createTestData("test");

        // select Managers.. make sure prefetch query works as expected
        List managers = context.performQuery(new SelectQuery(Manager.class));
        assertEquals(2, managers.size());
    }
}
