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
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.resolved.InsertBatch;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.DataNodes;
import org.apache.cayenne.access.translator.TranslatedBatch;
import org.apache.cayenne.access.translator.batch.InsertBatchTranslator;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class DataNodeBatchActionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void hasGeneratedKeys1() throws Exception {
        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        // test with adapter that supports keys
        JdbcAdapter adapter = buildAdapter(true);

        DataNode node = DataNodes.withAdapter(adapter, mock(RowReaderFactory.class));

        InsertBatch batch2 = new InsertBatch(resolver.getObjEntity(Artist.class).getDbEntity(), List.of());
        TranslatedBatch translated = new InsertBatchTranslator().translate(batch2, adapter);
        assertFalse(new DataNodeBatchAction(translated, node, false).hasGeneratedKeys());
    }

    @Test
    public void hasGeneratedKeys2() throws Exception {
        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        // test with adapter that does not support keys...
        JdbcAdapter adapter = buildAdapter(false);

        DataNode node = DataNodes.withAdapter(adapter, mock(RowReaderFactory.class));

        InsertBatch batch2 = new InsertBatch(resolver.getObjEntity(Artist.class).getDbEntity(), List.of());
        TranslatedBatch translated = new InsertBatchTranslator().translate(batch2, adapter);
        assertFalse(new DataNodeBatchAction(translated, node, false).hasGeneratedKeys());
    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = env.adhocObjectFactory().newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
