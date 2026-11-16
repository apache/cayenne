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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest1;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest2;
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
 * IN qualifiers with binary values, either explicit or built by "disjoint by id" prefetches.
 */
public class DataContextBinaryPKInListIT {

    private static final byte[] KEY1 = {1, 2, 3};
    private static final byte[] KEY2 = {4, 5, 6};
    private static final byte[] KEY3 = {7, 8, 9};

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.BINARY_PK_PROJECT);

    @BeforeEach
    public void createTestData() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        TableHelper tMaster = env.table("BINARY_PK_TEST1").setColumns("BIN_ID", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.VARCHAR);
        tMaster.insert(KEY1, "master1");
        tMaster.insert(KEY2, "master2");
        tMaster.insert(KEY3, "master3");

        TableHelper tDetail = env.table("BINARY_PK_TEST2").setColumns("ID", "FK_ID", "DETAIL_NAME")
                .setColumnTypes(Types.INTEGER, Types.VARBINARY, Types.VARCHAR);
        tDetail.insert(1, KEY1, "detail1");
        tDetail.insert(2, KEY2, "detail2");
        tDetail.insert(3, KEY2, "detail3");
    }

    @Test
    public void inDbExp() {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .where(ExpressionFactory.inDbExp(BinaryPKTest1.BIN_ID_PK_COLUMN, KEY1, KEY3))
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());

        assertEquals(2, masters.size());
        assertEquals("master1", masters.get(0).getName());
        assertEquals("master3", masters.get(1).getName());
    }

    @Test
    public void disjointByIdPrefetchToMany() {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .prefetch(BinaryPKTest1.BINARY_PKDETAILS.disjointById())
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertEquals(3, masters.size());

        env.runWithQueriesBlocked(() -> {
            assertEquals(1, masters.get(0).getBinaryPKDetails().size());
            assertEquals(2, masters.get(1).getBinaryPKDetails().size());
            assertEquals(0, masters.get(2).getBinaryPKDetails().size());
        });
    }

    @Test
    public void disjointByIdPrefetchToOne() {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        DataContext context = env.context();
        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.TO_BINARY_PKMASTER.disjointById())
                .select(context);
        assertEquals(3, details.size());

        // the prefetch query must have found both masters by their keys
        assertFetchedMaster(context, KEY1, "master1");
        assertFetchedMaster(context, KEY2, "master2");
    }

    private static void assertFetchedMaster(DataContext context, byte[] key, String expectedName) {
        Persistent object = context.getObjectStore().getObject(ObjectId.of("BinaryPKTest1", "BIN_ID", key));
        BinaryPKTest1 master = assertInstanceOf(BinaryPKTest1.class, object);
        assertEquals(PersistenceState.COMMITTED, master.getPersistenceState());
        assertEquals(expectedName, master.readPropertyDirectly("name"));
    }
}
