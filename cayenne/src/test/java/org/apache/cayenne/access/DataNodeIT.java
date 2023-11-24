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

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataNodeIT extends RuntimeCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testName() throws Exception {
        String tstName = "tst_name";
        DataNode node = new DataNode();
        assertNull(node.getName());
        node.setName(tstName);
        assertEquals(tstName, node.getName());
    }

    @Test
    public void testDataSourceFactory() throws Exception {
        String tstName = "tst_name";
        DataNode node = new DataNode();
        assertNull(node.getDataSourceFactory());
        node.setDataSourceFactory(tstName);
        assertEquals(tstName, node.getDataSourceFactory());
    }

    @Test
    public void testNodeEntityResolver() {
        DataNode node = new DataNode();
        assertNull(node.getEntityResolver());

        org.apache.cayenne.map.EntityResolver resolver = new org.apache.cayenne.map.EntityResolver();
        node.setEntityResolver(resolver);
        assertSame(resolver, node.getEntityResolver());
    }

    @Test
    public void testAdapter() throws Exception {
        DataNode node = new DataNode();

        assertNull(node.getAdapter());

        JdbcAdapter a1 = objectFactory.newInstance(
                JdbcAdapter.class, 
                JdbcAdapter.class.getName());
        node.setAdapter(a1);

        assertSame(a1, node.getAdapter());

        JdbcAdapter a2 = objectFactory.newInstance(
                JdbcAdapter.class, 
                JdbcAdapter.class.getName());
        node.setAdapter(a2);

        assertSame(a2, node.getAdapter());
    }
}
