/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.jdbc;

import static org.mockito.Mockito.mock;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.GeneratedColumnTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class BatchActionTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private AdhocObjectFactory objectFactory;

    public void testHasGeneratedKeys1() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that supports keys
        JdbcAdapter adapter = buildAdapter(true);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);
        assertTrue(new BatchAction(batch1, adapter, resolver, mock(RowReaderFactory.class)).hasGeneratedKeys());

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.getObjEntity(Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, adapter, resolver, mock(RowReaderFactory.class)).hasGeneratedKeys());
    }

    public void testHasGeneratedKeys2() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that does not support keys...
        JdbcAdapter adapter = buildAdapter(false);

        InsertBatchQuery batch1 = new InsertBatchQuery(resolver.getObjEntity(GeneratedColumnTestEntity.class)
                .getDbEntity(), 5);
        assertFalse(new BatchAction(batch1, adapter, resolver, mock(RowReaderFactory.class)).hasGeneratedKeys());

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.getObjEntity(Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, adapter, resolver, mock(RowReaderFactory.class)).hasGeneratedKeys());
    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
