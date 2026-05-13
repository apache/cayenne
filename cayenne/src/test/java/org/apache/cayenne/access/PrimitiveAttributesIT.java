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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.primitive.PrimitivesTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrimitiveAttributesIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.PRIMITIVE_PROJECT);

    private DataContext context;
    private DBHelper dbHelper;
    private UnitDbAdapter unitDbAdapter;

    @BeforeEach
    public void setUp() {
        context = env.dataContext();
        dbHelper = env.dbHelper();
        unitDbAdapter = env.getInstance(UnitDbAdapter.class);
    }


    @Test
    public void commit() {
        PrimitivesTestEntity e = context.newObject(PrimitivesTestEntity.class);
        e.setBooleanColumn(true);
        e.setIntColumn(88);
        e.setCharColumn('B');
        context.commitChanges();
    }

    @Test
    public void selectTest() throws Exception {
        TableHelper tPrimitives = new TableHelper(dbHelper, "PRIMITIVES_TEST");
        tPrimitives.setColumns("ID", "BOOLEAN_COLUMN", "INT_COLUMN", "CHAR_COLUMN");
        tPrimitives.insert(1, true, -100, String.valueOf('a'))
                .insert(2, false, 0, String.valueOf('~'))
                .insert(3, true, Integer.MAX_VALUE, String.valueOf('Z'));

        List<PrimitivesTestEntity> result = ObjectSelect.query(PrimitivesTestEntity.class)
                .orderBy(PrimitivesTestEntity.INT_COLUMN.asc()).select(context);
        assertEquals(3, result.size());
        assertEquals(-100, result.get(0).getIntColumn());
        assertEquals('a', result.get(0).getCharColumn());
        assertTrue(result.get(0).isBooleanColumn());

        assertEquals(0, result.get(1).getIntColumn());
        assertEquals('~', result.get(1).getCharColumn());
        assertFalse(result.get(1).isBooleanColumn());

        assertEquals(Integer.MAX_VALUE, result.get(2).getIntColumn());
        assertEquals('Z', result.get(2).getCharColumn());
        assertTrue(result.get(2).isBooleanColumn());
    }
}
