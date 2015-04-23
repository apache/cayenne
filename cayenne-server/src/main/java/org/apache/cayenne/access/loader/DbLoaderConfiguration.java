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
package org.apache.cayenne.access.loader;

import org.apache.cayenne.access.loader.filters.TableFilter;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.access.loader.filters.PatternFilter;

/**
 * @since 4.0
 */
public class DbLoaderConfiguration {

    /**
     * Returns a name of a generic class that should be used for all
     * ObjEntities. The most common generic class is
     * {@link org.apache.cayenne.CayenneDataObject}. If generic class name is
     * null (which is the default), DbLoader will assign each entity a unique
     * class name derived from the table name.
     *
     */
    private String genericClassName;

/*
    // TODO: Andrus, 10/29/2005 - this type of filtering should be delegated to adapter
    */
/* TODO by default should skip name.startsWith("BIN$") *//*

    private NameFilter tableFilter = NamePatternMatcher.build(null, null, "BIN$");

    private NameFilter columnFilter;

    private NameFilter proceduresFilter = new NameFilter() {
        private final Collection<String> excludedProcedures = Arrays.asList(
                "auto_pk_for_table",
                "auto_pk_for_table;1" // the last name is some Mac OS X Sybase artifact
        );

        @Override
        public boolean isIncluded(String string) {
            return !excludedProcedures.contains(string);
        }
    };
*/


    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported
     * DB schema.
     */
    private String namingStrategy;

    private String[] tableTypes;

    private FiltersConfig filtersConfig;

    public String getGenericClassName() {
        return genericClassName;
    }

    public void setGenericClassName(String genericClassName) {
        this.genericClassName = genericClassName;
    }

    public String[] getTableTypes() {
        return tableTypes;
    }

    public void setTableTypes(String[] tableTypes) {
        this.tableTypes = tableTypes;
    }

    public String getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
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

    @Override
    public String toString() {
        return "EntitiesFilters: " + getFiltersConfig();
    }
}
