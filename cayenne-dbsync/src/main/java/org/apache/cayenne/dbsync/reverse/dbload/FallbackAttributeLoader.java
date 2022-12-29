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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.map.DbEntity;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Attribute loader that goes on per-entity level in case {@link AttributeLoader} has failed for some reason.
 * @since 5.0
 */
class FallbackAttributeLoader extends PerEntityLoader {

    private final AttributeProcessor attributeProcessor;

    FallbackAttributeLoader(DbAdapter adapter, DbLoaderConfiguration config, DbLoaderDelegate delegate) {
        super(adapter, config, delegate);
        this.attributeProcessor = new AttributeProcessor(adapter);
    }

    @Override
    ResultSet getResultSet(DbEntity dbEntity, DatabaseMetaData metaData) throws SQLException {
        return metaData.getColumns(dbEntity.getCatalog(), dbEntity.getSchema(), dbEntity.getName(), WILDCARD);
    }

    @Override
    boolean shouldLoad(DbEntity entity) {
        return entity.getAttributes().size() == 0;
    }

    @Override
    void processResultSet(DbEntity dbEntity, DbLoadDataStore map, ResultSet rs) throws SQLException {
        PatternFilter columnFilter = config.getFiltersConfig()
                .tableFilter(dbEntity.getCatalog(), dbEntity.getSchema())
                .getIncludeTableColumnFilter(dbEntity.getName());

        attributeProcessor.processAttribute(rs, columnFilter, dbEntity);
    }
}
