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

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.map.EntityResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataNodeTest {

    @Test
    public void name() {
        DataNode node = new DataNode();
        assertNull(node.getName());

        node.setName("tst_name");
        assertEquals("tst_name", node.getName());
    }

    @Test
    public void dataSourceFactory() {
        DataNode node = new DataNode();
        assertNull(node.getDataSourceFactory());

        node.setDataSourceFactory("tst_name");
        assertEquals("tst_name", node.getDataSourceFactory());
    }

    @Test
    public void entityResolver() {
        DataNode node = new DataNode();
        assertNull(node.getEntityResolver());

        EntityResolver resolver = new EntityResolver();
        node.setEntityResolver(resolver);
        assertSame(resolver, node.getEntityResolver());
    }

    @Test
    public void adapter() {
        DataNode node = new DataNode();
        assertNull(node.getAdapter());

        DbAdapter a1 = mock(DbAdapter.class);
        node.setAdapter(a1);
        assertSame(a1, node.getAdapter());

        DbAdapter a2 = mock(DbAdapter.class);
        node.setAdapter(a2);
        assertSame(a2, node.getAdapter());
    }

    @Test
    public void pkGeneratorFollowsAdapter() {
        DataNode node = new DataNode();
        assertNull(node.getPkGenerator());

        PkGenerator g1 = mock(PkGenerator.class);
        node.setAdapter(adapterWithPkGenerator(g1));
        assertSame(g1, node.getPkGenerator());

        PkGenerator g2 = mock(PkGenerator.class);
        node.setAdapter(adapterWithPkGenerator(g2));
        assertSame(g2, node.getPkGenerator());
    }

    @Test
    public void pkGeneratorCustomRetainedAcrossAdapterChange() {
        DataNode node = new DataNode();
        node.setAdapter(adapterWithPkGenerator(mock(PkGenerator.class)));

        PkGenerator custom = mock(PkGenerator.class);
        node.setPkGenerator(custom);
        assertSame(custom, node.getPkGenerator());

        PkGenerator adapterDefault = mock(PkGenerator.class);
        node.setAdapter(adapterWithPkGenerator(adapterDefault));
        assertSame(custom, node.getPkGenerator());

        // null restores the adapter default
        node.setPkGenerator(null);
        assertSame(adapterDefault, node.getPkGenerator());
    }

    private DbAdapter adapterWithPkGenerator(PkGenerator pkGenerator) {
        DbAdapter adapter = mock(DbAdapter.class);
        when(adapter.createPkGenerator()).thenReturn(pkGenerator);
        return adapter;
    }
}
