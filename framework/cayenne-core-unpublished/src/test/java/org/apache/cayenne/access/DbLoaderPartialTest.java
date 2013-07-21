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

import java.util.Collection;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DbLoaderPartialTest extends ServerCase {

    @Inject
    private DbAdapter adapter;

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    private DbLoader loader;

    @Override
    protected void setUpAfterInjection() throws Exception {
        loader = new DbLoader(
                dataSourceFactory.getSharedDataSource().getConnection(),
                adapter,
                new DbLoaderDelegate() {

                    public boolean overwriteDbEntity(DbEntity ent)
                            throws CayenneException {
                        if (ent.getName().equalsIgnoreCase("ARTIST")
                                || ent.getName().equalsIgnoreCase("PAINTING")) {
                            return false;
                        }
                        return true;
                    }

                    public void dbEntityAdded(DbEntity ent) {
                    }

                    public void dbEntityRemoved(DbEntity ent) {
                    }

                    public void objEntityAdded(ObjEntity ent) {
                    }

                    public void objEntityRemoved(ObjEntity ent) {
                    }

                });
    }

    @Override
    protected void tearDownBeforeInjection() throws Exception {
        loader.getConnection().close();
    }

    /**
     * Tests that FKs are properly loaded when the relationship source is not loaded. See
     * CAY-479. This test will perform two reverse engineers. The second reverse engineer
     * will skip two tables that share relationships with PAINTING. Relationships in
     * ARTIST and GALLERY should remain unmodified, and all PAINTING relationships should
     * be loaded.
     */
    public void testPartialLoad() throws Exception {

        DataMap map = new DataMap();
        String tableLabel = adapter.tableTypeForTable();

        loader.loadDataMapFromDB(null, "%", new String[] {
            tableLabel
        }, map);

        Collection<?> rels = getDbEntity(map, "ARTIST").getRelationships();
        assertNotNull(rels);
        int artistRels = rels.size();

        rels = getDbEntity(map, "GALLERY").getRelationships();
        assertNotNull(rels);
        int galleryRels = rels.size();

        rels = getDbEntity(map, "PAINTING").getRelationships();
        assertNotNull(rels);
        int paintingRels = rels.size();

        loader.loadDataMapFromDB(null, "%", new String[] {
            tableLabel
        }, map);

        rels = getDbEntity(map, "ARTIST").getRelationships();
        assertNotNull(rels);
        assertEquals(artistRels, rels.size());

        rels = getDbEntity(map, "GALLERY").getRelationships();
        assertNotNull(rels);
        assertEquals(galleryRels, rels.size());

        rels = getDbEntity(map, "PAINTING").getRelationships();
        assertNotNull(rels);
        assertEquals(paintingRels, rels.size());
    }

    private DbEntity getDbEntity(DataMap map, String name) {
        DbEntity de = map.getDbEntity(name);
        // sometimes table names get converted to lowercase
        if (de == null) {
            de = map.getDbEntity(name.toLowerCase());
        }

        return de;
    }
}
