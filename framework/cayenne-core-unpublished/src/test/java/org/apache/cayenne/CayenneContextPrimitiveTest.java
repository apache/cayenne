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
package org.apache.cayenne;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.ClientMtTablePrimitives;
import org.apache.cayenne.testdo.mt.MtTablePrimitives;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextPrimitiveTest extends ClientCase {

    @Inject
    private CayenneContext context;
    
    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMtTablePrimitives;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE_PRIMITIVES");

        int bool = accessStackAdapter.supportsBoolean() ? Types.BOOLEAN : Types.INTEGER;
        
        tMtTablePrimitives = new TableHelper(dbHelper, "MT_TABLE_PRIMITIVES");
        tMtTablePrimitives.setColumns("ID", "BOOLEAN_COLUMN", "INT_COLUMN").setColumnTypes(
                Types.INTEGER,
                bool,
                Types.INTEGER);
    }

    private void createTwoPrimitivesDataSet() throws Exception {
        tMtTablePrimitives.insert(1, accessStackAdapter.supportsBoolean() ? true : 1, 0);
        tMtTablePrimitives.insert(2, accessStackAdapter.supportsBoolean() ? false : 0, 5);
    }

    public void testSelectPrimitives() throws Exception {
        createTwoPrimitivesDataSet();

        SelectQuery query = new SelectQuery(ClientMtTablePrimitives.class);
        query.addOrdering("db:" + MtTablePrimitives.ID_PK_COLUMN, SortOrder.ASCENDING);

        List<ClientMtTablePrimitives> results = context.performQuery(query);
        assertTrue(results.get(0).isBooleanColumn());
        assertFalse(results.get(1).isBooleanColumn());

        assertEquals(0, results.get(0).getIntColumn());
        assertEquals(5, results.get(1).getIntColumn());
    }

    public void testCommitChangesPrimitives() throws Exception {

        ClientMtTablePrimitives object = context.newObject(ClientMtTablePrimitives.class);

        object.setBooleanColumn(true);
        object.setIntColumn(3);

        context.commitChanges();

        assertTrue(tMtTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(3, tMtTablePrimitives.getInt("INT_COLUMN"));

        object.setBooleanColumn(false);
        object.setIntColumn(8);
        context.commitChanges();

        assertFalse(tMtTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(8, tMtTablePrimitives.getInt("INT_COLUMN"));
    }
}
