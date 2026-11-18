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

import org.apache.cayenne.PersistenceState;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Prefetches over single and compound binary keys. "disjoint by id" IN qualifiers over binary keys are CAY-3079.
 */
public class DataContextBinaryPKPrefetchIT {

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
    public void toOneJoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.TO_BINARY_PKMASTER.joint())
                .orderBy(BinaryPKTest2.DETAIL_NAME.asc())
                .select(env.context());
        assertToOneMasters(details);
    }

    @Test
    public void toOneDisjoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.TO_BINARY_PKMASTER.disjoint())
                .orderBy(BinaryPKTest2.DETAIL_NAME.asc())
                .select(env.context());
        assertToOneMasters(details);
    }

    @Test
    public void toOneDisjointByIdRepeatedKey() {
        if (!supported()) {
            return;
        }

        // one master key, collected once: "=" and not IN
        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .where(BinaryPKTest2.DETAIL_NAME.in("detail2", "detail3"))
                .prefetch(BinaryPKTest2.TO_BINARY_PKMASTER.disjointById())
                .orderBy(BinaryPKTest2.DETAIL_NAME.asc())
                .select(env.context());
        assertEquals(2, details.size());

        env.runWithQueriesBlocked(() -> {
            assertMaster("master2", details.get(0));
            assertMaster("master2", details.get(1));
        });
    }

    @Test
    public void toManyJoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .prefetch(BinaryPKTest1.BINARY_PKDETAILS.joint())
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertToManyDetails(masters);
    }

    @Test
    public void toManyDisjoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .prefetch(BinaryPKTest1.BINARY_PKDETAILS.disjoint())
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertToManyDetails(masters);
    }

    @Test
    public void toManyDisjointByIdSingleMaster() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .where(BinaryPKTest1.NAME.eq("master2"))
                .prefetch(BinaryPKTest1.BINARY_PKDETAILS.disjointById())
                .select(env.context());
        assertEquals(1, masters.size());

        env.runWithQueriesBlocked(() -> assertEquals(2, masters.get(0).getBinaryPKDetails().size()));
    }

    @Test
    public void compoundToOneJoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompoundDetail> details = ObjectSelect.query(BinaryPKCompoundDetail.class)
                .prefetch(BinaryPKCompoundDetail.TO_MASTER.joint())
                .orderBy(BinaryPKCompoundDetail.NAME.asc())
                .select(env.context());
        assertCompoundToOneMasters(details);
    }

    @Test
    public void compoundToOneDisjoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompoundDetail> details = ObjectSelect.query(BinaryPKCompoundDetail.class)
                .prefetch(BinaryPKCompoundDetail.TO_MASTER.disjoint())
                .orderBy(BinaryPKCompoundDetail.NAME.asc())
                .select(env.context());
        assertCompoundToOneMasters(details);
    }

    @Test
    public void compoundToOneDisjointById() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompoundDetail> details = ObjectSelect.query(BinaryPKCompoundDetail.class)
                .prefetch(BinaryPKCompoundDetail.TO_MASTER.disjointById())
                .orderBy(BinaryPKCompoundDetail.NAME.asc())
                .select(env.context());
        assertCompoundToOneMasters(details);
    }

    @Test
    public void compoundToManyJoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompound> masters = ObjectSelect.query(BinaryPKCompound.class)
                .prefetch(BinaryPKCompound.DETAILS.joint())
                .orderBy(BinaryPKCompound.NAME.asc())
                .select(env.context());
        assertCompoundToManyDetails(masters);
    }

    @Test
    public void compoundToManyDisjoint() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompound> masters = ObjectSelect.query(BinaryPKCompound.class)
                .prefetch(BinaryPKCompound.DETAILS.disjoint())
                .orderBy(BinaryPKCompound.NAME.asc())
                .select(env.context());
        assertCompoundToManyDetails(masters);
    }

    @Test
    public void compoundToManyDisjointById() {
        if (!supported()) {
            return;
        }

        List<BinaryPKCompound> masters = ObjectSelect.query(BinaryPKCompound.class)
                .prefetch(BinaryPKCompound.DETAILS.disjointById())
                .orderBy(BinaryPKCompound.NAME.asc())
                .select(env.context());
        assertCompoundToManyDetails(masters);
    }

    private static void assertToOneMasters(List<BinaryPKTest2> details) {
        assertEquals(3, details.size());
        env.runWithQueriesBlocked(() -> {
            assertMaster("master1", details.get(0));
            assertMaster("master2", details.get(1));
            assertMaster("master2", details.get(2));
        });
    }

    private static void assertMaster(String expectedName, BinaryPKTest2 detail) {
        BinaryPKTest1 master = assertInstanceOf(BinaryPKTest1.class, detail.readPropertyDirectly("toBinaryPKMaster"));
        assertEquals(PersistenceState.COMMITTED, master.getPersistenceState());
        assertEquals(expectedName, master.getName());
    }

    private static void assertToManyDetails(List<BinaryPKTest1> masters) {
        assertEquals(3, masters.size());
        env.runWithQueriesBlocked(() -> {
            assertEquals(1, masters.get(0).getBinaryPKDetails().size());
            assertEquals(2, masters.get(1).getBinaryPKDetails().size());
            assertEquals(0, masters.get(2).getBinaryPKDetails().size());
        });
    }

    private static void assertCompoundToOneMasters(List<BinaryPKCompoundDetail> details) {
        assertEquals(4, details.size());
        env.runWithQueriesBlocked(() -> {
            assertCompoundMaster("c1", details.get(0));
            assertCompoundMaster("c1", details.get(1));
            assertCompoundMaster("c2", details.get(2));
            assertCompoundMaster("c3", details.get(3));
        });
    }

    private static void assertCompoundMaster(String expectedName, BinaryPKCompoundDetail detail) {
        BinaryPKCompound master = assertInstanceOf(BinaryPKCompound.class, detail.readPropertyDirectly("toMaster"));
        assertEquals(PersistenceState.COMMITTED, master.getPersistenceState());
        assertEquals(expectedName, master.getName());
    }

    private static void assertCompoundToManyDetails(List<BinaryPKCompound> masters) {
        assertEquals(3, masters.size());
        env.runWithQueriesBlocked(() -> {
            assertEquals(2, masters.get(0).getDetails().size());
            assertEquals(1, masters.get(1).getDetails().size());
            assertEquals(1, masters.get(2).getDetails().size());
        });
    }
}
