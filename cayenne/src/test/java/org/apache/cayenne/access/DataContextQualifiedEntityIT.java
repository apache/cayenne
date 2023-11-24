/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inheritance_people.AbstractPerson;
import org.apache.cayenne.testdo.inheritance_people.CustomerRepresentative;
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.Manager;
import org.apache.cayenne.unit.di.runtime.PeopleProjectCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DataContextQualifiedEntityIT extends PeopleProjectCase {

    @Inject
    protected ObjectContext context;

    protected TableHelper tPerson;

    @Before
    public void setUp() throws Exception {
        // manually break circular deps
        dbHelper.update("PERSON").set("DEPARTMENT_ID", null, Types.INTEGER).execute();

        tPerson = new TableHelper(dbHelper, "PERSON");
        tPerson.setColumns(
                "CLIENT_COMPANY_ID",
                "CLIENT_CONTACT_TYPE",
                "DEPARTMENT_ID",
                "NAME",
                "PERSON_ID",
                "PERSON_TYPE",
                "SALARY").setColumnTypes(
                Types.INTEGER,
                Types.VARCHAR,
                Types.INTEGER,
                Types.VARCHAR,
                Types.INTEGER,
                Types.CHAR,
                Types.FLOAT);
    }

    protected void createPersonsDataSet() throws Exception {
        tPerson.insert(null, null, null, "e1", 1, "EE", 20000);
        tPerson.insert(null, null, null, "e2", 2, "EE", 25000);
        tPerson.insert(null, null, null, "e3", 3, "EE", 28000);
        tPerson.insert(null, null, null, "m1", 4, "EM", 30000);
        tPerson.insert(null, null, null, "m2", 5, "EM", 40000);
        tPerson.insert(null, null, null, "c1", 6, "C", null);
    }

    @Test
    public void testSelect() throws Exception {
        createPersonsDataSet();

        // just check that an appropriate qualifier was applied
        // no inheritance checks in this case...

        // select Abstract Ppl
        List<AbstractPerson> abstractPpl = ObjectSelect.query(AbstractPerson.class).select(context);
        assertEquals(6, abstractPpl.size());

        // select Customer Reps
        List<CustomerRepresentative> customerReps = ObjectSelect.query(CustomerRepresentative.class).select(context);
        assertEquals(1, customerReps.size());

        // select Employees
        List<Employee> employees = ObjectSelect.query(Employee.class).select(context);
        assertEquals(5, employees.size());

        // select Managers
        List<Manager> managers = ObjectSelect.query(Manager.class).select(context);
        assertEquals(2, managers.size());
    }

    @Test
    public void testPrefetch() throws Exception {
        createPersonsDataSet();

        // select Managers.. make sure prefetch query works as expected
        List<Manager> managers = ObjectSelect.query(Manager.class).select(context);
        assertEquals(2, managers.size());
    }
}
