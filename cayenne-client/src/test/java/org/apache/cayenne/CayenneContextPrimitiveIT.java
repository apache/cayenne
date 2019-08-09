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
package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.table_primitives.ClientTablePrimitives;
import org.apache.cayenne.testdo.table_primitives.TablePrimitives;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TABLE_PRIMITIVES_PROJECT)
public class CayenneContextPrimitiveIT extends ClientCase {

    @Inject
    private CayenneContext context;
    
    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tTablePrimitives;

    @Before
    public void setUp() throws Exception {
        int bool = accessStackAdapter.supportsBoolean() ? Types.BOOLEAN : Types.INTEGER;
        
        tTablePrimitives = new TableHelper(dbHelper, "TABLE_PRIMITIVES");
        tTablePrimitives.setColumns("ID", "BOOLEAN_COLUMN", "INT_COLUMN").setColumnTypes(
                Types.INTEGER,
                bool,
                Types.INTEGER);
    }

    private void createTwoPrimitivesDataSet() throws Exception {
        tTablePrimitives.insert(1, accessStackAdapter.supportsBoolean() ? true : 1, 0);
        tTablePrimitives.insert(2, accessStackAdapter.supportsBoolean() ? false : 0, 5);
    }

    @Test
    public void testSelectPrimitives() throws Exception {
        createTwoPrimitivesDataSet();

        List<ClientTablePrimitives> results = ObjectSelect.query(ClientTablePrimitives.class)
                .orderBy("db:" + TablePrimitives.ID_PK_COLUMN, SortOrder.ASCENDING)
                .select(context);
        assertTrue(results.get(0).isBooleanColumn());
        assertFalse(results.get(1).isBooleanColumn());

        assertEquals(0, results.get(0).getIntColumn());
        assertEquals(5, results.get(1).getIntColumn());
    }

    @Test
    public void testCommitChangesPrimitives() throws Exception {

        ClientTablePrimitives object = context.newObject(ClientTablePrimitives.class);

        object.setBooleanColumn(true);
        object.setIntColumn(3);

        context.commitChanges();

        assertTrue(tTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(3, tTablePrimitives.getInt("INT_COLUMN"));

        object.setBooleanColumn(false);
        object.setIntColumn(8);
        context.commitChanges();

        assertFalse(tTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(8, tTablePrimitives.getInt("INT_COLUMN"));
    }

    @Test
    public void testCommitEmptyChangesPrimitives() throws Exception {

        ClientTablePrimitives object = context.newObject(ClientTablePrimitives.class);

        context.commitChanges();

        assertFalse(tTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(0, tTablePrimitives.getInt("INT_COLUMN"));

        object.setBooleanColumn(true);
        object.setIntColumn(8);
        context.commitChanges();

        assertTrue(tTablePrimitives.getBoolean("BOOLEAN_COLUMN"));
        assertEquals(8, tTablePrimitives.getInt("INT_COLUMN"));
    }

    @Test
    public void testSelectEmptyPrimitives() throws Exception {
        tTablePrimitives.insert(1, accessStackAdapter.supportsBoolean() ? true : 1, null);

        ClientTablePrimitives object = ObjectSelect.query(ClientTablePrimitives.class).selectFirst(context);

        assertTrue(object.isBooleanColumn());
        assertEquals(0, object.getIntColumn());
    }
}
