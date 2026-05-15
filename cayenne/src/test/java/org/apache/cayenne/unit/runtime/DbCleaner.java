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

package org.apache.cayenne.unit.runtime;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.unit.AllTestsSchemaManager;

import java.sql.SQLException;
import java.util.Set;

/**
 * Cleans up test data in the scope of a given test DataMaps.
 */
public class DbCleaner {

    private final DbHelper dbHelper;
    private final AllTestsSchemaManager parentSchemaManager;
    private final Set<String> dataMaps;

    public DbCleaner(AllTestsSchemaManager parentSchemaManager, DbHelper dbHelper, Set<String> dataMaps) {
        this.parentSchemaManager = parentSchemaManager;
        this.dbHelper = dbHelper;
        this.dataMaps = dataMaps;
    }

    public void clean() {
        for (String map : dataMaps) {
            for (DbEntity entity : parentSchemaManager.dbEntitiesInDeleteOrder(map)) {
                try {
                    dbHelper.deleteAll(entity.getName());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
