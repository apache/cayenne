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

import org.apache.art.Enum1;
import org.apache.art.EnumEntity;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class EnumTest extends CayenneCase {

    public void testInsert() {
        ObjectContext context = createDataContext();

        EnumEntity e = context.newObject(EnumEntity.class);
        e.setEnumAttribute(Enum1.one);

        context.commitChanges();
    }

    public void testSelectQuery() throws Exception {
        deleteTestData();
        ObjectContext context = createDataContext();

        context.performGenericQuery(new SQLTemplate(
                EnumEntity.class,
                "insert into ENUM_ENTITY (ID, ENUM_ATTRIBUTE) VALUES (1, 'two')"));
        context.performGenericQuery(new SQLTemplate(
                EnumEntity.class,
                "insert into ENUM_ENTITY (ID, ENUM_ATTRIBUTE) VALUES (2, 'one')"));

        SelectQuery q = new SelectQuery(EnumEntity.class);
        q.andQualifier(ExpressionFactory.matchExp(
                EnumEntity.ENUM_ATTRIBUTE_PROPERTY,
                Enum1.one));

        EnumEntity e = (EnumEntity) DataObjectUtils.objectForQuery(context, q);
        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }

    public void testSQLTemplate() throws Exception {
        deleteTestData();
        ObjectContext context = createDataContext();

        context.performGenericQuery(new SQLTemplate(
                EnumEntity.class,
                "insert into ENUM_ENTITY (ID, ENUM_ATTRIBUTE) VALUES (1, 'two')"));
        context.performGenericQuery(new SQLTemplate(
                EnumEntity.class,
                "insert into ENUM_ENTITY (ID, ENUM_ATTRIBUTE) VALUES (2, 'one')"));

        SQLTemplate q = new SQLTemplate(
                EnumEntity.class,
                "SELECT * FROM ENUM_ENTITY WHERE ENUM_ATTRIBUTE = 'one'");
        q.setColumnNamesCapitalization(CapsStrategy.UPPER);

        EnumEntity e = (EnumEntity) DataObjectUtils.objectForQuery(context, q);
        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }
}
