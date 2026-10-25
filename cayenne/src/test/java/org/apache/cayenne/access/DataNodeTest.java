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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.unit.DataNodes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataNodeTest {

    private static DataNode node(DataNodeInitializer initializer, PkGenerator pkGenerator) {
        return new DataNode(null, null, initializer, pkGenerator, null, null, null, null, null, null, null, null);
    }

    @Test
    public void name() {
        assertNull(DataNodes.named(null).getName());
        assertEquals("tst_name", DataNodes.named("tst_name").getName());
    }

    @Test
    public void bareNode() {
        DataNode node = DataNodes.named(null);
        assertNull(node.getAdapter());
        assertNull(node.getPkGenerator());
        assertNull(node.getDataSource());
    }

    @Test
    public void initializer() {
        DbAdapter a1 = mock(DbAdapter.class);
        AtomicInteger initCount = new AtomicInteger();
        DataNode node = node(() -> {
            initCount.incrementAndGet();
            return a1;
        }, null);

        // not initialized until the adapter is requested, then initialized once and cached
        assertEquals(0, initCount.get());
        assertSame(a1, node.getAdapter());
        assertSame(a1, node.getAdapter());
        assertEquals(1, initCount.get());
    }

    @Test
    public void initializer_RetriedAfterFailure() {
        DbAdapter a1 = mock(DbAdapter.class);
        AtomicInteger initCount = new AtomicInteger();
        DataNode node = node(() -> {
            if (initCount.incrementAndGet() == 1) {
                throw new CayenneRuntimeException("DB is down");
            }
            return a1;
        }, null);

        assertThrows(CayenneRuntimeException.class, node::getAdapter);
        assertSame(a1, node.getAdapter());
        assertEquals(2, initCount.get());
    }

    @Test
    public void initializer_NoAdapter() {
        DataNode node = node(() -> null, null);
        assertThrows(CayenneRuntimeException.class, node::getAdapter);
    }

    @Test
    public void pkGeneratorFromAdapter() {
        PkGenerator generator = mock(PkGenerator.class);
        DataNode node = node(() -> adapterWithPkGenerator(generator), null);

        // requesting the generator resolves the adapter
        assertSame(generator, node.getPkGenerator());
    }

    @Test
    public void pkGeneratorCustom() {
        PkGenerator custom = mock(PkGenerator.class);
        DataNode node = node(() -> adapterWithPkGenerator(mock(PkGenerator.class)), custom);

        // the custom generator is used in place of the adapter default, before and after the adapter is resolved
        assertSame(custom, node.getPkGenerator());
        node.getAdapter();
        assertSame(custom, node.getPkGenerator());
    }

    private DbAdapter adapterWithPkGenerator(PkGenerator pkGenerator) {
        DbAdapter adapter = mock(DbAdapter.class);
        when(adapter.createPkGenerator()).thenReturn(pkGenerator);
        return adapter;
    }
}
