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

package org.apache.cayenne.query;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.inheritance_people.Department;
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.Manager;
import org.apache.cayenne.unit.di.runtime.PeopleProjectCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FluentSelectPrefetchRouterActionQualifiedEntityIT extends PeopleProjectCase {

    @Inject
    private EntityResolver resolver;

    @Test
    public void testPrefetchEmployee() {
        ObjEntity departmentEntity = resolver.getObjEntity(Department.class);
        ObjectSelect<Employee> query = ObjectSelect.query(Employee.class, Employee.NAME.eq("abc"))
                .prefetch(Employee.TO_DEPARTMENT.disjoint());

        FluentSelectPrefetchRouterAction action = new FluentSelectPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(query, router, resolver);
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);

        assertEquals(departmentEntity.getName(), prefetch.getEntityName());
        assertEquals(ExpressionFactory.exp("db:employees.NAME = 'abc' and (db:employees.PERSON_TYPE = 'EE' "
                + "or db:employees.PERSON_TYPE = 'EM')"), prefetch.getWhere());
    }

    @Test
    public void testPrefetchManager() {
        ObjEntity departmentEntity = resolver.getObjEntity(Department.class);

        ObjectSelect<Manager> query = ObjectSelect.query(Manager.class, Employee.NAME.eq("abc"))
                .prefetch(Manager.TO_DEPARTMENT.disjoint());

        FluentSelectPrefetchRouterAction action = new FluentSelectPrefetchRouterAction();

        MockQueryRouter router = new MockQueryRouter();
        action.route(query, router, resolver);
        assertEquals(1, router.getQueryCount());

        PrefetchSelectQuery prefetch = (PrefetchSelectQuery) router.getQueries().get(0);
        assertEquals(departmentEntity.getName(), prefetch.getEntityName());
        assertEquals(ExpressionFactory.exp("db:employees.NAME = 'abc' and db:employees.PERSON_TYPE = 'EM'"),
                prefetch.getWhere());
    }
}
