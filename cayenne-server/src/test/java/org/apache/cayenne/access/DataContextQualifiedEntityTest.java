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
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 */
@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class DataContextQualifiedEntityTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tPerson;

    @Override
    protected void setUpAfterInjection() throws Exception {
        // manually break circular deps
        dbHelper.update("PERSON").set("DEPARTMENT_ID", null, Types.INTEGER).execute();

        dbHelper.deleteAll("ADDRESS");
        dbHelper.deleteAll("DEPARTMENT");
        dbHelper.deleteAll("PERSON_NOTES");
        dbHelper.deleteAll("PERSON");
        dbHelper.deleteAll("CLIENT_COMPANY");
        
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

    public void testSelect() throws Exception {
        createPersonsDataSet();

        // just check that an appropriate qualifier was applied
        // no inheritance checks in this case...

        // select Abstract Ppl
        List<?> abstractPpl = context.performQuery(new SelectQuery(AbstractPerson.class));
        assertEquals(6, abstractPpl.size());

        // select Customer Reps
        List<?> customerReps = context.performQuery(new SelectQuery(
                CustomerRepresentative.class));
        assertEquals(1, customerReps.size());

        // select Employees
        List<?> employees = context.performQuery(new SelectQuery(Employee.class));
        assertEquals(5, employees.size());

        // select Managers
        List<?> managers = context.performQuery(new SelectQuery(Manager.class));
        assertEquals(2, managers.size());
    }

    public void testPrefetch() throws Exception {
        createPersonsDataSet();

        // select Managers.. make sure prefetch query works as expected
        List<?> managers = context.performQuery(new SelectQuery(Manager.class));
        assertEquals(2, managers.size());
    }
}
