/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;

/**
 * @since 4.0
 */
public class DbMergerConfig {

    private FiltersConfig filtersConfig;

    private boolean skipRelationships;

    private boolean skipPrimaryKey;

    public DbMergerConfig(FiltersConfig filtersConfig, boolean skipRelationships, boolean skipPrimaryKey) {
        this.filtersConfig = filtersConfig;
        this.skipRelationships = skipRelationships;
        this.skipPrimaryKey = skipPrimaryKey;
    }

    public void setSkipRelationships(boolean skipRelationships) {
        this.skipRelationships = skipRelationships;
    }

    public boolean isSkipRelationships() {
        return skipRelationships;
    }

    public void setSkipPrimaryKey(boolean skipPrimaryKey) {
        this.skipPrimaryKey = skipPrimaryKey;
    }

    public boolean isSkipPrimaryKey() {
        return skipPrimaryKey;
    }

    public FiltersConfig getFiltersConfig() {
        return filtersConfig;
    }

    public void setFiltersConfig(FiltersConfig filtersConfig) {
        this.filtersConfig = filtersConfig;
    }
}
