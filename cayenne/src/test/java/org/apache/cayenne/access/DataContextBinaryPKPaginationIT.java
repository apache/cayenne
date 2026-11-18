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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.binary_pk.BinaryPKCompound;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Paginated queries over binary PKs. Note: HSQLDB returns the same byte[] instance for each read of a row, hiding
 * identity comparisons. H2 doesn't.
 */
public class DataContextBinaryPKPaginationIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.BINARY_PK_PROJECT);

    private static boolean supported() {
        return env.testDbAdapter().supportsBinaryPK();
    }

    @BeforeEach
    public void createTestData() throws SQLException {
        if (!supported()) {
            return;
        }

        TableHelper tMaster = env.table("BINARY_PK_TEST1").setColumns("BIN_ID", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.VARCHAR);
        TableHelper tCompound = env.table("BINARY_PK_COMPOUND").setColumns("BIN_ID", "NUM", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.INTEGER, Types.VARCHAR);
        for (int i = 1; i <= 5; i++) {
            tMaster.insert(new byte[]{(byte) i, 1, 2}, "m" + i);
            // the binary part alone is not unique
            tCompound.insert(new byte[]{(byte) (i % 2), 3}, i, "c" + i);
        }
    }

    @Test
    public void objects() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .orderBy(BinaryPKTest1.NAME.asc())
                .pageSize(2)
                .select(env.context());

        assertEquals(5, masters.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("m" + (i + 1), masters.get(i).getName());
        }
    }

    @Test
    public void indexOfUnresolved() {
        if (!supported()) {
            return;
        }

        DataContext context = env.context();
        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .orderBy(BinaryPKTest1.NAME.asc())
                .pageSize(2)
                .select(context);

        // not resolved in the list yet
        BinaryPKTest1 m4 = ObjectSelect.query(BinaryPKTest1.class)
                .where(BinaryPKTest1.NAME.eq("m4"))
                .selectOne(context);

        assertEquals(3, masters.indexOf(m4));
        assertEquals(3, masters.lastIndexOf(m4));
    }

    @Test
    public void dataRows() {
        if (!supported()) {
            return;
        }

        List<DataRow> rows = ObjectSelect.dataRowQuery(BinaryPKTest1.class)
                .orderBy(BinaryPKTest1.NAME.asc())
                .pageSize(2)
                .select(env.context());

        assertEquals(5, rows.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("m" + (i + 1), rows.get(i).get("NAME"));
        }
    }

    @Test
    public void compoundKeyObjects() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompound> objects = ObjectSelect.query(BinaryPKCompound.class)
                .orderBy(BinaryPKCompound.NAME.asc())
                .pageSize(2)
                .select(env.context());

        assertEquals(5, objects.size());
        for (int i = 0; i < 5; i++) {
            assertEquals("c" + (i + 1), objects.get(i).getName());
        }
    }

    @Test
    public void mixedColumns() {
        if (!supported()) {
            return;
        }

        List<Object[]> rows = ObjectSelect.columnQuery(BinaryPKTest1.class, BinaryPKTest1.SELF, BinaryPKTest1.NAME)
                .orderBy(BinaryPKTest1.NAME.asc())
                .pageSize(2)
                .select(env.context());

        assertEquals(5, rows.size());
        for (int i = 0; i < 5; i++) {
            Object[] row = rows.get(i);
            BinaryPKTest1 master = assertInstanceOf(BinaryPKTest1.class, row[0]);
            assertEquals("m" + (i + 1), master.getName());
            assertEquals("m" + (i + 1), row[1]);
        }
    }
}
