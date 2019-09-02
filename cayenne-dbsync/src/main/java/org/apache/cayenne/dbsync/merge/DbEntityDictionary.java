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

package org.apache.cayenne.dbsync.merge;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

class DbEntityDictionary extends MergerDictionary<DbEntity> {

    private final DataMap container;

    private final FiltersConfig filtersConfig;

    DbEntityDictionary(DataMap container, FiltersConfig filtersConfig) {
        this.container = container;
        this.filtersConfig = filtersConfig;
    }

    @Override
    String getName(DbEntity entity) {
        return entity.getName();
    }

    @Override
    Collection<DbEntity> getAll() {
        return filter();
    }

    private Collection<DbEntity> filter() {
        if(filtersConfig == null) {
            return container.getDbEntities();
        }

        Collection<DbEntity> existingFiltered = new LinkedList<>();
        for (DbEntity entity : container.getDbEntities()) {
            TableFilter tableFilter = filtersConfig.tableFilter(entity.getCatalog(), entity.getSchema());
            if (tableFilter != null && tableFilter.isIncludeTable(entity.getName())) {
                existingFiltered.add(entity);
            }
        }
        return existingFiltered;
    }
}
