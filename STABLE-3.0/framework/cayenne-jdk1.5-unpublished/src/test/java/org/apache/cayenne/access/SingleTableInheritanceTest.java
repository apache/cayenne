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

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.inherit.AbstractPerson;
import org.apache.cayenne.testdo.inherit.Employee;
import org.apache.cayenne.testdo.inherit.Manager;
import org.apache.cayenne.unit.PeopleCase;

public class SingleTableInheritanceTest extends PeopleCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }
    
    public void testMatchingOnSuperAttributes() {
        QueryChain insert = new QueryChain();
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (1, 'E1', 'EE')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (2, 'E2', 'EM')"));
        createDataContext().performGenericQuery(insert);
        
        // fetch on leaf, but match on a super attribute
        SelectQuery select = new SelectQuery(Manager.class);
        select.andQualifier(ExpressionFactory.matchExp(AbstractPerson.NAME_PROPERTY, "E2"));
    
        List<Manager> results = createDataContext().performQuery(select);
        assertEquals(1, results.size());
        assertEquals("E2", results.get(0).getName());
    }
    
    public void testMatchingOnSuperAttributesWithPrefetch() {
        QueryChain insert = new QueryChain();
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (1, 'E1', 'EE')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (2, 'E2', 'EM')"));
        createDataContext().performGenericQuery(insert);
        
        // fetch on leaf, but match on a super attribute
        SelectQuery select = new SelectQuery(Employee.class);
        select.addPrefetch(Employee.TO_DEPARTMENT_PROPERTY);
        select.andQualifier(ExpressionFactory.matchExp(AbstractPerson.NAME_PROPERTY, "E2"));
    
        List<Manager> results = createDataContext().performQuery(select);
        assertEquals(1, results.size());
        assertEquals("E2", results.get(0).getName());
    }

    public void testPaginatedQueries() {

        QueryChain insert = new QueryChain();
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (1, 'E1', 'EE')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (2, 'E2', 'EM')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (3, 'E3', 'EE')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (4, 'E4', 'EM')"));
        insert
                .addQuery(new SQLTemplate(
                        AbstractPerson.class,
                        "INSERT INTO PERSON (PERSON_ID, NAME, PERSON_TYPE) VALUES (5, 'E5', 'EE')"));
        createDataContext().performGenericQuery(insert);

        SelectQuery select = new SelectQuery(AbstractPerson.class);
        select.addOrdering("db:" + AbstractPerson.PERSON_ID_PK_COLUMN, true);
        select.setPageSize(3);

        List<AbstractPerson> results = createDataContext().performQuery(select);
        assertEquals(5, results.size());

        assertTrue(results.get(0) instanceof Employee);

        // this is where things would blow up per CAY-1142
        assertTrue(results.get(1) instanceof Manager);

        assertTrue(results.get(3) instanceof Manager);
        assertTrue(results.get(4) instanceof Employee);
    }
}
