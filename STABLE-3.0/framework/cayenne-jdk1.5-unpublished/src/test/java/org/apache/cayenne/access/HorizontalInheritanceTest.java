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

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity1;
import org.apache.cayenne.unit.InheritanceCase;

/**
 * Tests for horizontal inheritance implementation.
 */
public class HorizontalInheritanceTest extends InheritanceCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testDatabaseUnionCapabilities() {

        QueryChain inserts = new QueryChain();
        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY1 "
                                + "(ID, SUBENTITY_STRING_DB_ATTR, SUPER_INT_DB_ATTR, SUPER_STRING_DB_ATTR) "
                                + "VALUES (1, 'V11', 1, 'V21')"));

        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY2 "
                                + "(ID, OVERRIDDEN_STRING_DB_ATTR, SUPER_INT_DB_ATTR, SUBENTITY_INT_DB_ATTR) "
                                + "VALUES (1, 'VX11', 101, 201)"));

        createDataContext().performGenericQuery(inserts);

        SQLTemplate unionSql = new SQLTemplate(
                SubEntity1.class,
                "SELECT ID, SUBENTITY_STRING_DB_ATTR, SUPER_STRING_DB_ATTR, SUPER_INT_DB_ATTR, 0, 'INHERITANCE_SUB_ENTITY1'"
                        + " FROM INHERITANCE_SUB_ENTITY1"
                        + " UNION ALL"
                        + " SELECT ID, OVERRIDDEN_STRING_DB_ATTR, '', SUBENTITY_INT_DB_ATTR, SUBENTITY_INT_DB_ATTR, 'INHERITANCE_SUB_ENTITY2'"
                        + " FROM INHERITANCE_SUB_ENTITY2");

        unionSql.setFetchingDataRows(true);
        assertEquals(2, createDataContext().performQuery(unionSql).size());
    }

    public void testSelectQueryOnConcreteLeafEntity() {

        QueryChain inserts = new QueryChain();
        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY1 "
                                + "(ID, SUBENTITY_STRING_DB_ATTR, SUPER_INT_DB_ATTR, SUPER_STRING_DB_ATTR) "
                                + "VALUES (1, 'V11', 1, 'V21')"));
        inserts
                .addQuery(new SQLTemplate(
                        SubEntity1.class,
                        "INSERT INTO INHERITANCE_SUB_ENTITY1 "
                                + "(ID, SUBENTITY_STRING_DB_ATTR, SUPER_INT_DB_ATTR, SUPER_STRING_DB_ATTR) "
                                + "VALUES (2, 'V12', 2, 'V22')"));
        createDataContext().performGenericQuery(inserts);

        SelectQuery select = new SelectQuery(SubEntity1.class);
        select.addOrdering(SubEntity1.SUB_ENTITY_STRING_ATTR_PROPERTY, true);

        List<SubEntity1> result = createDataContext().performQuery(select);
        assertEquals(2, result.size());
        assertEquals(PersistenceState.COMMITTED, result.get(0).getPersistenceState());
        assertEquals("V11", result.get(0).getSubEntityStringAttr());
        assertEquals(PersistenceState.COMMITTED, result.get(1).getPersistenceState());
        assertEquals("V12", result.get(1).getSubEntityStringAttr());
    }
}
