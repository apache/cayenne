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

import static java.util.Collections.singletonMap;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.consttest.Const1Entity;
import org.apache.cayenne.testdo.consttest.Const1Type;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime("cayenne-const.xml")
public class ConstQueryTest extends ServerCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CONST1_ENTITY");
    }

    private void createConst1EntityDataSet() throws Exception {
        TableHelper tableHelper = new TableHelper(dbHelper, "CONST1_ENTITY");
        tableHelper.setColumns("ID", "NAME", "STATUS", "TYPE");
        tableHelper.insert(1, "entity1", 1, 0);
        tableHelper.insert(2, "entity2", null, 1);
    }
    
    private void createConst1EntityDataSetWithEnumName() throws Exception {
        TableHelper tableHelper = new TableHelper(dbHelper, "CONST1_ENTITY");
        tableHelper.setColumns("ID", "NAME", "STATUS", "TYPE");
        tableHelper.insert(1, "entity1", 1, 0);
        tableHelper.insert(2, "org.apache.cayenne.testdo.consttest.Const1Type.ADMIN", null, 1);
    }

    public void testSelectByEnumValue() throws Exception {
        createConst1EntityDataSet();

        Expression expr = Expression.fromString("type = $type");
        SelectQuery query = new SelectQuery(Const1Entity.class, expr)
                .queryWithParameters(singletonMap("type", Const1Type.ORDINARY));
        List users = context.performQuery(query);
        assertEquals(1, users.size());
        assertEquals("entity1", ((Const1Entity) users.get(0)).getName());
    }

    public void testSelectByEnumValueSpecifiedAsConstant() throws Exception {
        createConst1EntityDataSet();

        Expression expr = Expression
                .fromString("type = org.apache.cayenne.testdo.consttest.Const1Type.ADMIN");
        SelectQuery query = new SelectQuery(Const1Entity.class, expr);
        List users = context.performQuery(query);
        assertEquals(1, users.size());
        assertEquals("entity2", ((Const1Entity) users.get(0)).getName());
    }

    public void testSelectByConstValue() throws Exception {
        createConst1EntityDataSet();

        Expression expr = Expression
                .fromString("status = org.apache.cayenne.testdo.consttest.Const1Status.DEFAULT");
        SelectQuery query = new SelectQuery(Const1Entity.class, expr);
        List users = context.performQuery(query);
        assertEquals(1, users.size());
        assertEquals("entity1", ((Const1Entity) users.get(0)).getName());
    }
    
    public void testSelectByString() throws Exception {
        createConst1EntityDataSetWithEnumName();

        Expression expr = Expression
                .fromString("name = 'org.apache.cayenne.testdo.consttest.Const1Type.ADMIN'");
        SelectQuery query = new SelectQuery(Const1Entity.class, expr);
        List users = context.performQuery(query);
        assertEquals(1, users.size());
        assertEquals("org.apache.cayenne.testdo.consttest.Const1Type.ADMIN", ((Const1Entity) users.get(0)).getName());
    }
}
