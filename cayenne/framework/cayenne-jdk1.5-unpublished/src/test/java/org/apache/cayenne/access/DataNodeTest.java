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


package org.apache.cayenne.access;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.unit.BasicCase;

public class DataNodeTest extends BasicCase {

    public void testName() throws Exception {
        String tstName = "tst_name";
        DataNode node = new DataNode();
        assertNull(node.getName());
        node.setName(tstName);
        assertEquals(tstName, node.getName());
    }

    public void testDataSourceLocation() throws Exception {
        String tstName = "tst_name";
        DataNode node = new DataNode();
        assertNull(node.getDataSourceLocation());
        node.setDataSourceLocation(tstName);
        assertEquals(tstName, node.getDataSourceLocation());
    }

    public void testDataSourceFactory() throws Exception {
        String tstName = "tst_name";
        DataNode node = new DataNode();
        assertNull(node.getDataSourceFactory());
        node.setDataSourceFactory(tstName);
        assertEquals(tstName, node.getDataSourceFactory());
    }

    public void testNodeEntityResolver() {
        DataNode node = new DataNode();
        assertNull(node.getEntityResolver());

        org.apache.cayenne.map.EntityResolver resolver = new org.apache.cayenne.map.EntityResolver();
        node.setEntityResolver(resolver);
        assertSame(resolver, node.getEntityResolver());
    }

    public void testAdapter() throws Exception {
        DataNode node = new DataNode();

        // entity sorter should have been created ... and since 1.2 shouldn't change no
        // matter what adapter we use.
        EntitySorter sorter = node.getEntitySorter();
        assertNotNull(sorter);
        assertNull(node.getAdapter());

        JdbcAdapter a1 = new JdbcAdapter();
        a1.setSupportsFkConstraints(true);
        node.setAdapter(a1);

        assertSame(a1, node.getAdapter());
        assertSame(sorter, node.getEntitySorter());

        JdbcAdapter a2 = new JdbcAdapter();
        a2.setSupportsFkConstraints(false);
        node.setAdapter(a2);

        assertSame(a2, node.getAdapter());
        assertSame(sorter, node.getEntitySorter());

        // flip FK flag and reset the same adapter, see if sorter has changed
        a2.setSupportsFkConstraints(true);
        node.setAdapter(a2);
        assertSame(sorter, node.getEntitySorter());
    }
}
