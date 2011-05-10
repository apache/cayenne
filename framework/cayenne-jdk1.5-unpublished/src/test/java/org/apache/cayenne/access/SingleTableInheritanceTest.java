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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class SingleTableInheritanceTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tPerson;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("ADDRESS");
        dbHelper.deleteAll("DEPARTMENT");
        dbHelper.deleteAll("PERSON_NOTES");
        dbHelper.deleteAll("CLIENT_COMPANY");
        dbHelper.deleteAll("PERSON");

        tPerson = new TableHelper(dbHelper, "PERSON");
        tPerson.setColumns("PERSON_ID", "NAME", "PERSON_TYPE");
    }

    private void create2PersonDataSet() throws Exception {
        tPerson.insert(1, "E1", "EE");
        tPerson.insert(2, "E2", "EM");
    }

    private void create5PersonDataSet() throws Exception {
        tPerson.insert(1, "E1", "EE");
        tPerson.insert(2, "E2", "EM");
        tPerson.insert(3, "E3", "EE");
        tPerson.insert(4, "E4", "EM");
        tPerson.insert(5, "E5", "EE");
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
}
