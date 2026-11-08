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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.inheritance_people.Department;
import org.apache.cayenne.testdo.inheritance_people.Employee;
import org.apache.cayenne.testdo.inheritance_people.Manager;
import org.apache.cayenne.PeopleTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Resolution of disjoint prefetches of entities with an inheritance qualifier.
 */
public class ResolvedSelectPrefetchQualifiedEntityIT extends PeopleTestBase {

    private CompiledMapping mapping;

    @BeforeEach
    public void setUp() {
        mapping = env.mapping();
    }

    @Test
    public void prefetchEmployee() {
        CompiledEntity departmentEntity = mapping.entity(Department.class);
        ObjectSelect<Employee> query = ObjectSelect.query(Employee.class)
                .where(Employee.NAME.eq("abc"))
                .prefetch(Employee.DEPARTMENT.disjoint());

        ResolvedSelect select = query.resolve(mapping);
        assertEquals(1, select.prefetches().size());

        ResolvedSelect prefetch = select.prefetches().getFirst();
        assertEquals(departmentEntity, prefetch.entity());
        assertEquals(ExpressionFactory.exp("db:employees.NAME = 'abc' and (db:employees.PERSON_TYPE = 'EE' "
                + "or db:employees.PERSON_TYPE = 'EM')"), prefetch.where());
    }

    @Test
    public void prefetchManager() {
        CompiledEntity departmentEntity = mapping.entity(Department.class);
        ObjectSelect<Manager> query = ObjectSelect.query(Manager.class)
                .where(Employee.NAME.eq("abc"))
                .prefetch(Manager.DEPARTMENT.disjoint());

        ResolvedSelect select = query.resolve(mapping);
        assertEquals(1, select.prefetches().size());

        ResolvedSelect prefetch = select.prefetches().getFirst();
        assertEquals(departmentEntity, prefetch.entity());
        assertEquals(ExpressionFactory.exp("db:employees.NAME = 'abc' and db:employees.PERSON_TYPE = 'EM'"),
                prefetch.where());
    }
}
