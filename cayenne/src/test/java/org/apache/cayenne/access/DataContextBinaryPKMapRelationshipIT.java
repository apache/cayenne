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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest1;
import org.apache.cayenne.testdo.binary_pk.BinaryPKTest3;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A to-many Map relationship keyed by a binary PK.
 */
public class DataContextBinaryPKMapRelationshipIT {

    private static final byte[] MASTER1 = {1, 2, 3};
    private static final byte[] MASTER2 = {4, 5, 6};
    private static final byte[] CHILD1 = {11, 12};
    private static final byte[] CHILD2 = {13, 14};
    private static final byte[] CHILD3 = {15, 16};

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
        tMaster.insert(MASTER1, "master1");
        tMaster.insert(MASTER2, "master2");

        TableHelper tChild = env.table("BINARY_PK_TEST3").setColumns("BIN_ID", "MASTER_FK", "NAME")
                .setColumnTypes(Types.VARBINARY, Types.VARBINARY, Types.VARCHAR);
        tChild.insert(CHILD1, MASTER1, "child1");
        tChild.insert(CHILD2, MASTER1, "child2");
        tChild.insert(CHILD3, MASTER2, "child3");
    }

    @Test
    public void lookupByKey() {
        if (!supported()) {
            return;
        }

        BinaryPKTest1 master = ObjectSelect.query(BinaryPKTest1.class)
                .where(BinaryPKTest1.NAME.eq("master1"))
                .selectOne(env.context());

        Map<Object, BinaryPKTest3> children = master.getBinaryPKChildren();
        assertEquals(2, children.size());

        BinaryPKTest3 child1 = children.get(CHILD1.clone());
        assertNotNull(child1);
        assertEquals("child1", child1.getName());
        assertTrue(children.containsKey(CHILD2.clone()));
        assertFalse(children.containsKey(CHILD3.clone()));
        assertNull(children.get(new byte[]{11}));
    }

    @Test
    public void lookupByKeyPrefetched() {
        if (!supported()) {
            return;
        }

        List<BinaryPKTest1> masters = ObjectSelect.query(BinaryPKTest1.class)
                .prefetch(BinaryPKTest1.BINARY_PKCHILDREN.disjoint())
                .orderBy(BinaryPKTest1.NAME.asc())
                .select(env.context());
        assertEquals(2, masters.size());

        env.runWithQueriesBlocked(() -> {
            assertEquals("child2", masters.get(0).getBinaryPKChildren().get(CHILD2.clone()).getName());
            assertEquals("child3", masters.get(1).getBinaryPKChildren().get(CHILD3.clone()).getName());
        });
    }

    @Test
    public void removeByKey() {
        if (!supported()) {
            return;
        }

        BinaryPKTest1 master = ObjectSelect.query(BinaryPKTest1.class)
                .where(BinaryPKTest1.NAME.eq("master1"))
                .selectOne(env.context());

        Map<Object, BinaryPKTest3> children = master.getBinaryPKChildren();
        BinaryPKTest3 child1 = children.get(CHILD1.clone());

        assertSame(child1, children.remove(CHILD1.clone()));
        assertEquals(1, children.size());
        assertFalse(children.containsKey(CHILD1.clone()));
    }

    @Test
    public void newChildRemappedOnCommit() {
        if (!supported()) {
            return;
        }

        DataContext context = env.context();
        BinaryPKTest1 master = ObjectSelect.query(BinaryPKTest1.class)
                .where(BinaryPKTest1.NAME.eq("master2"))
                .selectOne(context);
        Map<Object, BinaryPKTest3> children = master.getBinaryPKChildren();

        BinaryPKTest3 child = context.newObject(BinaryPKTest3.class);
        child.setName("child4");
        child.setBinaryPKMaster(master);
        context.commitChanges();

        byte[] key = ((byte[]) Cayenne.pkForObject(child)).clone();
        assertEquals(2, children.size());
        assertSame(child, children.get(key));
    }
}
