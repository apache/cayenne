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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.SQLException;

import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DbEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class EntityLoaderIT extends BaseLoaderIT {

    @Test
    public void testGetTablesWithWrongCatalog() throws Exception {
        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(
                FiltersConfig.create("WRONG", null, TableFilter.everything(), PatternFilter.INCLUDE_NOTHING)
        );

        EntityLoader loader = new EntityLoader(adapter, config, new DefaultDbLoaderDelegate());
        try {
            loader.load(connection.getMetaData(), store);
        } catch (SQLException ex) {
            // SQL Server will throw exception here.
            assertTrue(ex.getMessage().contains("WRONG")); // just check that message is about "WRONG" catalog
        }

        assertTrue("Store is not empty", store.getDbEntities().isEmpty());
    }

    @Test
    public void testGetTablesWithWrongSchema() throws Exception {
        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(
                FiltersConfig.create(null, "WRONG", TableFilter.everything(), PatternFilter.INCLUDE_NOTHING)
        );

        EntityLoader loader = new EntityLoader(adapter, config, new DefaultDbLoaderDelegate());
        loader.load(connection.getMetaData(), store);

        assertTrue("Store is not empty", store.getDbEntities().isEmpty());
    }

    @Test
    public void testLoad() throws Exception {

        EntityLoader loader = new EntityLoader(adapter, EMPTY_CONFIG, new DefaultDbLoaderDelegate());
        loader.load(connection.getMetaData(), store);

        assertFalse("Store not empty", store.getDbEntities().isEmpty());
        assertDbEntities();

        if(accessStackAdapter.supportsLobs()) {
            assertLobDbEntities();
        }
    }

    private void assertDbEntities() {
        DbEntity dae = getDbEntity("ARTIST");
        assertNotNull("Null 'ARTIST' entity, other DbEntities: " + store.getDbEntityMap(), dae);
        assertEquals("ARTIST", dae.getName().toUpperCase());

        if (adapter.supportsGeneratedKeys()) {
            DbEntity bag = getDbEntity("GENERATED_COLUMN_TEST");
            assertNotNull("Null 'GENERATED_COLUMN_TEST' entity, other DbEntities: " + store.getDbEntityMap(), bag);
        }
    }

    private void assertLobDbEntities() {
        DbEntity blobEnt = getDbEntity("BLOB_TEST");
        assertNotNull(blobEnt);

        DbEntity clobEnt = getDbEntity("CLOB_TEST");
        assertNotNull(clobEnt);
    }


}
