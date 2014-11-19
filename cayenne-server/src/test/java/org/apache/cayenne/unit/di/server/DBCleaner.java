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

package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.unit.UnitDbAdapter;

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DBCleaner {

    private FlavoredDBHelper dbHelper;
    private String location;
    private DataDomain domain;
    private XMLDataChannelDescriptorLoader loader;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private Injector injector;

    public DBCleaner(FlavoredDBHelper dbHelper, DataDomain dataDomain, String location) {
        this.dbHelper = dbHelper;
        this.location = location;
        this.domain = dataDomain;
    }

    public void clean() throws SQLException {
        if (location.equals(CayenneProjects.BINARY_PK_PROJECT)) {
            if (accessStackAdapter.supportsBinaryPK()) {
                dbHelper.deleteAll("BINARY_PK_TEST2");
                dbHelper.deleteAll("BINARY_PK_TEST1");
            }
        } else if (location.equals(CayenneProjects.EMPTY_PROJECT)) {
            return;
        } else if (location.equals(CayenneProjects.LOB_PROJECT)) {
            dbHelper.deleteAll("CLOB_TEST_RELATION");
            if (accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("BLOB_TEST");
                dbHelper.deleteAll("CLOB_TEST");
            }
            dbHelper.deleteAll("TEST");
        } else if (location.equals(CayenneProjects.MISC_TYPES_PROJECT)) {
            if (accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("SERIALIZABLE_ENTITY");
            }
            dbHelper.deleteAll("ARRAYS_ENTITY");
            dbHelper.deleteAll("CHARACTER_ENTITY");
        } else if (location.equals(CayenneProjects.RETURN_TYPES_PROJECT)) {
            if (accessStackAdapter.supportsLobs()) {
                dbHelper.deleteAll("TYPES_MAPPING_LOBS_TEST1");
                dbHelper.deleteAll("TYPES_MAPPING_TEST2");
            }
            dbHelper.deleteAll("TYPES_MAPPING_TEST1");
        } else {
            loader = new XMLDataChannelDescriptorLoader();
            injector.injectMembers(loader);

            URL url = getClass().getClassLoader().getResource(location);
            ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

            for (DataMap map : tree.getRootNode().getDataMaps()) {
                DataMap dataMap = domain.getDataMap(map.getName());
                List<DbEntity> entities = new ArrayList<DbEntity>(dataMap.getDbEntities());
                domain.getEntitySorter().sortDbEntities(entities, true);

                for (DbEntity entity : entities) {
                    dbHelper.deleteAll(entity.getName());
                }
            }
        }
    }
}
