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
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.PEOPLE_PROJECT)
public class DataContextEJBQLInheritanceTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PERSON");

        TableHelper person = new TableHelper(dbHelper, "PERSON");
        person.setColumns("PERSON_ID", "NAME", "PERSON_TYPE", "SALARY")
                .setColumnTypes(
                        Types.INTEGER, 
                        Types.VARCHAR, 
                        Types.CHAR,
                        Types.FLOAT);

        person.insert(1, "a", "EE", 20000);
        person.insert(2, "b", "EE", 25000);
        person.insert(4, "c", "EM", 30000);
        person.insert(5, "d", "EM", 40000);
        person.insert(6, "e", "C", null);
    }

    public void testSelect() throws Exception {

        EJBQLQuery superclass = new EJBQLQuery(
                "select p from AbstractPerson p ORDER BY p.name");

        List<?> superclassResult = context.performQuery(superclass);
        assertEquals(5, superclassResult.size());

        assertEquals(Employee.class.getName(), superclassResult
                .get(0)
                .getClass()
                .getName());
        assertEquals(Employee.class.getName(), superclassResult
                .get(1)
                .getClass()
                .getName());
        assertEquals(Manager.class.getName(), superclassResult
                .get(2)
                .getClass()
                .getName());
        assertEquals(Manager.class.getName(), superclassResult
                .get(3)
                .getClass()
                .getName());
        assertEquals(CustomerRepresentative.class.getName(), superclassResult
                .get(4)
                .getClass()
                .getName());

        EJBQLQuery subclass = new EJBQLQuery("select e from Employee e ORDER BY e.name");

        List<?> subclassResult = context.performQuery(subclass);
        assertEquals(4, subclassResult.size());

        assertEquals(Employee.class.getName(), subclassResult.get(0).getClass().getName());
        assertEquals(Employee.class.getName(), subclassResult.get(1).getClass().getName());
        assertEquals(Manager.class.getName(), subclassResult.get(2).getClass().getName());
        assertEquals(Manager.class.getName(), subclassResult.get(3).getClass().getName());
    }
}
