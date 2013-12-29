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

import java.util.List;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class ClientSelectQueryExpressionTest extends ClientCase {

    @Inject
    private CayenneContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTable1;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE1");

        tMtTable1 = new TableHelper(dbHelper, "MT_TABLE1");
        tMtTable1.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1");
    }

    protected void createMtTable1DataSet() throws Exception {
        for (int i = 1; i <= 20; i++) {
            tMtTable1.insert(i, "globalAttr" + i, "serverAttr" + i);
        }
    }

    public void testDoubleSelectLikeExpression() throws Exception {
        createMtTable1DataSet();

        List<ClientMtTable1> mtTable1List = context.performQuery(new SelectQuery(ClientMtTable1.class));

        Expression exp = ExpressionFactory.likeExp(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, "globalAttr1%");
        exp.filterObjects(mtTable1List);

        SelectQuery query = new SelectQuery(ClientMtTable1.class, exp);
        List<ClientMtTable1> matchingMtTableList = context.performQuery(query);

        assertEquals(11, matchingMtTableList.size());
    }
}
