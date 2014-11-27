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
package org.apache.cayenne.access.loader.filters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @since 3.2.
 * @Immutable
 */
public class FiltersConfig {

    private final List<DbPath> dbPaths;
    private final Map<DbPath, EntityFilters> filters;

    private List<DbPath> pathsForQueries;

    public FiltersConfig(EntityFilters ... filters) {
        this(Arrays.asList(filters));
    }

    public FiltersConfig(Collection<EntityFilters> filters) {
        this.dbPaths = new LinkedList<DbPath>();
        this.filters = new HashMap<DbPath, EntityFilters>();
        for (EntityFilters filter : filters) {
            if (filter == null) {
                continue;
            }

            DbPath path = filter.getDbPath();
            if (this.dbPaths.contains(path)) {
                this.filters.put(path, this.filters.get(path).join(filter));
                continue;
            }

            this.dbPaths.add(path);
            this.filters.put(path, filter);
        }

        Collections.sort(this.dbPaths);
    }

    /**
     * Used for loading tables and procedures, it's aim avoid unnecessary queries by compacting pairs of
     * (Catalog, Schema)
     *
     * Example:
     * <ul>
     *      <li>"aaa", null</li>
     *      <li>"aaa", "11"</li>
     *      <li>"aa", null</li>
     *      <li>"aa", "a"</li>
     *      <li>"aa", "aa"</li>
     *      <li>"aa", "aa"</li>
     * </ul>
     *
     * Should return
     * <ul>
     *      <li>"aa", null</li>
     *      <li>"aaa", null</li>
     * </ul>
     * For more examples please see tests.
     *
     * @return list of pairs (Catalog, Schema) for which getTables and getProcedures should be called
     *
     **/
    public List<DbPath> pathsForQueries() {
        if (pathsForQueries != null) {
            return pathsForQueries;
        }

        pathsForQueries = new LinkedList<DbPath>();
        if (filters.isEmpty()) {
            return pathsForQueries;
        }

        boolean save = true;
        String catalog = null;
        String schema = null;
        for (DbPath path : dbPaths) {
            if (save || catalog != null && !catalog.equals(path.catalog)) {
                catalog = path.catalog;
                schema = null;
                save = true;
            }

            if (save || schema != null && !schema.equals(path.schema)) {
                schema = path.schema;
                save = true;
            }

            if (save) {
                save = false;
                pathsForQueries.add(new DbPath(catalog, schema));
            }
        }

        return pathsForQueries;
    }

    /**
     * TODO comment
     *
     * Return filters that applicable for path (filters which path covering path passed in method)
     * */
    public EntityFilters filter(DbPath path) {
        EntityFilters res = new EntityFilters(path);
        for (Map.Entry<DbPath, EntityFilters> entry : filters.entrySet()) {
            if (entry.getKey().isCover(path)) {
                res = res.join(entry.getValue());
            }
        }

        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Map<DbPath, EntityFilters> filters = ((FiltersConfig) o).filters;
        if (this.filters.size() != filters.size()) {
            return false;
        }

        for (Map.Entry<DbPath, EntityFilters> entry : this.filters.entrySet()) {
            EntityFilters f = filters.get(entry.getKey());
            if (f == null || !f.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (DbPath dbPath : dbPaths) {
            res.append("    ").append(dbPath).append(" -> ").append(filters.get(dbPath)).append("\n");
        }

        return res.toString();
    }

    @Override
    public int hashCode() {
        return filters.hashCode();
    }

    public List<DbPath> getDbPaths() {
        return dbPaths;
    }
}
