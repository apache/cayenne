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
package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;

/**
 * @since 4.0
 */
public class DbLoaderConfiguration {

    private Boolean skipRelationshipsLoading;

    private Boolean skipPrimaryKeyLoading;

    private String[] tableTypes;

    private FiltersConfig filtersConfig;

    public String[] getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(String[] tableTypes) {
        this.tableTypes = tableTypes;
    }

    public FiltersConfig getFiltersConfig() {
        if (filtersConfig == null) {
            // this case is used often in tests where config not initialized properly
            return FiltersConfig.create(null, null, TableFilter.everything(), PatternFilter.INCLUDE_NOTHING);
        }
        return filtersConfig;
    }

    public void setFiltersConfig(FiltersConfig filtersConfig) {
        this.filtersConfig = filtersConfig;
    }

    public boolean isSkipRelationshipsLoading() {
        return skipRelationshipsLoading != null && skipRelationshipsLoading;
    }

    public void setSkipRelationshipsLoading(Boolean skipRelationshipsLoading) {
        this.skipRelationshipsLoading = skipRelationshipsLoading;
    }

    public boolean isSkipPrimaryKeyLoading() {
        return skipPrimaryKeyLoading != null && skipPrimaryKeyLoading;
    }

    public void setSkipPrimaryKeyLoading(Boolean skipPrimaryKeyLoading) {
        this.skipPrimaryKeyLoading = skipPrimaryKeyLoading;
    }

    @Override
    public String toString() {
        String res = "EntitiesFilters: " + getFiltersConfig();
        if (isSkipRelationshipsLoading()) {
            res += "\n Skip Loading Relationships! \n";
        }

        if (isSkipPrimaryKeyLoading()) {
            res += "\n Skip Loading PrimaryKeys! \n";
        }

        return res;
    }
}
