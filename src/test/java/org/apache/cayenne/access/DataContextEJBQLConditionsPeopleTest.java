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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.inherit.Address;
import org.apache.cayenne.testdo.inherit.Department;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;

public class DataContextEJBQLConditionsPeopleTest extends PeopleCase {

    @Override
    protected void setUp() throws Exception {
        deleteTestData();

        ObjectContext context = createDataContext();
        Department d1 = context.newObject(Department.class);
        d1.setName("d1");

        Department d2 = context.newObject(Department.class);
        d2.setName("d2");

        Department d3 = context.newObject(Department.class);
        d3.setName("d3");

        context.commitChanges();

        Manager m1 = context.newObject(Manager.class);
        m1.setName("m1");
        m1.setPersonType("EM");

        Manager m2 = context.newObject(Manager.class);
        m2.setName("m2");
        m2.setPersonType("EM");

        Manager m3 = context.newObject(Manager.class);
        m3.setName("m3");
        m3.setPersonType("EM");

        Address a1 = context.newObject(Address.class);
        m1.addToAddresses(a1);

        Address a2 = context.newObject(Address.class);
        m2.addToAddresses(a2);

        Address a3 = context.newObject(Address.class);
        m3.addToAddresses(a3);

        d1.addToEmployees(m1);
        d1.addToEmployees(m2);
        d3.addToEmployees(m3);

        context.commitChanges();

        d1.setToManager(m1);
        d2.setToManager(m2);
        d3.setToManager(m3);

        context.commitChanges();
    }

    @Override
    protected void tearDown() throws Exception {
        // help in cleaning up relationship cycles...
        ObjectContext context = createDataContext();
        context.performGenericQuery(new SQLTemplate(
                Department.class,
                "update DEPARTMENT set MANAGER_ID = NULL"));
    }

    public void testCollectionMemberOfId() throws Exception {

        String ejbql = "SELECT DISTINCT m FROM Manager m JOIN m.managedDepartments d"
                + " WHERE m MEMBER d.employees";

        ObjectContext context = createDataContext();

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(2, objects.size());

        Set<String> ids = new HashSet<String>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Manager m = (Manager) it.next();
            ids.add(m.getName());
        }

        assertTrue(ids.contains("m1"));
        assertTrue(ids.contains("m3"));
    }

    public void testCollectionNotMemberOfId() throws Exception {

        String ejbql = "SELECT DISTINCT m FROM Manager m JOIN m.managedDepartments d"
                + " WHERE m NOT MEMBER d.employees";

        ObjectContext context = createDataContext();

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Set<String> ids = new HashSet<String>();
        Iterator<?> it = objects.iterator();
        while (it.hasNext()) {
            Manager m = (Manager) it.next();
            ids.add(m.getName());
        }

        assertTrue(ids.contains("m2"));
    }

    public void testCollectionNotMemberOfToOne() throws Exception {

        // need a better test ... this query returns zero rows by definition
        String ejbql = "SELECT a"
                + " FROM Address a JOIN a.toEmployee m JOIN m.toDepartment d"
                + " WHERE m NOT MEMBER d.employees";

        ObjectContext context = createDataContext();

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<?> objects = context.performQuery(query);
        assertEquals(0, objects.size());
    }
}
