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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.generated.GeneratedColumnTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BatchActionGeneratedIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.GENERATED_PROJECT);

    private CayenneRuntime runtime;
    private AdhocObjectFactory objectFactory;


    @BeforeEach
    public void setUp() {
        runtime = env.runtime();
        objectFactory = env.getInstance(AdhocObjectFactory.class);
    }

    @Test
    public void hasGeneratedKeys1() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

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
    public void hasGeneratedKeys2() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

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
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
