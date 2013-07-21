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

package org.apache.cayenne.conn;

import junit.framework.TestCase;

import org.apache.cayenne.util.Util;

public class DataSourceInfoTest extends TestCase {

    private DataSourceInfo dsi;

    @Override
    public void setUp() throws Exception {
        dsi = new DataSourceInfo();
        dsi.setUserName("a");
        dsi.setPassword("b");
        dsi.setMinConnections(1);
        dsi.setMaxConnections(2);
        dsi.setJdbcDriver("b");
        dsi.setDataSourceUrl("c");
        dsi.setAdapterClassName("d");
    }

    public void testDefaultValues() throws java.lang.Exception {
        DataSourceInfo localDsi = new DataSourceInfo();
        assertEquals(1, localDsi.getMinConnections());
        assertTrue(localDsi.getMinConnections() <= localDsi.getMaxConnections());
    }

    public void testClone() throws java.lang.Exception {
        DataSourceInfo dsiClone = dsi.cloneInfo();
        assertEquals(dsi, dsiClone);
        assertTrue(dsi != dsiClone);
    }

    public void testSerialize() throws java.lang.Exception {
        DataSourceInfo dsiUnserialized = Util.cloneViaSerialization(dsi);
        assertEquals(dsi, dsiUnserialized);
        assertTrue(dsi != dsiUnserialized);
    }

}
