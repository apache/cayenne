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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class DBCleaner {

    private final FlavoredDBHelper dbHelper;
    private final SchemaBuilder schemaBuilder;
    private final Collection<DataMap> dataMaps;

    public DBCleaner(FlavoredDBHelper dbHelper, SchemaBuilder schemaBuilder, Collection<DataMap> dataMaps) {
        this.dbHelper = dbHelper;
        this.schemaBuilder = schemaBuilder;
        this.dataMaps = dataMaps;
    }

    public void clean() throws SQLException {
        for (DataMap map : dataMaps) {
            List<DbEntity> entities = schemaBuilder.dbEntitiesInDeleteOrder(map);
            for (DbEntity entity : entities) {
                dbHelper.deleteAll(entity.getName());
            }
        }
    }
}
