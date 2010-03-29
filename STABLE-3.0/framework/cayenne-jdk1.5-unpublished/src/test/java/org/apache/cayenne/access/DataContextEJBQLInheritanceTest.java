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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.testdo.inherit.CustomerRepresentative;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;

public class DataContextEJBQLInheritanceTest extends PeopleCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testSelect() throws Exception {
        createTestData("testSelect");

        ObjectContext context = createDataContext();

        EJBQLQuery superclass = new EJBQLQuery(
                "select p from AbstractPerson p ORDER BY p.name");

        List superclassResult = context.performQuery(superclass);
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

        List subclassResult = context.performQuery(subclass);
        assertEquals(4, subclassResult.size());

        assertEquals(Employee.class.getName(), subclassResult.get(0).getClass().getName());
        assertEquals(Employee.class.getName(), subclassResult.get(1).getClass().getName());
        assertEquals(Manager.class.getName(), subclassResult.get(2).getClass().getName());
        assertEquals(Manager.class.getName(), subclassResult.get(3).getClass().getName());
    }
}
