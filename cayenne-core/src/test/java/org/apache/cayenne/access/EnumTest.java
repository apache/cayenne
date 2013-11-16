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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Enum1;
import org.apache.cayenne.testdo.testmap.EnumEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EnumTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("ENUM_ENTITY");

    }

    private void createDataSet() throws Exception {
        TableHelper tEnumEntity = new TableHelper(dbHelper, "ENUM_ENTITY");
        tEnumEntity.setColumns("ID", "ENUM_ATTRIBUTE");

        tEnumEntity.insert(1, "two");
        tEnumEntity.insert(2, "one");
    }

    public void testInsert() {
        EnumEntity e = context.newObject(EnumEntity.class);
        e.setEnumAttribute(Enum1.one);
        context.commitChanges();
    }

    public void testSelectQuery() throws Exception {
        createDataSet();

        SelectQuery q = new SelectQuery(EnumEntity.class);
        q.andQualifier(ExpressionFactory.matchExp(
                EnumEntity.ENUM_ATTRIBUTE_PROPERTY,
                Enum1.one));

        EnumEntity e = (EnumEntity) Cayenne.objectForQuery(context, q);
        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }

    public void testSQLTemplate() throws Exception {
        createDataSet();

        SQLTemplate q = new SQLTemplate(
                EnumEntity.class,
                "SELECT * FROM ENUM_ENTITY WHERE ENUM_ATTRIBUTE = 'one'");
        q.setColumnNamesCapitalization(CapsStrategy.UPPER);

        EnumEntity e = (EnumEntity) Cayenne.objectForQuery(context, q);
        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }
}
