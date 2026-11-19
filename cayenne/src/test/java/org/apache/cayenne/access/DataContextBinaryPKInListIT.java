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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.binary_pk.BinaryPKCompound;
import org.apache.cayenne.testdo.binary_pk.BinaryPKCompoundDetail;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Qualifiers with binary key values: IN lists, either explicit or built by "disjoint by id" prefetches, and compound
 * key matches.
 */
public class DataContextBinaryPKInListIT {

    private static final byte[] KEY1 = {1, 2, 3};
    private static final byte[] KEY2 = {4, 5, 6};
    private static final byte[] KEY3 = {7, 8, 9};

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
        tMaster.insert(KEY1, "master1");
        tMaster.insert(KEY2, "master2");
        tMaster.insert(KEY3, "master3");

        TableHelper tDetail = env.table("BINARY_PK_TEST2").setColumns("ID", "FK_ID", "DETAIL_NAME")
                .setColumnTypes(Types.INTEGER, Types.VARBINARY, Types.VARCHAR);
        tDetail.insert(1, KEY1, "detail1");
        tDetail.insert(2, KEY2, "detail2");
        tDetail.insert(3, KEY2, "detail3");

        TableHelper tCompound = env.table("BINARY_PK_COMPOUND").setColumns("BIN_ID", "NUM", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.INTEGER, Types.VARCHAR);
        tCompound.insert(KEY1, 1, "c1");
        tCompound.insert(KEY1, 2, "c2");
        tCompound.insert(KEY2, 1, "c3");

        TableHelper tCompoundDetail = env.table("BINARY_PK_COMPOUND_DETAIL")
                .setColumns("ID", "MASTER_BIN_ID", "MASTER_NUM", "NAME")
                .setColumnTypes(Types.INTEGER, Types.VARBINARY, Types.INTEGER, Types.VARCHAR);
        tCompoundDetail.insert(1, KEY1, 1, "d1");
        tCompoundDetail.insert(2, KEY1, 1, "d2");
        tCompoundDetail.insert(3, KEY1, 2, "d3");
        tCompoundDetail.insert(4, KEY2, 1, "d4");
    }

    @Test
    public void inDbExp() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(ExpressionFactory.inDbExp(BinaryPKTest1.BIN_ID_PK_COLUMN, KEY1, KEY3)),
                "master1", "master3");
    }

    @Test
    public void notInDbExp() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(ExpressionFactory.notInDbExp(BinaryPKTest1.BIN_ID_PK_COLUMN, KEY1, KEY3)),
                "master2");
    }

    @Test
    public void idPropertyIn() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(BinaryPKTest1.BIN_ID_PK_PROPERTY.in(KEY1, KEY3)), "master1", "master3");
    }

    @Test
    public void selfIdsIn() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(BinaryPKTest1.SELF.idsIn(KEY1, KEY3)), "master1", "master3");
    }

    @Test
    public void selfIdsInObjectIds() {
        if (!supported()) {
            return;
        }

        Expression qualifier = BinaryPKTest1.SELF.idsIn(
                ObjectId.of("BinaryPKTest1", BinaryPKTest1.BIN_ID_PK_COLUMN, KEY1),
                ObjectId.of("BinaryPKTest1", BinaryPKTest1.BIN_ID_PK_COLUMN, KEY3));
        assertMasters(selectMasters(qualifier), "master1", "master3");
    }

    @Test
    public void selfInObjects() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> all = selectMasters(null);
        assertMasters(selectMasters(BinaryPKTest1.SELF.in(all.get(0), all.get(2))), "master1", "master3");
    }

    @Test
    public void relationshipInObjects() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = selectMasters(null);
        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .where(BinaryPKTest2.BINARY_PKMASTER.in(masters.get(1), masters.get(2)))
                .orderBy(BinaryPKTest2.DETAIL_NAME.asc())
                .select(env.context());

        assertEquals(List.of("detail2", "detail3"), details.stream().map(BinaryPKTest2::getDetailName).toList());
    }

    @Test
    public void parameterizedIn() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(ExpressionFactory.exp("db:BIN_ID in $ids", List.of(KEY1, KEY3))),
                "master1", "master3");
    }

    @Test
    public void parameterizedInSingleValue() {
        if (!supported()) {
            return;
        }

        assertMasters(selectMasters(ExpressionFactory.exp("db:BIN_ID in $id", (Object) KEY2)), "master2");
    }

    @Test
    public void compoundSelfEqObjectId() {
        if (!supported()) {
            return;
        }

        ObjectId id = ObjectId.of("BinaryPKCompound", Map.of("BIN_ID", KEY1, "NUM", 2));
        List<BinaryPKCompound> result = ObjectSelect.query(BinaryPKCompound.class)
                .where(BinaryPKCompound.SELF.eqId(id))
                .select(env.context());

        assertEquals(1, result.size());
        assertEquals("c2", result.get(0).getName());
    }

    @Test
    public void compoundRelationshipEq() {
        if (!supported()) {
            return;
        }

        BinaryPKCompound master = ObjectSelect.query(BinaryPKCompound.class)
                .where(BinaryPKCompound.NAME.eq("c1"))
                .selectOne(env.context());
        List<BinaryPKCompoundDetail> details = ObjectSelect.query(BinaryPKCompoundDetail.class)
                .where(BinaryPKCompoundDetail.MASTER.eq(master))
                .orderBy(BinaryPKCompoundDetail.NAME.asc())
                .select(env.context());

        assertEquals(List.of("d1", "d2"), details.stream().map(BinaryPKCompoundDetail::getName).toList());
    }

    @Test
    public void disjointByIdPrefetchToMany() {
        if (!supported()) {
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
        if (!supported()) {
            return;
        }

        DataContext context = env.context();
        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.BINARY_PKMASTER.disjointById())
                .select(context);
        assertEquals(3, details.size());

        // the prefetch query must have found both masters by their keys
        assertFetchedMaster(context, KEY1, "master1");
        assertFetchedMaster(context, KEY2, "master2");
    }

    private static List<BinaryPKTest1> selectMasters(Expression qualifier) {
        ObjectSelect<BinaryPKTest1> query = ObjectSelect.query(BinaryPKTest1.class).orderBy(BinaryPKTest1.NAME.asc());
        if (qualifier != null) {
            query.where(qualifier);
        }
        return query.select(env.context());
    }

    private static void assertMasters(List<BinaryPKTest1> masters, String... expectedNames) {
        assertEquals(List.of(expectedNames), masters.stream().map(BinaryPKTest1::getName).toList());
    }

    private static void assertFetchedMaster(DataContext context, byte[] key, String expectedName) {
        Persistent object = context.getObjectStore().getObject(ObjectId.of("BinaryPKTest1", "BIN_ID", key));
        BinaryPKTest1 master = assertInstanceOf(BinaryPKTest1.class, object);
        assertEquals(PersistenceState.COMMITTED, master.getPersistenceState());
        assertEquals(expectedName, master.readPropertyDirectly("name"));
    }
}
