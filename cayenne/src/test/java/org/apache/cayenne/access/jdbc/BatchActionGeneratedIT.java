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

package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.StatementContext;
import org.apache.cayenne.access.StatementContexts;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.testdo.generated.GeneratedColumnTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BatchActionGeneratedIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.GENERATED_PROJECT);

    @Test
    public void hasGeneratedKeys1() throws Exception {
        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        // test with adapter that supports keys
        JdbcAdapter adapter = buildAdapter(true);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        assertTrue(new BatchAction(batch1, node, false).hasGeneratedKeys());
    }

    @Test
    public void hasGeneratedKeys2() {
        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        // test with adapter that does not support keys...
        JdbcAdapter adapter = buildAdapter(false);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        assertTrue(new BatchAction(batch1, node, false).hasGeneratedKeys());
        assertFalse(new BatchAction(batch1, node, false).supportsGeneratedKeys(true));

    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = env.adhocObjectFactory().newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }

    @Test
    public void generatedKeysReportedAndAppliedToTemporaryIds() {
        DataNode node = env.dataNode();
        Assumptions.assumeTrue(node.getAdapter().supportsGeneratedKeys(), "generated keys unsupported");

        DbEntity entity = node.getEntityResolver().getObjEntity(GeneratedColumnTestEntity.class).getDbEntity();
        InsertBatchQuery batch = new InsertBatchQuery(entity, 2);
        ObjectId id1 = ObjectId.of("GeneratedColumnTestEntity");
        ObjectId id2 = ObjectId.of("GeneratedColumnTestEntity");
        batch.add(new HashMap<>(Map.of("NAME", "g1")), id1);
        batch.add(new HashMap<>(Map.of("NAME", "g2")), id2);

        StatementContext statementContext = StatementContexts.run(node, batch);

        // whether the rows ran as a batch or one by one, the statement reports update counts and the keys as DataRows
        List<DataRow> keys = new ArrayList<>();
        int updated = 0;
        for (QueryResult result : statementContext.getResults()) {
            switch (result) {
                case SelectResult<?> select -> select.objects().forEach(o -> keys.add((DataRow) o));
                case UpdateResult(int[] counts) -> {
                    for (int c : counts) {
                        updated += c;
                    }
                }
                default -> throw new AssertionError("Unexpected result: " + result);
            }
        }
        assertEquals(2, updated);
        assertEquals(2, keys.size());

        // the keys are stored in the temporary ids right away, as later rows may reference them via their FKs
        assertTrue(id1.isReplacementIdAttached());
        assertTrue(id2.isReplacementIdAttached());
        Object pk1 = id1.getReplacementIdMap().get("GENERATED_COLUMN");
        Object pk2 = id2.getReplacementIdMap().get("GENERATED_COLUMN");
        assertNotNull(pk1);
        assertNotNull(pk2);
        assertEquals(keys.get(0).values().iterator().next().toString(), pk1.toString());
        assertEquals(keys.get(1).values().iterator().next().toString(), pk2.toString());
    }
}
