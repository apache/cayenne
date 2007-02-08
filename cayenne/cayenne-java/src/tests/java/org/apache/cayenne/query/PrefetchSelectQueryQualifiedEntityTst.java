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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.inherit.Department;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleTestCase;

/**
 * @author Andrei Adamchik
 */
public class PrefetchSelectQueryQualifiedEntityTst extends PeopleTestCase {

    /**
     * @deprecated since 1.2
     */
    public void testPrefetchEmployee() throws Exception {
        ObjEntity departmentEntity = getDomain().getEntityResolver().lookupObjEntity(
                Department.class);
        SelectQuery q = new SelectQuery(Employee.class, ExpressionFactory.matchExp(
                "name",
                "abc"));

        PrefetchSelectQuery prefetch = new PrefetchSelectQuery(getDomain()
                .getEntityResolver(), q, "toDepartment");

        assertSame(departmentEntity, prefetch.getRoot());
        assertEquals(Expression.fromString("db:employees.NAME = 'abc' "
                + "and (db:employees.PERSON_TYPE = 'EE' "
                + "or db:employees.PERSON_TYPE = 'EM')"), prefetch.getQualifier());
    }

    /**
     * @deprecated since 1.2
     */
    public void testPrefetchManager() throws Exception {
        ObjEntity departmentEntity = getDomain().getEntityResolver().lookupObjEntity(
                Department.class);
        SelectQuery q = new SelectQuery(Manager.class, ExpressionFactory.matchExp(
                "name",
                "abc"));

        PrefetchSelectQuery prefetch = new PrefetchSelectQuery(getDomain()
                .getEntityResolver(), q, "toDepartment");

        assertSame(departmentEntity, prefetch.getRoot());
        assertEquals(
                Expression
                        .fromString("db:employees.NAME = 'abc' and db:employees.PERSON_TYPE = 'EM'"),
                prefetch.getQualifier());
    }
}
