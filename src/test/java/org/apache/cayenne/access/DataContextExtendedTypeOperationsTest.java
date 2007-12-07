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

import java.util.Arrays;

import org.apache.art.ExtendedTypeEntity;
import org.apache.art.StringET1;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextExtendedTypeOperationsTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testStoreExtendedType() {
        ExtendedTypeEntity e1 = createDataContext().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));
        e1.getObjectContext().commitChanges();

        SQLTemplate checkQ = new SQLTemplate(
                ExtendedTypeEntity.class,
                "SELECT * FROM EXTENDED_TYPE_TEST WHERE NAME = 'X'");
        checkQ.setFetchingDataRows(true);
        checkQ.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
        assertEquals(1, e1.getObjectContext().performQuery(checkQ).size());
    }

    public void testInExpressionExtendedTypeArray() {
        ExtendedTypeEntity e1 = createDataContext().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));

        ExtendedTypeEntity e2 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e2.setName(new StringET1("Y"));

        ExtendedTypeEntity e3 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e3.setName(new StringET1("Z"));

        e1.getObjectContext().commitChanges();

        Expression in = ExpressionFactory.inExp(
                ExtendedTypeEntity.NAME_PROPERTY,
                new Object[] {
                        new StringET1("X"), new StringET1("Y")
                });
        SelectQuery query = new SelectQuery(ExtendedTypeEntity.class, in);
        assertEquals(2, e1.getObjectContext().performQuery(query).size());
    }

    public void testInExpressionExtendedTypeList() {
        ExtendedTypeEntity e1 = createDataContext().newObject(ExtendedTypeEntity.class);
        e1.setName(new StringET1("X"));

        ExtendedTypeEntity e2 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e2.setName(new StringET1("Y"));

        ExtendedTypeEntity e3 = e1.getObjectContext().newObject(ExtendedTypeEntity.class);
        e3.setName(new StringET1("Z"));

        e1.getObjectContext().commitChanges();

        Expression in = ExpressionFactory.inExp(ExtendedTypeEntity.NAME_PROPERTY, Arrays
                .asList(new StringET1("X"), new StringET1("Y")));
        SelectQuery query = new SelectQuery(ExtendedTypeEntity.class, in);
        assertEquals(2, e1.getObjectContext().performQuery(query).size());
    }
}
