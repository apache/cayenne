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

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.primitive.PrimitivesTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cayenne.exp.FunctionExpressionFactory.avgExp;
import static org.apache.cayenne.exp.FunctionExpressionFactory.sumExp;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
@UseServerRuntime(CayenneProjects.PRIMITIVE_PROJECT)
public class ObjectSelect_PrimitiveColumnsIT extends ServerCase {
    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    private TableHelper tPrimitives;

    @Before
    public void createTestRecords() throws Exception {
        tPrimitives = new TableHelper(dbHelper, "PRIMITIVES_TEST");
        tPrimitives.setColumns("ID", "BOOLEAN_COLUMN", "INT_COLUMN");
        for (int i = 1; i <= 20; i++) {
            tPrimitives.insert(i, (i % 2 == 0), i * 10);
        }
    }

    @Test
    public void test_SelectIntegerColumn() throws Exception {
        int intColumn2 = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(PrimitivesTestEntity.INT_COLUMN)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .selectFirst(context);
        assertEquals(10, intColumn2);
    }

    @Test
    public void test_SelectIntegerList() throws Exception {
        List<Integer> intColumns = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(PrimitivesTestEntity.INT_COLUMN)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .select(context);
        assertEquals(20, intColumns.size());
        assertEquals(10, (int)intColumns.get(0));
    }

    @Test
    public void test_SelectIntegerExpColumn() throws Exception {
        Property<Integer> property = Property.create("intColumn",
                ExpressionFactory.exp("(obj:intColumn + obj:intColumn)"), Integer.class);

        int intColumn2 = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(property)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .selectFirst(context);
        assertEquals(20, intColumn2);
    }

    @Test
    public void test_SelectBooleanColumn() throws Exception {
        boolean boolColumn = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(PrimitivesTestEntity.BOOLEAN_COLUMN)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .selectFirst(context);
        assertEquals(false, boolColumn);
    }

    @Test
    public void test_SelectBooleanList() throws Exception {
        List<Boolean> intColumns = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(PrimitivesTestEntity.BOOLEAN_COLUMN)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .select(context);
        assertEquals(20, intColumns.size());
        assertEquals(false, intColumns.get(0));
    }

    @Test
    public void test_SelectBooleanExpColumn() throws Exception {
        if(!unitDbAdapter.supportsSelectBooleanExpression()) {
            return;
        }

        Property<Boolean> property = Property.create("boolColumn",
                ExpressionFactory.exp("(obj:intColumn < 10)"), Boolean.class);

        boolean boolColumn = ObjectSelect.query(PrimitivesTestEntity.class)
                .column(property)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .selectFirst(context);
        assertEquals(false, boolColumn);
    }

    @Test
    public void test_SelectColumnsList() throws Exception {
        List<Object[]> columns = ObjectSelect.query(PrimitivesTestEntity.class)
                .columns(PrimitivesTestEntity.INT_COLUMN, PrimitivesTestEntity.BOOLEAN_COLUMN)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .select(context);

        assertEquals(20, columns.size());
        Object[] result = {10, false};
        assertArrayEquals(result, columns.get(0));
    }

    @Test
    public void test_SelectColumnsExpList() throws Exception {
        if(!unitDbAdapter.supportsSelectBooleanExpression()) {
            return;
        }

        Property<Integer> intProperty = Property.create("intColumn",
                ExpressionFactory.exp("(obj:intColumn + 1)"), Integer.class);

        Property<Boolean> boolProperty = Property.create("boolColumn",
                ExpressionFactory.exp("(obj:intColumn = 10)"), Boolean.class);

        List<Object[]> columns = ObjectSelect.query(PrimitivesTestEntity.class)
                .columns(intProperty, boolProperty)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc())
                .select(context);

        assertEquals(20, columns.size());
        Object[] result = {11, true};
        assertArrayEquals(result, columns.get(0));
    }

    @Test
    public void testSum() throws Exception {
        int sum = ObjectSelect.query(PrimitivesTestEntity.class)
                .sum(PrimitivesTestEntity.INT_COLUMN)
                .selectOne(context);
        assertEquals(2100, sum);
    }

    @Test
    public void testAvg() throws Exception {
        int avg = ObjectSelect.query(PrimitivesTestEntity.class)
                .avg(PrimitivesTestEntity.INT_COLUMN)
                .selectOne(context);
        assertEquals(105.0, avg, 0.00001);
    }

    @Test
    public void testOrderByCount() throws Exception {
        tPrimitives.insert(21, true, 210);

        List<Object[]> res = ObjectSelect
                .columnQuery(PrimitivesTestEntity.class, PrimitivesTestEntity.BOOLEAN_COLUMN, PrimitivesTestEntity.INT_COLUMN.count())
                .orderBy(PrimitivesTestEntity.INT_COLUMN.count().asc())
                .select(context);
        assertEquals(2, res.size());

        assertEquals(false, res.get(0)[0]);
        assertEquals(true, res.get(1)[0]);

        assertEquals(10L, res.get(0)[1]);
        assertEquals(11L, res.get(1)[1]);
    }
}
