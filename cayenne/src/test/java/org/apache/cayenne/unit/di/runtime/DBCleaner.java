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

package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.xml.XMLDataChannelDescriptorLoader;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.unit.UnitDbAdapter;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class DBCleaner {

    private FlavoredDBHelper dbHelper;
    private String location;

    @Inject
    private SchemaBuilder schemaBuilder;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private Injector injector;

    public DBCleaner(FlavoredDBHelper dbHelper, String location) {
        this.dbHelper = dbHelper;
        this.location = location;
    }

    public void clean() throws SQLException {
        XMLDataChannelDescriptorLoader loader = new XMLDataChannelDescriptorLoader();
        injector.injectMembers(loader);

        URL url = getClass().getClassLoader().getResource(location);
        ConfigurationTree<DataChannelDescriptor> tree = loader.load(new URLResource(url));

        for (DataMap map : tree.getRootNode().getDataMaps()) {
            List<DbEntity> entities = schemaBuilder.dbEntitiesInDeleteOrder(map);

            for (DbEntity entity : entities) {
                dbHelper.deleteAll(entity.getName());
            }
        }
    }
}
