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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class BatchActionIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testHasGeneratedKeys1() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that supports keys
        JdbcAdapter adapter = buildAdapter(true);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.getObjEntity(Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, node, false).hasGeneratedKeys());
    }

    @Test
    public void testHasGeneratedKeys2() throws Exception {
        EntityResolver resolver = runtime.getChannel().getEntityResolver();

        // test with adapter that does not support keys...
        JdbcAdapter adapter = buildAdapter(false);

        DataNode node = new DataNode();
        node.setAdapter(adapter);
        node.setEntityResolver(resolver);
        node.setRowReaderFactory(mock(RowReaderFactory.class));

        InsertBatchQuery batch2 = new InsertBatchQuery(resolver.getObjEntity(Artist.class).getDbEntity(), 5);
        assertFalse(new BatchAction(batch2, node, false).hasGeneratedKeys());
    }

    JdbcAdapter buildAdapter(boolean supportGeneratedKeys) {
        JdbcAdapter adapter = objectFactory.newInstance(JdbcAdapter.class, JdbcAdapter.class.getName());
        adapter.setSupportsGeneratedKeys(supportGeneratedKeys);
        return adapter;
    }
}
