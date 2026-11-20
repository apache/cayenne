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
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest1;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest2;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DataContextBinaryPKIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.BINARY_PK_PROJECT);

    @Test
    public void insertBinaryPK() {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        DataContext context = env.context();
        BinaryPKTest1 master = (BinaryPKTest1) context.newObject("BinaryPKTest1");
        master.setName("master1");

        BinaryPKTest2 detail = (BinaryPKTest2) context.newObject("BinaryPKTest2");
        detail.setDetailName("detail2");

        master.addToBinaryPKDetails(detail);

        context.commitChanges();
    }

    @Test
    public void fetchRelationshipBinaryPK() {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        DataContext context = env.context();
        BinaryPKTest1 master = (BinaryPKTest1) context.newObject("BinaryPKTest1");
        master.setName("master1");

        BinaryPKTest2 detail = (BinaryPKTest2) context.newObject("BinaryPKTest2");
        detail.setDetailName("detail2");

        master.addToBinaryPKDetails(detail);

        context.commitChanges();
        context.invalidateObjects(master, detail);

        BinaryPKTest2 fetchedDetail = ObjectSelect.query(BinaryPKTest2.class).selectFirst(context);
        assertNotNull(fetchedDetail.readPropertyDirectly("binaryPKMaster"));

        BinaryPKTest1 fetchedMaster = fetchedDetail.getBinaryPKMaster();
        assertNotNull(fetchedMaster);
        assertEquals(PersistenceState.HOLLOW, fetchedMaster.getPersistenceState());
        assertEquals("master1", fetchedMaster.getName());
    }

    @Test
    public void disjointPrefetchToOneBinaryPK() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        TableHelper tMaster = masterTable();
        TableHelper tDetail = detailTable();
        tMaster.insert(new byte[]{1, 2, 3}, "master1");
        tMaster.insert(new byte[]{4, 5, 6}, "master2");
        tDetail.insert(1, new byte[]{1, 2, 3}, "detail1");
        tDetail.insert(2, new byte[]{4, 5, 6}, "detail2");

        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.BINARY_PKMASTER.disjoint())
                .orderBy(BinaryPKTest2.DETAIL_NAME.asc())
                .select(env.context());
        assertEquals(2, details.size());

        env.runWithQueriesBlocked(() -> {
            assertPrefetchedMaster("master1", details.get(0));
            assertPrefetchedMaster("master2", details.get(1));
        });
    }

    @Test
    public void disjointByIdPrefetchToOneBinaryPK() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        // a single key, so that the prefetch query is a plain "=" and not an IN list
        masterTable().insert(new byte[]{1, 2, 3}, "master1");
        detailTable().insert(1, new byte[]{1, 2, 3}, "detail1");

        List<BinaryPKTest2> details = ObjectSelect.query(BinaryPKTest2.class)
                .prefetch(BinaryPKTest2.BINARY_PKMASTER.disjointById())
                .select(env.context());
        assertEquals(1, details.size());

        env.runWithQueriesBlocked(() -> assertPrefetchedMaster("master1", details.get(0)));
    }

    @Test
    public void localCacheBinaryQualifier() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        masterTable().insert(new byte[]{1, 2, 3}, "master1");
        masterTable().insert(new byte[]{4, 5, 6}, "master2");

        DataContext context = env.context();
        BinaryPKTest1 master = selectMasterCached(context, new byte[]{4, 5, 6});
        assertEquals("master2", master.getName());

        env.runWithQueriesBlocked(() -> assertSame(master, selectMasterCached(context, new byte[]{4, 5, 6})));
    }

    private static BinaryPKTest1 selectMasterCached(DataContext context, byte[] key) {
        return ObjectSelect.query(BinaryPKTest1.class)
                .where(ExpressionFactory.matchDbExp(BinaryPKTest1.BIN_ID_PK_COLUMN, key))
                .localCache()
                .selectOne(context);
    }

    @Test
    public void byIdBinaryPK() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        masterTable().insert(new byte[]{1, 2, 3}, "master1");
        masterTable().insert(new byte[]{4, 5, 6}, "master2");

        BinaryPKTest1 master = ObjectSelect.query(BinaryPKTest1.class)
                .byId(new byte[]{4, 5, 6})
                .selectOne(env.context());
        assertEquals("master2", master.getName());
    }

    @Test
    public void byIdsBinaryPK() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        masterTable().insert(new byte[]{1, 2, 3}, "master1");
        masterTable().insert(new byte[]{4, 5, 6}, "master2");
        masterTable().insert(new byte[]{7, 8, 9}, "master3");

        List<BinaryPKTest1> scalars = ObjectSelect.query(BinaryPKTest1.class)
                .byIds(new byte[]{1, 2, 3}, new byte[]{7, 8, 9})
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertEquals(List.of("master1", "master3"), scalars.stream().map(BinaryPKTest1::getName).toList());

        List<BinaryPKTest1> ids = ObjectSelect.query(BinaryPKTest1.class)
                .byIds(ObjectId.of("BinaryPKTest1", "BIN_ID", new byte[]{4, 5, 6}), new byte[]{7, 8, 9})
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertEquals(List.of("master2", "master3"), ids.stream().map(BinaryPKTest1::getName).toList());
    }

    @Test
    public void snapshotsFetchedWithOneQuery() throws SQLException {
        if (!env.testDbAdapter().supportsBinaryPK()) {
            return;
        }

        masterTable().insert(new byte[]{1, 2, 3}, "master1");
        masterTable().insert(new byte[]{4, 5, 6}, "master2");
        masterTable().insert(new byte[]{7, 8, 9}, "master3");

        List<ObjectId> ids = List.of(
                ObjectId.of("BinaryPKTest1", "BIN_ID", new byte[]{1, 2, 3}),
                ObjectId.of("BinaryPKTest1", "BIN_ID", new byte[]{4, 5, 6}),
                ObjectId.of("BinaryPKTest1", "BIN_ID", new byte[]{7, 8, 9}));

        DataContext context = env.context();
        Map<ObjectId, DataRow> snapshots = new HashMap<>();
        int queries = env.runWithQueryCounter(() -> snapshots.putAll(context.getObjectStore().getSnapshots(ids)));

        assertEquals(1, queries);
        assertEquals(3, snapshots.size());
        assertEquals("master2", snapshots.get(ids.get(1)).get("NAME"));
    }

    private TableHelper masterTable() {
        return env.table("BINARY_PK_TEST1").setColumns("BIN_ID", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.VARCHAR);
    }

    private TableHelper detailTable() {
        return env.table("BINARY_PK_TEST2").setColumns("ID", "FK_ID", "DETAIL_NAME")
                .setColumnTypes(Types.INTEGER, Types.VARBINARY, Types.VARCHAR);
    }

    private static void assertPrefetchedMaster(String expectedName, BinaryPKTest2 detail) {
        BinaryPKTest1 master = assertInstanceOf(BinaryPKTest1.class, detail.readPropertyDirectly("binaryPKMaster"));
        assertEquals(PersistenceState.COMMITTED, master.getPersistenceState());
        assertEquals(expectedName, master.getName());
    }
}
