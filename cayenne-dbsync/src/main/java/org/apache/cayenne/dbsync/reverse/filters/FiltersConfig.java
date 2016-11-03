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

package org.apache.cayenne.dbsync.reverse.filters;

import java.util.Arrays;

/**
 * @since 4.0.
 */
public class FiltersConfig {

    private final CatalogFilter[] catalogs;

    public FiltersConfig(CatalogFilter ... catalogs) {
        if (catalogs == null || catalogs.length == 0) {
            throw new IllegalArgumentException("catalogs(" + Arrays.toString(catalogs) + ") can't be null or empty");
        }

        this.catalogs = catalogs;
    }

    public CatalogFilter[] getCatalogs() {
        return catalogs;
    }

    public PatternFilter proceduresFilter(String catalog, String schema) {
        SchemaFilter schemaFilter = getSchemaFilter(catalog, schema);
        return schemaFilter == null ? null : schemaFilter.procedures;
    }

    public TableFilter tableFilter(String catalog, String schema) {
        SchemaFilter schemaFilter = getSchemaFilter(catalog, schema);
        return schemaFilter == null ? null : schemaFilter.tables;
    }

    protected SchemaFilter getSchemaFilter(String catalog, String schema) {
        CatalogFilter catalogFilter = getCatalog(catalog);
        if (catalogFilter == null) {
            return null;
        }

        return catalogFilter.getSchema(schema);
    }

    protected CatalogFilter getCatalog(String catalog) {
        for (CatalogFilter catalogFilter : catalogs) {
            if (catalogFilter.name == null || catalogFilter.name.equals(catalog)) {
                return catalogFilter;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (CatalogFilter catalog : catalogs) {
            catalog.toString(builder, "");
        }

        return builder.toString();
    }

    public static FiltersConfig create(String catalog, String schema, TableFilter tableFilter, PatternFilter procedures) {
        return new FiltersConfig(
                    new CatalogFilter(catalog,
                        new SchemaFilter(schema, tableFilter, procedures)));
    }
}
