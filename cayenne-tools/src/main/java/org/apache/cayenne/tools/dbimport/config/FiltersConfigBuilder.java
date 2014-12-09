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
package org.apache.cayenne.tools.dbimport.config;

import static org.apache.cayenne.access.loader.filters.FilterFactory.NULL;
import static org.apache.cayenne.access.loader.filters.FilterFactory.TRUE;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.access.loader.filters.EntityFilters;
import org.apache.cayenne.access.loader.filters.Filter;
import org.apache.cayenne.access.loader.filters.FilterFactory;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.access.loader.filters.ListFilter;

/**
* @since 4.0.
*/
public final class FiltersConfigBuilder {

    private final ReverseEngineering engineering;
    private final List<EntityFilters> filters = new LinkedList<EntityFilters>();

    public FiltersConfigBuilder(ReverseEngineering engineering) {
        this.engineering = engineering;
    }

    public FiltersConfigBuilder add(EntityFilters filter) {
        if (!filter.isEmpty()) {
            this.filters.add(filter);
        } else if (!filter.getDbPath().equals(new DbPath())) {
            this.filters.add(defaultFilter(filter.getDbPath()));
        }

        return this;
    }

    public FiltersConfig filtersConfig() {
        DbPath path = new DbPath();

        filters.addAll(processFilters(path, engineering));
        filters.addAll(processSchemas(path, engineering.getSchemas()));
        filters.addAll(processCatalog(path, engineering.getCatalogs()));

        if (filters.isEmpty()) {
            filters.add(defaultFilter(path));
        }
        return new FiltersConfig(filters);
    }

    private EntityFilters defaultFilter(DbPath path) {
        return new EntityFilters(path, TRUE, TRUE, NULL);
    }

    private Collection<? extends EntityFilters> processSchemas(DbPath root, Collection<Schema> schemas) {
        List<EntityFilters> filters = new LinkedList<EntityFilters>();
        for (Schema schema : schemas) {
            DbPath path = new DbPath(root.catalog, schema.getName());
            List<EntityFilters> schemaFilters = processFilters(path, schema);
            if (schemaFilters.isEmpty()) {
                schemaFilters.add(defaultFilter(path));
            }

            filters.addAll(schemaFilters);
        }

        return filters;
    }

    private Collection<? extends EntityFilters> processCatalog(DbPath root, Collection<Catalog> catalogs) {
        List<EntityFilters> filters = new LinkedList<EntityFilters>();
        for (Catalog catalog: catalogs) {
            DbPath path = new DbPath(catalog.getName());

            List<EntityFilters> catalogFilters = new LinkedList<EntityFilters>();
            catalogFilters.addAll(processFilters(path, catalog));
            catalogFilters.addAll(processSchemas(path, catalog.getSchemas()));

            if (catalogFilters.isEmpty()) {
                catalogFilters.add(defaultFilter(path));
            }

            filters.addAll(catalogFilters);
        }

        return filters;
    }

    private List<EntityFilters> processFilters(DbPath root, FilterContainer container) {
        LinkedList<EntityFilters> res = new LinkedList<EntityFilters>();
        res.addAll(processTableFilters(root, container.getIncludeTables()));

        EntityFilters filter = new EntityFilters(
                root,
                processIncludes(container.getIncludeTables()).join(processExcludes(container.getExcludeTables())),
                processIncludes(container.getIncludeColumns()).join(processExcludes(container.getExcludeColumns())),
                processIncludes(container.getIncludeProcedures()).join(processExcludes(container.getExcludeProcedures()))
        );

        if (!filter.isEmpty()) {
            res.add(filter);
        }

        return res;
    }

    private List<EntityFilters> processTableFilters(DbPath root, Collection<IncludeTable> tables) {
        List<EntityFilters> list = new LinkedList<EntityFilters>();
        for (IncludeTable includeTable : tables) {
            Filter<String> filter = TRUE
                    .join(processIncludes(includeTable.getIncludeColumns()))
                    .join(processExcludes(includeTable.getExcludeColumns()));

            DbPath dbPath = new DbPath(root.catalog, root.schema, includeTable.getPattern());
            list.add(new EntityFilters(dbPath, NULL, filter, NULL));
        }
        return list;
    }

    private Filter<String> processIncludes(Collection<? extends PatternParam> filters) {
        return processFilters("include", filters);
    }

    private Filter<String> processExcludes(Collection<? extends PatternParam> excludeProcedures) {
        return processFilters("exclude", excludeProcedures);
    }

    private Filter<String> processFilters(String factoryMethodName, Collection<? extends PatternParam> includeProcedures) {
        Method factoryMethod;
        try {
            factoryMethod = FilterFactory.class.getMethod(factoryMethodName, String.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        Collection<Filter<String>> filters = new LinkedList<Filter<String>>();
        for (PatternParam includeProcedure : includeProcedures) {
            try {
                filters.add((Filter<String>) factoryMethod.invoke(FilterFactory.class, includeProcedure.getPattern()));
            } catch (Exception e) {
                // TODO log / process exact parsing exception
                e.printStackTrace();
            }
        }

        if (filters.isEmpty()) {
            return NULL;
        }
        return new ListFilter<String>(filters);
    }
}
