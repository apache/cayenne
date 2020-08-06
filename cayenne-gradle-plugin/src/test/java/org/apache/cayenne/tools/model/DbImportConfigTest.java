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

package org.apache.cayenne.tools.model;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class DbImportConfigTest {

    @Test
    public void toEmptyReverseEngineering() throws Exception {
        DbImportConfig config = new DbImportConfig();
        ReverseEngineering rr = config.toReverseEngineering();

        assertNotNull(rr);

        assertEquals(0, rr.getCatalogs().size());
        assertEquals(0, rr.getSchemas().size());
        assertEquals(0, rr.getIncludeTables().size());
        assertEquals(0, rr.getExcludeTables().size());
        assertEquals(0, rr.getIncludeColumns().size());
        assertEquals(0, rr.getExcludeColumns().size());
        assertEquals(0, rr.getIncludeProcedures().size());
        assertEquals(0, rr.getExcludeProcedures().size());

        assertNull(rr.getDefaultPackage());
        assertNull(rr.getMeaningfulPkTables());
        assertEquals("org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator", rr.getNamingStrategy());
        assertFalse(rr.getSkipPrimaryKeyLoading());
        assertFalse(rr.getSkipRelationshipsLoading());
        assertEquals("", rr.getStripFromTableNames());
        assertArrayEquals(new String[0], rr.getTableTypes());

        assertFalse(rr.isForceDataMapCatalog());
        assertFalse(rr.isForceDataMapSchema());
        assertFalse(rr.isUseJava7Types());
        assertTrue(rr.isUsePrimitives());

        assertTrue(rr.isEmptyContainer());
    }

    @Test
    public void toReverseEngineering() throws Exception {
        DbImportConfig config = new DbImportConfig();

        config.catalog("catalog1");
        config.schema("schema1");

        config.setDefaultPackage("com.example.package");
        config.setMeaningfulPkTables("pk_tables");
        config.setNamingStrategy("com.example.naming");
        config.setSkipPrimaryKeyLoading(true);
        config.setSkipRelationshipsLoading(true);
        config.setStripFromTableNames("strip");
        config.tableType("table");
        config.tableTypes("view", "alias");

        config.setForceDataMapCatalog(true);
        config.setForceDataMapSchema(true);
        config.setUseJava7Types(true);
        config.setUsePrimitives(false);

        ReverseEngineering rr = config.toReverseEngineering();

        assertNotNull(rr);

        assertEquals(1, rr.getCatalogs().size());
        assertEquals("catalog1", rr.getCatalogs().iterator().next().getName());
        assertEquals(1, rr.getSchemas().size());
        assertEquals("schema1", rr.getSchemas().iterator().next().getName());
        assertEquals(0, rr.getIncludeTables().size());
        assertEquals(0, rr.getExcludeTables().size());
        assertEquals(0, rr.getIncludeColumns().size());
        assertEquals(0, rr.getExcludeColumns().size());
        assertEquals(0, rr.getIncludeProcedures().size());
        assertEquals(0, rr.getExcludeProcedures().size());

        assertEquals("com.example.package", rr.getDefaultPackage());
        assertEquals("pk_tables", rr.getMeaningfulPkTables());
        assertEquals("com.example.naming", rr.getNamingStrategy());
        assertTrue(rr.getSkipPrimaryKeyLoading());
        assertTrue(rr.getSkipRelationshipsLoading());
        assertEquals("strip", rr.getStripFromTableNames());
        assertArrayEquals(new String[]{"table", "view", "alias"}, rr.getTableTypes());

        assertTrue(rr.isForceDataMapCatalog());
        assertTrue(rr.isForceDataMapSchema());
        assertTrue(rr.isUseJava7Types());
        assertFalse(rr.isUsePrimitives());
    }

}
